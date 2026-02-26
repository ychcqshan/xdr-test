"""
A-COLL-002: 网络连接采集器
"""
import logging
from typing import List

import psutil

from agent.collector.base import BaseCollector

logger = logging.getLogger('xdr-agent')


class NetworkCollector(BaseCollector):

    def name(self) -> str:
        return "NETWORK"

    def collect(self) -> List[dict]:
        result = []
        for conn in psutil.net_connections(kind='inet'):
            try:
                local = f"{conn.laddr.ip}:{conn.laddr.port}" if conn.laddr else ""
                remote = f"{conn.raddr.ip}:{conn.raddr.port}" if conn.raddr else ""

                result.append({
                    'localAddr': local,
                    'remoteAddr': remote,
                    'protocol': 'TCP' if conn.type == 1 else 'UDP',
                    'status': conn.status,
                    'pid': conn.pid or 0,
                    'port': conn.laddr.port if conn.laddr else 0,
                })
            except Exception:
                continue

        logger.debug(f"网络连接采集完成, 共 {len(result)} 条连接")
        return result
