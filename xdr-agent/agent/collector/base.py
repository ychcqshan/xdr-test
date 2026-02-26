"""
采集器基类
"""
from abc import ABC, abstractmethod
from typing import List


class BaseCollector(ABC):
    """所有采集器的基类"""

    @abstractmethod
    def name(self) -> str:
        """采集器名称"""
        ...

    @abstractmethod
    def collect(self) -> List[dict]:
        """执行一次采集，返回数据列表"""
        ...

    @property
    def interval(self) -> int:
        """采集间隔(秒)，可通过策略动态调整"""
        return 60
