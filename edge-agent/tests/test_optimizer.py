import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import unittest
from src.transport.optimizer import ChunkOptimizer

class TestChunkOptimizer(unittest.TestCase):

    def test_initial_state(self):
        opt = ChunkOptimizer(min_chunk_size=5, max_chunk_size=50, initial_chunk_size=15)
        self.assertEqual(opt.get_optimal_chunk_size(), 15)

    def test_low_latency_scaling_up(self):
        opt = ChunkOptimizer(min_chunk_size=2, max_chunk_size=50, initial_chunk_size=10)
        for _ in range(5):
            opt.record_transmission(batch_size=10, payload_bytes=50000, rtt_ms=5.0, cpu_load=5.0)
        self.assertGreater(opt.get_optimal_chunk_size(), 10)

    def test_high_latency_penalty(self):
        opt = ChunkOptimizer(min_chunk_size=2, max_chunk_size=50, initial_chunk_size=20)
        for _ in range(5):
            opt.record_transmission(batch_size=20, payload_bytes=1000, rtt_ms=800.0, cpu_load=90.0)
        self.assertLess(opt.get_optimal_chunk_size(), 20)
if __name__ == '__main__':
    unittest.main()
