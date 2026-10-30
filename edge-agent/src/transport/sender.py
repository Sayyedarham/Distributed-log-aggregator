import logging
import socket
import struct
import time
from typing import Any, Dict, Optional, Tuple
from ..events.observer import Event, EventBus
from ..serialization.protobuf_serializer import ProtobufSerializer
logger = logging.getLogger('edge_agent.transport')

class MetricSender:

    def __init__(self, host: str='localhost', port: int=9090, timeout_sec: float=10.0, reconnect_interval_sec: float=5.0, max_reconnect_attempts: int=0, event_bus: Optional[EventBus]=None) -> None:
        self.host = host
        self.port = port
        self.timeout_sec = timeout_sec
        self.reconnect_interval_sec = reconnect_interval_sec
        self.max_reconnect_attempts = max_reconnect_attempts
        self.event_bus = event_bus
        self._socket: Optional[socket.socket] = None
        self._is_connected = False
        self._reconnect_attempts = 0

    @property
    def is_connected(self) -> bool:
        return self._is_connected

    def connect(self) -> bool:
        self.disconnect()
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(self.timeout_sec)
            s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            s.connect((self.host, self.port))
            self._socket = s
            self._is_connected = True
            self._reconnect_attempts = 0
            logger.info('Successfully connected to Ingestion Gateway at %s:%d', self.host, self.port)
            if self.event_bus:
                self.event_bus.publish(Event.CONNECTION_RESTORED, {'host': self.host, 'port': self.port})
            return True
        except Exception as e:
            self._is_connected = False
            self._reconnect_attempts += 1
            logger.warning('Connection failed to %s:%d (attempt %d): %s', self.host, self.port, self._reconnect_attempts, e)
            if self.event_bus:
                self.event_bus.publish(Event.CONNECTION_LOST, {'error': str(e)})
            return False

    def disconnect(self) -> None:
        if self._socket:
            try:
                self._socket.close()
            except Exception:
                pass
            self._socket = None
        if self._is_connected:
            self._is_connected = False
            if self.event_bus:
                self.event_bus.publish(Event.CONNECTION_LOST)

    def send_frame(self, data: bytes) -> Tuple[bool, float]:
        if not self._is_connected or not self._socket:
            if not self.connect():
                return (False, 0.0)
        t_start = time.perf_counter()
        try:
            header = struct.pack('>I', len(data))
            self._socket.sendall(header + data)
            t_end = time.perf_counter()
            rtt_ms = (t_end - t_start) * 1000.0
            return (True, rtt_ms)
        except Exception as e:
            logger.error('Network send error: %s', e)
            self.disconnect()
            return (False, 0.0)

    def receive_frame(self) -> Optional[bytes]:
        if not self._is_connected or not self._socket:
            return None
        try:
            header = self._recv_all(4)
            if not header or len(header) < 4:
                return None
            frame_len = struct.unpack('>I', header)[0]
            if frame_len > 10 * 1024 * 1024:
                logger.error('Received frame length %d exceeds 10MB ceiling', frame_len)
                self.disconnect()
                return None
            payload = self._recv_all(frame_len)
            return payload
        except Exception as e:
            logger.error('Network receive error: %s', e)
            self.disconnect()
            return None

    def send_batch(self, batch_bytes: bytes) -> Tuple[bool, Optional[Dict[str, Any]], float]:
        t0 = time.perf_counter()
        success, send_rtt = self.send_frame(batch_bytes)
        if not success:
            if self.event_bus:
                self.event_bus.publish(Event.BATCH_FAILED)
            return (False, None, 0.0)
        ack_data = self.receive_frame()
        t1 = time.perf_counter()
        total_rtt_ms = (t1 - t0) * 1000.0
        ack_dict = None
        if ack_data:
            try:
                ack_dict = ProtobufSerializer.deserialize_ack(ack_data)
                if self.event_bus:
                    self.event_bus.publish(Event.BATCH_SENT, ack_dict)
                return (True, ack_dict, total_rtt_ms)
            except Exception as e:
                logger.warning('Could not parse ServerAck: %s', e)
        return (True, ack_dict, total_rtt_ms)

    def register_agent(self, agent_info: Dict[str, Any], requested_metrics: list) -> Optional[Dict[str, Any]]:
        reg_bytes = ProtobufSerializer.serialize_registration(agent_info, requested_metrics)
        success, _ = self.send_frame(reg_bytes)
        if not success:
            return None
        resp_bytes = self.receive_frame()
        if not resp_bytes:
            return None
        try:
            return ProtobufSerializer.deserialize_registration_response(resp_bytes)
        except Exception as e:
            logger.error('Registration deserialization failed: %s', e)
            return None

    def _recv_all(self, num_bytes: int) -> Optional[bytes]:
        data = bytearray()
        while len(data) < num_bytes:
            chunk = self._socket.recv(num_bytes - len(data))
            if not chunk:
                return None
            data.extend(chunk)
        return bytes(data)
