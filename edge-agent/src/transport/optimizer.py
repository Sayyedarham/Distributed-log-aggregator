import logging
import math
import time
from typing import Optional
logger = logging.getLogger('edge_agent.optimizer')

class ChunkOptimizer:

    def __init__(self, min_chunk_size: int=1, max_chunk_size: int=200, initial_chunk_size: int=10, latency_weight: float=0.4, throughput_weight: float=0.4, cpu_weight: float=0.2, ema_alpha: float=0.25, target_latency_ms: float=50.0) -> None:
        self.min_chunk_size = max(1, min_chunk_size)
        self.max_chunk_size = max(self.min_chunk_size, max_chunk_size)
        self.current_chunk_size = max(self.min_chunk_size, min(initial_chunk_size, self.max_chunk_size))
        self.w_lat = latency_weight
        self.w_thru = throughput_weight
        self.w_cpu = cpu_weight
        self.alpha = ema_alpha
        self.target_latency_ms = target_latency_ms
        self.ema_latency_ms: Optional[float] = None
        self.ema_throughput_bps: Optional[float] = None
        self.last_cpu_load: float = 10.0
        self._prev_score: float = 0.0
        self._step_direction: int = 1
        self._last_adjustment_time = time.time()

    def record_transmission(self, batch_size: int, payload_bytes: int, rtt_ms: float, cpu_load: float) -> None:
        if self.ema_latency_ms is None:
            self.ema_latency_ms = rtt_ms
        else:
            self.ema_latency_ms = self.alpha * rtt_ms + (1.0 - self.alpha) * self.ema_latency_ms
        rtt_sec = max(0.001, rtt_ms / 1000.0)
        sample_thru = payload_bytes / rtt_sec
        if self.ema_throughput_bps is None:
            self.ema_throughput_bps = sample_thru
        else:
            self.ema_throughput_bps = self.alpha * sample_thru + (1.0 - self.alpha) * self.ema_throughput_bps
        self.last_cpu_load = max(0.0, min(100.0, cpu_load))
        self._evaluate_and_adjust()

    def _evaluate_and_adjust(self) -> None:
        lat = self.ema_latency_ms or 20.0
        thru = self.ema_throughput_bps or 10000.0
        cpu = self.last_cpu_load
        thru_norm = min(1.0, math.log10(max(10.0, thru)) / 7.0)
        lat_ratio = lat / max(1.0, self.target_latency_ms)
        lat_penalty = min(2.0, lat_ratio ** 1.5)
        cpu_penalty = cpu / 100.0
        current_score = self.w_thru * thru_norm - self.w_lat * lat_penalty - self.w_cpu * cpu_penalty
        if current_score >= self._prev_score:
            step = 2 if abs(current_score - self._prev_score) > 0.05 else 1
            new_chunk = self.current_chunk_size + self._step_direction * step
        else:
            self._step_direction = -self._step_direction
            step = 1
            new_chunk = self.current_chunk_size + self._step_direction * step
        new_chunk = max(self.min_chunk_size, min(new_chunk, self.max_chunk_size))
        if new_chunk != self.current_chunk_size:
            logger.debug('Chunk optimizer adjusted batch size: %d -> %d (Score: %.3f, Lat: %.1fms, Thru: %.1f KB/s)', self.current_chunk_size, new_chunk, current_score, lat, thru / 1024.0)
            self.current_chunk_size = new_chunk
        self._prev_score = current_score

    def get_optimal_chunk_size(self) -> int:
        return self.current_chunk_size
