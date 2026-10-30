import logging
import os
import uuid
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Optional
import yaml
logger = logging.getLogger('edge_agent.config')
_DEFAULT_CONFIG_PATH = str(Path(__file__).resolve().parent.parent / 'config' / 'agent_config.yaml')

@dataclass
class AgentConfig:
    agent_id: str = 'auto'
    collection_interval_sec: float = 10.0
    health_check_port: int = 8090
    gateway_host: str = 'localhost'
    gateway_port: int = 9090
    connection_timeout_sec: float = 10.0
    reconnect_interval_sec: float = 5.0
    max_reconnect_attempts: int = 0
    buffer_capacity: int = 10000
    buffer_use_c_extension: bool = True
    min_batch_size: int = 1
    max_batch_size: int = 100
    compression_enabled: bool = True
    compression_threshold_bytes: int = 1024
    optimizer_enabled: bool = True
    optimizer_initial_chunk_size: int = 10
    optimizer_min_chunk_size: int = 1
    optimizer_max_chunk_size: int = 200
    optimizer_adjustment_interval_sec: float = 30.0
    optimizer_latency_weight: float = 0.4
    optimizer_throughput_weight: float = 0.4
    optimizer_cpu_weight: float = 0.2
    recovery_enabled: bool = True
    recovery_cache_dir: str = '/tmp/log-aggregator-recovery'
    recovery_max_cache_size_mb: int = 100
    recovery_flush_batch_size: int = 50
    collector_cpu: bool = True
    collector_memory: bool = True
    collector_disk: bool = True
    collector_network: bool = True
    log_level: str = 'INFO'
    log_file: Optional[str] = '/var/log/log-aggregator/agent.log'
    log_max_file_size_mb: int = 50
    log_backup_count: int = 5

    def __post_init__(self) -> None:
        if self.agent_id == 'auto':
            self.agent_id = str(uuid.uuid4())
        self.validate()

    def validate(self) -> None:
        errors: list[str] = []
        if self.collection_interval_sec <= 0:
            errors.append('collection_interval_sec must be > 0')
        if self.health_check_port < 0 or self.health_check_port > 65535:
            errors.append('health_check_port must be 0–65535')
        if self.gateway_port < 1 or self.gateway_port > 65535:
            errors.append('gateway_port must be 1–65535')
        if self.connection_timeout_sec <= 0:
            errors.append('connection_timeout_sec must be > 0')
        if self.reconnect_interval_sec <= 0:
            errors.append('reconnect_interval_sec must be > 0')
        if self.buffer_capacity < 1:
            errors.append('buffer_capacity must be >= 1')
        if self.min_batch_size < 1:
            errors.append('min_batch_size must be >= 1')
        if self.max_batch_size < self.min_batch_size:
            errors.append('max_batch_size must be >= min_batch_size')
        if self.optimizer_min_chunk_size < 1:
            errors.append('optimizer_min_chunk_size must be >= 1')
        if self.optimizer_max_chunk_size < self.optimizer_min_chunk_size:
            errors.append('optimizer_max_chunk_size must be >= optimizer_min_chunk_size')
        weight_sum = self.optimizer_latency_weight + self.optimizer_throughput_weight + self.optimizer_cpu_weight
        if abs(weight_sum - 1.0) > 0.01:
            errors.append(f'Optimizer weights must sum to 1.0 (got {weight_sum:.3f})')
        if self.recovery_max_cache_size_mb < 1:
            errors.append('recovery_max_cache_size_mb must be >= 1')
        if self.log_level.upper() not in ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL'):
            errors.append(f'Invalid log_level: {self.log_level}')
        if errors:
            raise ValueError('Configuration validation failed:\n  • ' + '\n  • '.join(errors))

    @classmethod
    def load(cls, config_path: Optional[str]=None, cli_overrides: Optional[Dict[str, Any]]=None) -> 'AgentConfig':
        merged: Dict[str, Any] = {}
        yaml_values = cls._load_yaml(config_path or _DEFAULT_CONFIG_PATH)
        merged.update(yaml_values)
        env_values = cls._load_env()
        merged.update(env_values)
        if cli_overrides:
            for key, value in cli_overrides.items():
                if value is not None:
                    merged[key] = value
        logger.info('Configuration loaded (sources: yaml=%s, env=%d keys, cli=%d keys)', config_path or 'default', len(env_values), len(cli_overrides or {}))
        return cls(**merged)

    @classmethod
    def _load_yaml(cls, path: str) -> Dict[str, Any]:
        flat: Dict[str, Any] = {}
        if not os.path.isfile(path):
            logger.warning('Config file not found: %s — using defaults', path)
            return flat
        with open(path, 'r', encoding='utf-8') as fh:
            raw: Dict[str, Any] = yaml.safe_load(fh) or {}
        agent = raw.get('agent', {})
        if 'id' in agent:
            flat['agent_id'] = agent['id']
        if 'collection_interval_sec' in agent:
            flat['collection_interval_sec'] = float(agent['collection_interval_sec'])
        if 'health_check_port' in agent:
            flat['health_check_port'] = int(agent['health_check_port'])
        gw = raw.get('gateway', {})
        for key in ('host', 'port', 'connection_timeout_sec', 'reconnect_interval_sec', 'max_reconnect_attempts'):
            if key in gw:
                flat[f'gateway_{key}' if key in ('host', 'port') else key] = gw[key]
        buf = raw.get('buffer', {})
        if 'capacity' in buf:
            flat['buffer_capacity'] = int(buf['capacity'])
        if 'use_c_extension' in buf:
            flat['buffer_use_c_extension'] = bool(buf['use_c_extension'])
        transport = raw.get('transport', {})
        for key in ('min_batch_size', 'max_batch_size', 'compression_enabled', 'compression_threshold_bytes'):
            if key in transport:
                flat[key] = transport[key]
        opt = raw.get('optimizer', {})
        _opt_map = {'enabled': 'optimizer_enabled', 'initial_chunk_size': 'optimizer_initial_chunk_size', 'min_chunk_size': 'optimizer_min_chunk_size', 'max_chunk_size': 'optimizer_max_chunk_size', 'adjustment_interval_sec': 'optimizer_adjustment_interval_sec', 'latency_weight': 'optimizer_latency_weight', 'throughput_weight': 'optimizer_throughput_weight', 'cpu_weight': 'optimizer_cpu_weight'}
        for yaml_key, field_name in _opt_map.items():
            if yaml_key in opt:
                flat[field_name] = opt[yaml_key]
        rec = raw.get('recovery', {})
        _rec_map = {'enabled': 'recovery_enabled', 'cache_dir': 'recovery_cache_dir', 'max_cache_size_mb': 'recovery_max_cache_size_mb', 'flush_batch_size': 'recovery_flush_batch_size'}
        for yaml_key, field_name in _rec_map.items():
            if yaml_key in rec:
                flat[field_name] = rec[yaml_key]
        coll = raw.get('collectors', {})
        for name in ('cpu', 'memory', 'disk', 'network'):
            if name in coll:
                flat[f'collector_{name}'] = bool(coll[name])
        log = raw.get('logging', {})
        _log_map = {'level': 'log_level', 'file': 'log_file', 'max_file_size_mb': 'log_max_file_size_mb', 'backup_count': 'log_backup_count'}
        for yaml_key, field_name in _log_map.items():
            if yaml_key in log:
                flat[field_name] = log[yaml_key]
        return flat

    @classmethod
    def _load_env(cls) -> Dict[str, Any]:
        prefix = 'EDGE_AGENT_'
        env: Dict[str, Any] = {}
        _env_map: Dict[str, tuple[str, type]] = {'AGENT_ID': ('agent_id', str), 'COLLECTION_INTERVAL_SEC': ('collection_interval_sec', float), 'HEALTH_CHECK_PORT': ('health_check_port', int), 'GATEWAY_HOST': ('gateway_host', str), 'GATEWAY_PORT': ('gateway_port', int), 'CONNECTION_TIMEOUT_SEC': ('connection_timeout_sec', float), 'RECONNECT_INTERVAL_SEC': ('reconnect_interval_sec', float), 'MAX_RECONNECT_ATTEMPTS': ('max_reconnect_attempts', int), 'BUFFER_CAPACITY': ('buffer_capacity', int), 'BUFFER_USE_C_EXTENSION': ('buffer_use_c_extension', lambda v: v.lower() in ('1', 'true', 'yes')), 'MIN_BATCH_SIZE': ('min_batch_size', int), 'MAX_BATCH_SIZE': ('max_batch_size', int), 'COMPRESSION_ENABLED': ('compression_enabled', lambda v: v.lower() in ('1', 'true', 'yes')), 'COMPRESSION_THRESHOLD_BYTES': ('compression_threshold_bytes', int), 'OPTIMIZER_ENABLED': ('optimizer_enabled', lambda v: v.lower() in ('1', 'true', 'yes')), 'RECOVERY_ENABLED': ('recovery_enabled', lambda v: v.lower() in ('1', 'true', 'yes')), 'RECOVERY_CACHE_DIR': ('recovery_cache_dir', str), 'LOG_LEVEL': ('log_level', str), 'LOG_FILE': ('log_file', str)}
        for suffix, (field_name, converter) in _env_map.items():
            raw_val = os.environ.get(f'{prefix}{suffix}')
            if raw_val is not None:
                try:
                    env[field_name] = converter(raw_val)
                except (ValueError, TypeError) as exc:
                    logger.warning('Ignoring invalid env var %s%s=%r: %s', prefix, suffix, raw_val, exc)
        return env
