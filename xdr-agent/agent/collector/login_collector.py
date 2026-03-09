"""
A-COLL-004: 登录事件采集器
"""
import logging
import platform
from typing import List
from datetime import datetime

import psutil

from agent.collector.base import BaseCollector

logger = logging.getLogger('xdr-agent')


class LoginCollector(BaseCollector):

    def name(self) -> str:
        return "LOGIN"

    def collect(self) -> List[dict]:
        result = []
        for user in psutil.users():
            result.append({
                'userName': user.name,
                'terminal': user.terminal or '',
                'host': user.host or 'localhost',
                'loginType': 'REMOTE' if user.host and user.host != 'localhost' else 'LOCAL',
                'loginTime': datetime.fromtimestamp(user.started).isoformat(),
                'pid': getattr(user, 'pid', 0),
            })

        logger.debug(f"登录事件采集完成, 共 {len(result)} 个会话")
        return result
