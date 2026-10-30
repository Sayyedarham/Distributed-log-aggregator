import argparse
import logging
import signal
import sys
import time
from typing import Any
from .agent import NodeAgent
from .config import AgentConfig
from .utils.logging_config import setup_logging
logger = logging.getLogger('edge_agent.main')

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description='Production Edge Collection Agent - Distributed Log Aggregator & Metric Monitor')
    parser.add_argument('--config', '-c', type=str, default=None, help='Path to YAML configuration file.')
    parser.add_argument('--gateway-host', type=str, default=None, help='Ingestion Gateway host address.')
    parser.add_argument('--gateway-port', type=int, default=None, help='Ingestion Gateway TCP port (default 9090).')
    parser.add_argument('--interval', '-i', type=float, default=None, help='Metric collection interval in seconds.')
    parser.add_argument('--agent-id', type=str, default=None, help='Explicit unique agent identifier (default auto UUID).')
    parser.add_argument('--log-level', type=str, default=None, choices=['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL'], help='Logging verbosity level.')
    return parser.parse_args()

def main() -> None:
    args = parse_args()
    config = AgentConfig.load(config_path=args.config, cli_overrides={'agent_id': args.agent_id, 'gateway_host': args.gateway_host, 'gateway_port': args.gateway_port, 'collection_interval_sec': args.interval, 'log_level': args.log_level})
    setup_logging(log_level=config.log_level, log_file=config.log_file, max_file_size_mb=config.log_max_file_size_mb, backup_count=config.log_backup_count, agent_id=config.agent_id)
    agent = NodeAgent(config)

    def handle_signal(sig: int, frame: Any) -> None:
        logger.info('Received termination signal %d. Shutting down agent...', sig)
        agent.stop()
        sys.exit(0)
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)
    try:
        agent.start()
        while True:
            time.sleep(1)
    except (KeyboardInterrupt, SystemExit):
        agent.stop()
    except Exception as e:
        logger.critical('Fatal error encountered in Edge Agent: %s', e, exc_info=True)
        agent.stop()
        sys.exit(1)
if __name__ == '__main__':
    main()
