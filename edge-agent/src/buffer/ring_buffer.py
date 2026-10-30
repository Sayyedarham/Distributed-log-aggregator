import threading
from typing import Any, Generic, Iterator, List, Optional, TypeVar
T = TypeVar('T')

class RingBuffer(Generic[T]):

    def __init__(self, capacity: int=10000) -> None:
        if capacity <= 0:
            raise ValueError('Capacity must be positive integer')
        self._capacity: int = capacity
        self._buffer: List[Optional[T]] = [None] * capacity
        self._head: int = 0
        self._tail: int = 0
        self._count: int = 0
        self._dropped_count: int = 0
        self._lock = threading.Lock()

    @property
    def capacity(self) -> int:
        return self._capacity

    @property
    def dropped_count(self) -> int:
        with self._lock:
            return self._dropped_count

    def push(self, item: T) -> bool:
        with self._lock:
            evicted = False
            if self._count == self._capacity:
                self._tail = (self._tail + 1) % self._capacity
                self._dropped_count += 1
                evicted = True
            else:
                self._count += 1
            self._buffer[self._head] = item
            self._head = (self._head + 1) % self._capacity
            return evicted

    def pop(self) -> Optional[T]:
        with self._lock:
            if self._count == 0:
                return None
            item = self._buffer[self._tail]
            self._buffer[self._tail] = None
            self._tail = (self._tail + 1) % self._capacity
            self._count -= 1
            return item

    def pop_batch(self, max_items: int) -> List[T]:
        if max_items <= 0:
            return []
        with self._lock:
            items: List[T] = []
            to_pop = min(max_items, self._count)
            for _ in range(to_pop):
                item = self._buffer[self._tail]
                self._buffer[self._tail] = None
                self._tail = (self._tail + 1) % self._capacity
                if item is not None:
                    items.append(item)
            self._count -= len(items)
            return items

    def peek(self) -> Optional[T]:
        with self._lock:
            if self._count == 0:
                return None
            return self._buffer[self._tail]

    def is_full(self) -> bool:
        with self._lock:
            return self._count == self._capacity

    def is_empty(self) -> bool:
        with self._lock:
            return self._count == 0

    def clear(self) -> None:
        with self._lock:
            self._buffer = [None] * self._capacity
            self._head = 0
            self._tail = 0
            self._count = 0

    def __len__(self) -> int:
        with self._lock:
            return self._count

    def __repr__(self) -> str:
        with self._lock:
            return f'<RingBuffer count={self._count}/{self._capacity} dropped={self._dropped_count}>'
