from abc import ABC, abstractmethod
from typing import Any, Dict

class MetricCollector(ABC):

    @abstractmethod
    def collect(self) -> Dict[str, Any]:
        pass

    @abstractmethod
    def get_name(self) -> str:
        pass

    def is_available(self) -> bool:
        return True
