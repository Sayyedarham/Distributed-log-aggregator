import struct
import time
import zlib
from typing import Any, Dict, List, Optional, Tuple
MSG_METRIC_PAYLOAD = 1
MSG_METRIC_BATCH = 2
MSG_SERVER_ACK = 3
MSG_AGENT_REGISTRATION = 4
MSG_REGISTRATION_RESPONSE = 5

class BinaryBufferWriter:

    def __init__(self) -> None:
        self._bytes = bytearray()

    def write_byte(self, val: int) -> None:
        self._bytes.append(val & 255)

    def write_bool(self, val: bool) -> None:
        self._bytes.append(1 if val else 0)

    def write_int(self, val: int) -> None:
        self._bytes.extend(struct.pack('>i', int(val)))

    def write_long(self, val: int) -> None:
        self._bytes.extend(struct.pack('>q', int(val)))

    def write_double(self, val: float) -> None:
        self._bytes.extend(struct.pack('>d', float(val)))

    def write_string(self, s: Optional[str]) -> None:
        if s is None:
            self.write_int(-1)
            return
        encoded = s.encode('utf-8')
        self.write_int(len(encoded))
        self._bytes.extend(encoded)

    def write_bytes(self, b: bytes) -> None:
        self.write_int(len(b))
        self._bytes.extend(b)

    def to_bytes(self) -> bytes:
        return bytes(self._bytes)

class BinaryBufferReader:

    def __init__(self, data: bytes) -> None:
        self._data = data
        self._offset = 0

    def read_byte(self) -> int:
        b = self._data[self._offset]
        self._offset += 1
        return b

    def read_bool(self) -> bool:
        return self.read_byte() != 0

    def read_int(self) -> int:
        val = struct.unpack_from('>i', self._data, self._offset)[0]
        self._offset += 4
        return val

    def read_long(self) -> int:
        val = struct.unpack_from('>q', self._data, self._offset)[0]
        self._offset += 8
        return val

    def read_double(self) -> float:
        val = struct.unpack_from('>d', self._data, self._offset)[0]
        self._offset += 8
        return val

    def read_string(self) -> Optional[str]:
        length = self.read_int()
        if length < 0:
            return None
        s = self._data[self._offset:self._offset + length].decode('utf-8', errors='replace')
        self._offset += length
        return s

    def has_remaining(self) -> bool:
        return self._offset < len(self._data)

