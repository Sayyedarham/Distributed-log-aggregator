import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import unittest
from src.serialization.protobuf_serializer import ProtobufSerializer, BinaryBufferReader, BinaryBufferWriter

class TestTransportAndSerialization(unittest.TestCase):

    def test_binary_buffer_primitive_roundtrip(self):
        writer = BinaryBufferWriter()
        writer.write_byte(42)
        writer.write_bool(True)
        writer.write_int(123456)
        writer.write_long(9876543210)
        writer.write_double(3.14159)
        writer.write_string('distributed-telemetry')
        data = writer.to_bytes()
        reader = BinaryBufferReader(data)
        self.assertEqual(reader.read_byte(), 42)
        self.assertTrue(reader.read_bool())
        self.assertEqual(reader.read_int(), 123456)
        self.assertEqual(reader.read_long(), 9876543210)
        self.assertAlmostEqual(reader.read_double(), 3.14159, places=5)
        self.assertEqual(reader.read_string(), 'distributed-telemetry')

    def test_payload_serialization(self):
        agent_info = {'agent_id': 'test-agent-001', 'hostname': 'prod-node-12', 'ip_address': '10.0.1.55', 'os_type': 'linux', 'os_version': '5.15.0', 'boot_time': 1700000000, 'agent_version': '1.0.0'}
        cpu = {'user_percent': 15.5, 'system_percent': 3.2, 'idle_percent': 81.3, 'core_count': 8}
        mem = {'total_bytes': 16000000000, 'used_percent': 45.0}
        payload_bytes = ProtobufSerializer.serialize_payload(agent_info=agent_info, cpu=cpu, memory=mem, sequence_number=1)
        self.assertGreater(len(payload_bytes), 20)
        batch_bytes = ProtobufSerializer.serialize_batch([payload_bytes], batch_id='b123')
        self.assertGreater(len(batch_bytes), len(payload_bytes))
if __name__ == '__main__':
    unittest.main()
