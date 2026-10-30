import enum
import logging
import threading
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable, Dict, List, Optional
logger = logging.getLogger('edge_agent.events')

class Event(enum.Enum):
    METRIC_COLLECTED = 'metric_collected'
    CONNECTION_LOST = 'connection_lost'
    CONNECTION_RESTORED = 'connection_restored'
    BUFFER_FULL = 'buffer_full'
    AGENT_STARTED = 'agent_started'
    AGENT_STOPPED = 'agent_stopped'
    ERROR = 'error'
    RECOVERY_STARTED = 'recovery_started'
    RECOVERY_COMPLETED = 'recovery_completed'
    BATCH_SENT = 'batch_sent'
    BATCH_FAILED = 'batch_failed'
EventCallback = Callable[[Event, Optional[Any]], None]

class EventBus:

    def __init__(self, max_workers: int=4) -> None:
        self._subscribers: Dict[Event, List[EventCallback]] = {}
        self._lock = threading.Lock()
        self._executor = ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix='event-bus')

    def subscribe(self, event_type: Event, callback: EventCallback) -> None:
        with self._lock:
            self._subscribers.setdefault(event_type, []).append(callback)
        logger.debug('Subscriber added for %s: %s', event_type.value, callback.__qualname__)

    def unsubscribe(self, event_type: Event, callback: EventCallback) -> None:
        with self._lock:
            subscribers = self._subscribers.get(event_type, [])
            try:
                subscribers.remove(callback)
                logger.debug('Subscriber removed for %s: %s', event_type.value, callback.__qualname__)
            except ValueError:
                pass

    def publish(self, event_type: Event, data: Optional[Any]=None) -> None:
        logger.debug('Event published: %s (data=%s)', event_type.value, data)
        with self._lock:
            callbacks = list(self._subscribers.get(event_type, []))
        for cb in callbacks:
            self._executor.submit(self._safe_invoke, cb, event_type, data)

    @staticmethod
    def _safe_invoke(callback: EventCallback, event_type: Event, data: Optional[Any]) -> None:
        try:
            callback(event_type, data)
        except Exception:
            logger.exception('Unhandled exception in event callback %s for %s', callback.__qualname__, event_type.value)

    def shutdown(self) -> None:
        self._executor.shutdown(wait=True)
        logger.info('EventBus shut down')
