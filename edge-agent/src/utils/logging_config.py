import json
import logging
import logging.handlers
import os
import platform
import sys
from datetime import datetime, timezone
from typing import Optional

class JSONFormatter(logging.Formatter):

    def __init__(self, agent_id: str='unknown') -> None:
        super().__init__()
        self._agent_id = agent_id
        self._hostname = platform.node()

    def format(self, record: logging.LogRecord) -> str:
        log_entry: dict = {'timestamp': datetime.fromtimestamp(record.created, tz=timezone.utc).isoformat(), 'level': record.levelname, 'logger': record.name, 'message': record.getMessage(), 'agent_id': self._agent_id, 'hostname': self._hostname, 'module': record.module, 'funcName': record.funcName, 'lineno': record.lineno}
        if record.exc_info and record.exc_info[1] is not None:
            log_entry['exc_info'] = self.formatException(record.exc_info)
        if hasattr(record, 'extra_data'):
            log_entry['extra'] = record.extra_data
        return json.dumps(log_entry, default=str)

def configure_logging(level: str='INFO', log_file: Optional[str]=None, max_file_size_mb: int=50, backup_count: int=5, agent_id: str='unknown') -> logging.Logger:
    root_logger = logging.getLogger('edge_agent')
    root_logger.setLevel(getattr(logging, level.upper(), logging.INFO))
    root_logger.handlers.clear()
    formatter = JSONFormatter(agent_id=agent_id)
    console_handler = logging.StreamHandler(sys.stderr)
    console_handler.setFormatter(formatter)
    root_logger.addHandler(console_handler)
    if log_file:
        log_dir = os.path.dirname(log_file)
        try:
            if log_dir:
                os.makedirs(log_dir, exist_ok=True)
            file_handler = logging.handlers.RotatingFileHandler(filename=log_file, maxBytes=max_file_size_mb * 1024 * 1024, backupCount=backup_count, encoding='utf-8')
            file_handler.setFormatter(formatter)
            root_logger.addHandler(file_handler)
        except OSError as exc:
            root_logger.warning('Unable to set up file logging at %s: %s', log_file, exc)
    return root_logger
