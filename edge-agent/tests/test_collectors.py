import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import unittest
from src.collectors import CpuCollector, MemoryCollector, DiskCollector, NetworkCollector

class TestCollectors(unittest.TestCase):

    def test_cpu_collector(self):
        c = CpuCollector()
        self.assertEqual(c.get_name(), 'cpu')
        data = c.collect()
        self.assertIn('user_percent', data)
        self.assertIn('core_count', data)
        self.assertGreaterEqual(data['core_count'], 1)

    def test_memory_collector(self):
        m = MemoryCollector()
        self.assertEqual(m.get_name(), 'memory')
        data = m.collect()
        self.assertIn('total_bytes', data)
        self.assertIn('used_percent', data)
        self.assertGreater(data['total_bytes'], 0)

    def test_disk_collector(self):
        d = DiskCollector()
        self.assertEqual(d.get_name(), 'disk')
        data = d.collect()
        self.assertIn('devices', data)
        self.assertIsInstance(data['devices'], list)

    def test_network_collector(self):
        n = NetworkCollector()
        self.assertEqual(n.get_name(), 'network')
        data = n.collect()
        self.assertIn('interfaces', data)
        self.assertIsInstance(data['interfaces'], list)
if __name__ == '__main__':
    unittest.main()
