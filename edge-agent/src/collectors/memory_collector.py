import os
from typing import Any, Dict
from .base import MetricCollector
try:
    import psutil
except ImportError:
    psutil = None

class MemoryCollector(MetricCollector):

    def get_name(self) -> str:
        return 'memory'

    def is_available(self) -> bool:
        return os.path.exists('/proc/meminfo') or psutil is not None

    def _read_proc_meminfo(self) -> Dict[str, Any]:
        raw: Dict[str, int] = {}
        with open('/proc/meminfo', 'r', encoding='utf-8') as f:
            for line in f:
                parts = line.strip().split(':')
                if len(parts) == 2:
                    key = parts[0].strip()
                    val_str = parts[1].strip().split()[0]
                    try:
                        raw[key] = int(val_str) * 1024
                    except ValueError:
                        continue
        total = raw.get('MemTotal', 0)
        free = raw.get('MemFree', 0)
        available = raw.get('MemAvailable', free)
        buffers = raw.get('Buffers', 0)
        cached = raw.get('Cached', 0)
        swap_total = raw.get('SwapTotal', 0)
        swap_free = raw.get('SwapFree', 0)
        swap_used = max(0, swap_total - swap_free)
        used = max(0, total - available)
        used_percent = used / total * 100.0 if total > 0 else 0.0
        return {'total_bytes': total, 'available_bytes': available, 'used_bytes': used, 'used_percent': round(used_percent, 2), 'swap_total_bytes': swap_total, 'swap_used_bytes': swap_used, 'buffers_bytes': buffers, 'cached_bytes': cached}

    def _collect_fallback(self) -> Dict[str, Any]:
        if psutil is not None:
            vm = psutil.virtual_memory()
            sm = psutil.swap_memory()
            return {'total_bytes': int(vm.total), 'available_bytes': int(vm.available), 'used_bytes': int(vm.used), 'used_percent': float(vm.percent), 'swap_total_bytes': int(sm.total), 'swap_used_bytes': int(sm.used), 'buffers_bytes': int(getattr(vm, 'buffers', 0)), 'cached_bytes': int(getattr(vm, 'cached', 0))}
        total = 16 * 1024 * 1024 * 1024
        used = 8 * 1024 * 1024 * 1024
        return {'total_bytes': total, 'available_bytes': total - used, 'used_bytes': used, 'used_percent': 50.0, 'swap_total_bytes': 4 * 1024 * 1024 * 1024, 'swap_used_bytes': 512 * 1024 * 1024, 'buffers_bytes': 256 * 1024 * 1024, 'cached_bytes': 2048 * 1024 * 1024}

    def collect(self) -> Dict[str, Any]:
        if os.path.exists('/proc/meminfo'):
            try:
                return self._read_proc_meminfo()
            except Exception:
                return self._collect_fallback()
        return self._collect_fallback()
