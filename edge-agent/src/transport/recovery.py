import heapq
import json
import logging
import os
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
logger = logging.getLogger('edge_agent.recovery')

@dataclass(order=True)
class PrioritizedPayload:
    sort_key: int
    timestamp_ms: int = field(compare=True)
    payload_bytes: bytes = field(compare=False)
    batch_id: str = field(compare=False)

class RecoveryManager:

    def __init__(self, cache_dir: str='/tmp/log-aggregator-recovery', max_cache_size_mb: int=100, flush_batch_size: int=50) -> None:
        self.cache_dir = Path(cache_dir)
        self.max_cache_bytes = max_cache_size_mb * 1024 * 1024
        self.flush_batch_size = flush_batch_size
        self._lock = threading.Lock()
        self._priority_queue: List[PrioritizedPayload] = []
        self._is_recovering = False
        try:
            self.cache_dir.mkdir(parents=True, exist_ok=True)
        except Exception as e:
            logger.warning('Could not create recovery cache dir %s: %s. Using local fallback.', cache_dir, e)
            self.cache_dir = Path('./recovery_cache')
            self.cache_dir.mkdir(parents=True, exist_ok=True)
        self._index_file = self.cache_dir / 'recovery_index.json'
        self._load_existing_cache()

    def _load_existing_cache(self) -> None:
        with self._lock:
            spool_files = sorted(self.cache_dir.glob('spool_*.bin'))
            for s_file in spool_files:
                try:
                    with open(s_file, 'rb') as f:
                        data = f.read()
                    if len(data) >= 12:
                        priority = int.from_bytes(data[0:4], byteorder='big', signed=True)
                        ts = int.from_bytes(data[4:12], byteorder='big', signed=True)
                        payload = data[12:]
                        heapq.heappush(self._priority_queue, PrioritizedPayload(sort_key=-priority, timestamp_ms=ts, payload_bytes=payload, batch_id=s_file.stem))
                except Exception as e:
                    logger.error('Failed loading recovery spool file %s: %s', s_file, e)
            if self._priority_queue:
                logger.info('Recovered %d cached telemetry payloads from disk storage.', len(self._priority_queue))

    def cache_payload(self, payload_bytes: bytes, priority: int=0, timestamp_ms: Optional[int]=None) -> bool:
        now_ms = timestamp_ms or int(time.time() * 1000)
        batch_id = f'spool_{now_ms}_{priority}_{len(self._priority_queue)}'
        file_path = self.cache_dir / f'{batch_id}.bin'
        with self._lock:
            self._enforce_cache_limits()
            try:
                with open(file_path, 'wb') as f:
                    f.write(priority.to_bytes(4, byteorder='big', signed=True))
                    f.write(now_ms.to_bytes(8, byteorder='big', signed=True))
                    f.write(payload_bytes)
                heapq.heappush(self._priority_queue, PrioritizedPayload(sort_key=-priority, timestamp_ms=now_ms, payload_bytes=payload_bytes, batch_id=batch_id))
                logger.debug('Safely cached metric payload to recovery spool: %s', batch_id)
                return True
            except Exception as e:
                logger.error('Error writing recovery payload to disk: %s', e)
                return False

    def pop_recovery_batch(self, max_count: Optional[int]=None) -> List[Tuple[str, bytes]]:
        count = max_count or self.flush_batch_size
        batch: List[Tuple[str, bytes]] = []
        with self._lock:
            while self._priority_queue and len(batch) < count:
                item = heapq.heappop(self._priority_queue)
                batch.append((item.batch_id, item.payload_bytes))
        return batch

    def acknowledge_recovered(self, batch_ids: List[str]) -> None:
        with self._lock:
            for b_id in batch_ids:
                f_path = self.cache_dir / f'{b_id}.bin'
                if f_path.exists():
                    try:
                        f_path.unlink()
                    except Exception as e:
                        logger.warning('Could not delete recovery file %s: %s', f_path, e)

    def return_unacknowledged(self, items: List[Tuple[str, bytes]]) -> None:
        with self._lock:
            for b_id, payload in items:
                heapq.heappush(self._priority_queue, PrioritizedPayload(sort_key=0, timestamp_ms=int(time.time() * 1000), payload_bytes=payload, batch_id=b_id))

    def _enforce_cache_limits(self) -> None:
        try:
            total_size = sum((f.stat().st_size for f in self.cache_dir.glob('*.bin')))
            if total_size > self.max_cache_bytes:
                files = sorted(self.cache_dir.glob('*.bin'), key=lambda p: p.stat().st_mtime)
                for f in files[:max(1, len(files) // 5)]:
                    f.unlink(missing_ok=True)
                logger.warning('Recovery cache ceiling exceeded. Evicted oldest cache files.')
        except Exception:
            pass

    def pending_count(self) -> int:
        with self._lock:
            return len(self._priority_queue)
