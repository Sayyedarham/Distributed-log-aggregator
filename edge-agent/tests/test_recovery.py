import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import shutil
import tempfile
import unittest
from src.transport.recovery import RecoveryManager

class TestRecoveryManager(unittest.TestCase):

    def setUp(self):
        self.test_dir = tempfile.mkdtemp()
        self.rm = RecoveryManager(cache_dir=self.test_dir, max_cache_size_mb=10, flush_batch_size=5)

    def tearDown(self):
        shutil.rmtree(self.test_dir, ignore_errors=True)

    def test_cache_and_priority_flush(self):
        self.rm.cache_payload(b'normal_payload_1', priority=0)
        self.rm.cache_payload(b'critical_payload_alert', priority=2)
        self.rm.cache_payload(b'warning_payload', priority=1)
        self.assertEqual(self.rm.pending_count(), 3)
        batch = self.rm.pop_recovery_batch(max_count=2)
        self.assertEqual(len(batch), 2)
        self.assertEqual(batch[0][1], b'critical_payload_alert')
        self.assertEqual(batch[1][1], b'warning_payload')
        self.rm.acknowledge_recovered([batch[0][0], batch[1][0]])
        self.assertEqual(self.rm.pending_count(), 1)
if __name__ == '__main__':
    unittest.main()
