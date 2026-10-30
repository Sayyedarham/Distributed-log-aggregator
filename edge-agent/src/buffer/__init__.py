import logging
logger = logging.getLogger('edge_agent.buffer')
try:
    from ._ring_buffer import RingBuffer as CRingBuffer
    RingBuffer = CRingBuffer
    HAS_C_EXTENSION = True
    logger.info('Loaded compiled C-extension RingBuffer.')
except (ImportError, AttributeError):
    from .ring_buffer import RingBuffer as PyRingBuffer
    RingBuffer = PyRingBuffer
    HAS_C_EXTENSION = False
    logger.info('Using native Python thread-safe RingBuffer.')
__all__ = ['RingBuffer', 'HAS_C_EXTENSION']
