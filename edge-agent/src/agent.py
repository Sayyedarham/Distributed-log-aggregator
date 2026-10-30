import http.server
import json
import logging
import platform
import socket
import threading
import time
import uuid
from typing import Any, Dict, List, Optional
from .buffer import RingBuffer
from .collectors import CpuCollector, DiskCollector, MemoryCollector, MetricCollector, NetworkCollector
from .config import AgentConfig
from .events.observer import Event, EventBus
from .serialization.protobuf_serializer import ProtobufSerializer
from .transport import ChunkOptimizer, MetricSender, RecoveryManager
logger = logging.getLogger('edge_agent.agent')

class HealthCheckHandler(http.server.BaseHTTPRequestHandler):
    agent_instance: Any = None

    def do_GET(self) -> None:
        if self.path in ('/health', '/healthz', '/'):
            status_data = self.agent_instance.get_health_status()
            body = json.dumps(status_data).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format: str, *args: Any) -> None:
        pass

class NodeAgent:

    def __init__(self, config: AgentConfig) -> None:
        self.config = config
        self.agent_id = str(uuid.uuid4())[:8] if config.agent_id == 'auto' else config.agent_id
        self.hostname = socket.gethostname()
        self.ip_address = self._resolve_local_ip()
        self.os_type = platform.system().lower()
        self.os_version = platform.release()
        self.boot_time = int(time.time())
        self.agent_info = {'agent_id': self.agent_id, 'hostname': self.hostname, 'ip_address': self.ip_address, 'os_type': self.os_type, 'os_version': self.os_version, 'boot_time': self.boot_time, 'agent_version': '1.0.0'}
        self.event_bus = EventBus()
        self.collectors: List[MetricCollector] = []
        if config.collect_cpu:
            self.collectors.append(CpuCollector())
        if config.collect_memory:
            self.collectors.append(MemoryCollector())
        if config.collect_disk:
            self.collectors.append(DiskCollector())
        if config.collect_network:
            self.collectors.append(NetworkCollector())
        self.buffer = RingBuffer[bytes](capacity=config.buffer_capacity)
        self.optimizer = ChunkOptimizer(min_chunk_size=config.min_chunk_size, max_chunk_size=config.max_chunk_size, initial_chunk_size=config.initial_chunk_size, latency_weight=config.opt_latency_weight, throughput_weight=config.opt_throughput_weight, cpu_weight=config.opt_cpu_weight)
        self.recovery = RecoveryManager(cache_dir=config.recovery_cache_dir, max_cache_size_mb=config.recovery_max_cache_mb, flush_batch_size=config.recovery_flush_batch_size)
        self.sender = MetricSender(host=config.gateway_host, port=config.gateway_port, timeout_sec=config.connection_timeout_sec, reconnect_interval_sec=config.reconnect_interval_sec, max_reconnect_attempts=config.max_reconnect_attempts, event_bus=self.event_bus)
        self._running = False
        self._sequence_number = 0
        self._collection_thread: Optional[threading.Thread] = None
        self._transmission_thread: Optional[threading.Thread] = None
        self._http_server: Optional[http.server.HTTPServer] = None
        self._http_thread: Optional[threading.Thread] = None

    def _resolve_local_ip(self) -> str:
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(('8.8.8.8', 80))
                return s.getsockname()[0]
        except Exception:
            return '127.0.0.1'

    def start(self) -> None:
        logger.info('Starting Edge Node Agent [ID: %s, Host: %s, OS: %s %s]', self.agent_id, self.hostname, self.os_type, self.os_version)
        self._running = True
        self.event_bus.publish(Event.AGENT_STARTED, {'agent_id': self.agent_id})
        self._start_health_server()
        self._register_with_gateway()
        self._collection_thread = threading.Thread(target=self._collection_loop, name='TelemetryCollector', daemon=True)
        self._collection_thread.start()
        self._transmission_thread = threading.Thread(target=self._transmission_loop, name='NetworkTransmitter', daemon=True)
        self._transmission_thread.start()
        logger.info('Node Agent running successfully.')

    def _start_health_server(self) -> None:
        try:
            HealthCheckHandler.agent_instance = self
            self._http_server = http.server.HTTPServer(('0.0.0.0', self.config.health_check_port), HealthCheckHandler)
            self._http_thread = threading.Thread(target=self._http_server.serve_forever, name='HealthServer', daemon=True)
            self._http_thread.start()
            logger.info('Health check server listening on port %d', self.config.health_check_port)
        except Exception as e:
            logger.warning('Could not bind health check server to port %d: %s', self.config.health_check_port, e)

    def _register_with_gateway(self) -> None:
        requested = [c.get_name() for c in self.collectors]
        try:
            resp = self.sender.register_agent(self.agent_info, requested)
            if resp and resp.get('accepted'):
                logger.info('Gateway accepted registration. Configured interval: %dms', resp.get('collection_interval_ms', 10000))
        except Exception as e:
            logger.warning('Initial registration attempt deferred: %s', e)

    def _collection_loop(self) -> None:
        while self._running:
            t_start = time.perf_counter()
            cpu_data = None
            mem_data = None
            disk_data = None
            net_data = None
            for collector in self.collectors:
                try:
                    c_name = collector.get_name()
                    data = collector.collect()
                    if c_name == 'cpu':
                        cpu_data = data
                    elif c_name == 'memory':
                        mem_data = data
                    elif c_name == 'disk':
                        disk_data = data
                    elif c_name == 'network':
                        net_data = data
                except Exception as e:
                    logger.error('Error executing collector %s: %s', collector.get_name(), e)
            self._sequence_number += 1
            payload_bytes = ProtobufSerializer.serialize_payload(agent_info=self.agent_info, cpu=cpu_data, memory=mem_data, disk=disk_data, network=net_data, sequence_number=self._sequence_number, priority=0)
            evicted = self.buffer.push(payload_bytes)
            if evicted:
                self.event_bus.publish(Event.BUFFER_FULL, {'dropped_total': self.buffer.dropped_count})
            self.event_bus.publish(Event.METRIC_COLLECTED, {'seq': self._sequence_number})
            elapsed = time.perf_counter() - t_start
            sleep_time = max(0.1, self.config.collection_interval_sec - elapsed)
            time.sleep(sleep_time)

    def _transmission_loop(self) -> None:
        while self._running:
            if self.sender.is_connected and self.recovery.pending_count() > 0:
                self._flush_recovery_cache()
            chunk_size = self.optimizer.get_optimal_chunk_size()
            payloads = self.buffer.pop_batch(chunk_size)
            if not payloads:
                time.sleep(0.5)
                continue
            batch_id = f'batch_{self.agent_id}_{int(time.time() * 1000)}'
            batch_bytes = ProtobufSerializer.serialize_batch(payload_bytes_list=payloads, batch_id=batch_id, compress=self.config.compression_enabled)
            success, ack, rtt_ms = self.sender.send_batch(batch_bytes)
            if success:
                cpu_load = 15.0
                for c in self.collectors:
                    if c.get_name() == 'cpu' and hasattr(c, '_last_total_jiffies'):
                        cpu_load = getattr(c, '_last_user_jiffies', 15.0) or 15.0
                        break
                self.optimizer.record_transmission(batch_size=len(payloads), payload_bytes=len(batch_bytes), rtt_ms=rtt_ms, cpu_load=cpu_load)
            else:
                logger.warning('Network delivery failed. Caching %d payloads to stateful recovery disk queue.', len(payloads))
                for p_bytes in payloads:
                    self.recovery.cache_payload(p_bytes, priority=0)
                time.sleep(self.config.reconnect_interval_sec)

    def _flush_recovery_cache(self) -> None:
        batch = self.recovery.pop_recovery_batch(self.config.recovery_flush_batch_size)
        if not batch:
            return
        batch_ids = [b[0] for b in batch]
        payload_bytes_list = [b[1] for b in batch]
        batch_id = f'recovery_{int(time.time() * 1000)}'
        batch_bytes = ProtobufSerializer.serialize_batch(payload_bytes_list=payload_bytes_list, batch_id=batch_id, compress=self.config.compression_enabled)
        success, _, _ = self.sender.send_batch(batch_bytes)
        if success:
            logger.info('Successfully flushed %d cached telemetry spools.', len(batch))
            self.recovery.acknowledge_recovered(batch_ids)
        else:
            logger.warning('Failed flushing recovery batch. Re-queuing items.')
            self.recovery.return_unacknowledged(batch)

    def stop(self) -> None:
        logger.info('Stopping Node Agent gracefully...')
        self._running = False
        if self._http_server:
            try:
                self._http_server.shutdown()
            except Exception:
                pass
        self.sender.disconnect()
        self.event_bus.publish(Event.AGENT_STOPPED)
        logger.info('Node Agent shutdown complete.')

    def get_health_status(self) -> Dict[str, Any]:
        return {'status': 'healthy' if self._running else 'stopped', 'agent_id': self.agent_id, 'hostname': self.hostname, 'connected_to_gateway': self.sender.is_connected, 'buffer_utilization': len(self.buffer), 'buffer_capacity': self.buffer.capacity, 'buffer_dropped_total': self.buffer.dropped_count, 'recovery_pending_spools': self.recovery.pending_count(), 'optimal_chunk_size': self.optimizer.get_optimal_chunk_size(), 'uptime_seconds': int(time.time() - self.boot_time)}