class ProtobufSerializer:

    @staticmethod
    def serialize_agent_info(writer: BinaryBufferWriter, info: Dict[str, Any]) -> None:
        writer.write_string(info.get('agent_id', ''))
        writer.write_string(info.get('hostname', ''))
        writer.write_string(info.get('ip_address', ''))
        writer.write_string(info.get('os_type', ''))
        writer.write_string(info.get('os_version', ''))
        writer.write_long(info.get('boot_time', 0))
        writer.write_string(info.get('agent_version', '1.0.0'))

    @staticmethod
    def deserialize_agent_info(reader: BinaryBufferReader) -> Dict[str, Any]:
        return {'agent_id': reader.read_string() or '', 'hostname': reader.read_string() or '', 'ip_address': reader.read_string() or '', 'os_type': reader.read_string() or '', 'os_version': reader.read_string() or '', 'boot_time': reader.read_long(), 'agent_version': reader.read_string() or ''}

    @classmethod
    def serialize_payload(cls, agent_info: Dict[str, Any], cpu: Optional[Dict[str, Any]]=None, memory: Optional[Dict[str, Any]]=None, disk: Optional[Dict[str, Any]]=None, network: Optional[Dict[str, Any]]=None, logs: Optional[List[Dict[str, Any]]]=None, timestamp_ms: Optional[int]=None, sequence_number: int=0, priority: int=0, is_recovery_data: bool=False) -> bytes:
        writer = BinaryBufferWriter()
        writer.write_byte(MSG_METRIC_PAYLOAD)
        cls.serialize_agent_info(writer, agent_info)
        writer.write_long(timestamp_ms if timestamp_ms is not None else int(time.time() * 1000))
        writer.write_long(sequence_number)
        writer.write_int(priority)
        writer.write_bool(is_recovery_data)
        if cpu:
            writer.write_bool(True)
            writer.write_double(cpu.get('user_percent', 0.0))
            writer.write_double(cpu.get('system_percent', 0.0))
            writer.write_double(cpu.get('idle_percent', 0.0))
            writer.write_double(cpu.get('iowait_percent', 0.0))
            writer.write_double(cpu.get('steal_percent', 0.0))
            writer.write_int(cpu.get('core_count', 1))
            writer.write_double(cpu.get('load_avg_1m', 0.0))
            writer.write_double(cpu.get('load_avg_5m', 0.0))
            writer.write_double(cpu.get('load_avg_15m', 0.0))
            per_core = cpu.get('per_core', [])
            writer.write_int(len(per_core))
            for core in per_core:
                writer.write_int(core.get('core_id', 0))
                writer.write_double(core.get('usage_percent', 0.0))
        else:
            writer.write_bool(False)
        if memory:
            writer.write_bool(True)
            writer.write_long(memory.get('total_bytes', 0))
            writer.write_long(memory.get('available_bytes', 0))
            writer.write_long(memory.get('used_bytes', 0))
            writer.write_double(memory.get('used_percent', 0.0))
            writer.write_long(memory.get('swap_total_bytes', 0))
            writer.write_long(memory.get('swap_used_bytes', 0))
            writer.write_long(memory.get('buffers_bytes', 0))
            writer.write_long(memory.get('cached_bytes', 0))
        else:
            writer.write_bool(False)
        if disk:
            writer.write_bool(True)
            devices = disk.get('devices', [])
            writer.write_int(len(devices))
            for dev in devices:
                writer.write_string(dev.get('device_name', ''))
                writer.write_long(dev.get('reads_completed', 0))
                writer.write_long(dev.get('writes_completed', 0))
                writer.write_long(dev.get('read_bytes', 0))
                writer.write_long(dev.get('write_bytes', 0))
                writer.write_double(dev.get('read_rate_bps', 0.0))
                writer.write_double(dev.get('write_rate_bps', 0.0))
                writer.write_double(dev.get('utilization_percent', 0.0))
                writer.write_long(dev.get('total_bytes', 0))
                writer.write_long(dev.get('used_bytes', 0))
                writer.write_double(dev.get('used_percent', 0.0))
        else:
            writer.write_bool(False)
        if network:
            writer.write_bool(True)
            ifaces = network.get('interfaces', [])
            writer.write_int(len(ifaces))
            for iface in ifaces:
                writer.write_string(iface.get('name', ''))
                writer.write_long(iface.get('bytes_sent', 0))
                writer.write_long(iface.get('bytes_recv', 0))
                writer.write_long(iface.get('packets_sent', 0))
                writer.write_long(iface.get('packets_recv', 0))
                writer.write_long(iface.get('errors_in', 0))
                writer.write_long(iface.get('errors_out', 0))
                writer.write_double(iface.get('send_rate_bps', 0.0))
                writer.write_double(iface.get('recv_rate_bps', 0.0))
        else:
            writer.write_bool(False)
        log_list = logs or []
        writer.write_int(len(log_list))
        for log in log_list:
            writer.write_string(log.get('source', 'system'))
            writer.write_string(log.get('level', 'INFO'))
            writer.write_string(log.get('message', ''))
            writer.write_long(log.get('timestamp_ms', int(time.time() * 1000)))
            tags = log.get('tags', {})
            writer.write_int(len(tags))
            for k, v in tags.items():
                writer.write_string(k)
                writer.write_string(str(v))
        return writer.to_bytes()

    @classmethod
    def serialize_batch(cls, payload_bytes_list: List[bytes], batch_id: str, compress: bool=False) -> bytes:
        writer = BinaryBufferWriter()
        writer.write_byte(MSG_METRIC_BATCH)
        writer.write_string(batch_id)
        writer.write_long(int(time.time() * 1000))
        writer.write_int(len(payload_bytes_list))
        body_writer = BinaryBufferWriter()
        for p_bytes in payload_bytes_list:
            body_writer.write_int(len(p_bytes))
            body_writer._bytes.extend(p_bytes)
        raw_body = body_writer.to_bytes()
        if compress and len(raw_body) > 512:
            compressed_body = zlib.compress(raw_body, level=6)
            writer.write_bool(True)
            writer.write_int(len(compressed_body))
            writer._bytes.extend(compressed_body)
        else:
            writer.write_bool(False)
            writer.write_int(len(raw_body))
            writer._bytes.extend(raw_body)
        return writer.to_bytes()

    @staticmethod
    def deserialize_ack(data: bytes) -> Dict[str, Any]:
        reader = BinaryBufferReader(data)
        msg_type = reader.read_byte()
        if msg_type != MSG_SERVER_ACK:
            raise ValueError(f'Expected MSG_SERVER_ACK ({MSG_SERVER_ACK}), got {msg_type}')
        return {'batch_id': reader.read_string() or '', 'success': reader.read_bool(), 'error_message': reader.read_string() or '', 'server_timestamp_ms': reader.read_long(), 'next_interval_ms': reader.read_int()}

    @staticmethod
    def serialize_registration(agent_info: Dict[str, Any], requested_metrics: List[str]) -> bytes:
        writer = BinaryBufferWriter()
        writer.write_byte(MSG_AGENT_REGISTRATION)
        ProtobufSerializer.serialize_agent_info(writer, agent_info)
        writer.write_long(int(time.time() * 1000))
        writer.write_int(len(requested_metrics))
        for m in requested_metrics:
            writer.write_string(m)
        writer.write_int(10000)
        return writer.to_bytes()

    @staticmethod
    def deserialize_registration_response(data: bytes) -> Dict[str, Any]:
        reader = BinaryBufferReader(data)
        msg_type = reader.read_byte()
        if msg_type != MSG_REGISTRATION_RESPONSE:
            raise ValueError(f'Expected MSG_REGISTRATION_RESPONSE, got {msg_type}')
        accepted = reader.read_bool()
        assigned_id = reader.read_string() or ''
        interval_ms = reader.read_int()
        num_metrics = reader.read_int()
        collect_metrics = [reader.read_string() or '' for _ in range(num_metrics)]
        return {'accepted': accepted, 'assigned_agent_id': assigned_id, 'collection_interval_ms': interval_ms, 'collect_metrics': collect_metrics}
