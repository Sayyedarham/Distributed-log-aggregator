import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import threading
import unittest
from src.buffer.ring_buffer import RingBuffer

class TestRingBuffer(unittest.TestCase):

    def test_basic_push_pop(self):
        rb = RingBuffer[str](capacity=3)
        self.assertTrue(rb.is_empty())
        self.assertEqual(len(rb), 0)
        evicted = rb.push('item1')
        self.assertFalse(evicted)
        self.assertEqual(len(rb), 1)
        rb.push('item2')
        rb.push('item3')
        self.assertTrue(rb.is_full())
        item = rb.pop()
        self.assertEqual(item, 'item1')
        self.assertEqual(len(rb), 2)

    def test_eviction_on_capacity(self):
        rb = RingBuffer[int](capacity=3)
        rb.push(10)
        rb.push(20)
        rb.push(30)
        evicted = rb.push(40)
        self.assertTrue(evicted)
        self.assertEqual(rb.dropped_count, 1)
        self.assertEqual(len(rb), 3)
        self.assertEqual(rb.pop(), 20)
        self.assertEqual(rb.pop(), 30)
        self.assertEqual(rb.pop(), 40)
        self.assertIsNone(rb.pop())

    def test_pop_batch(self):
        rb = RingBuffer[int](capacity=10)
        for i in range(7):
            rb.push(i)
        batch = rb.pop_batch(4)
        self.assertEqual(batch, [0, 1, 2, 3])
        self.assertEqual(len(rb), 3)
        batch2 = rb.pop_batch(10)
        self.assertEqual(batch2, [4, 5, 6])
        self.assertTrue(rb.is_empty())

    def test_concurrency_thread_safety(self):
        rb = RingBuffer[int](capacity=1000)
        num_items = 500

        def producer():
            for i in range(num_items):
                rb.push(i)
        t1 = threading.Thread(target=producer)
        t2 = threading.Thread(target=producer)
        t1.start()
        t2.start()
        t1.join()
        t2.join()
        self.assertEqual(len(rb), 1000)
if __name__ == '__main__':
    unittest.main()
