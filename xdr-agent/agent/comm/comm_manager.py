"""
A-COMM-001~008: 通信管理器
"""
import gzip
import json
import logging
import sqlite3
import os
from typing import Optional

import requests

from agent.core.config import get_server_url, get_agent_id

logger = logging.getLogger('xdr-agent')
DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'cache.db')


class CommManager:
    """Agent与后端的通信管理"""

    def __init__(self):
        self.base_url = get_server_url()
        self.session = requests.Session()
        self.session.headers.update({'Content-Type': 'application/json'})
        self._init_cache_db()

    def _init_cache_db(self):
        """初始化本地SQLite缓存"""
        conn = sqlite3.connect(DB_PATH)
        conn.execute('''
            CREATE TABLE IF NOT EXISTS event_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                endpoint TEXT NOT NULL,
                payload TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()
        conn.close()

    def post(self, endpoint: str, data: dict, compress: bool = True) -> Optional[dict]:
        """
        发送POST请求，失败时缓存到本地
        A-COMM-002: GZIP压缩
        A-COMM-005: 断网续传
        """
        payload = json.dumps(data)
        try:
            headers = {}
            if compress:
                body = gzip.compress(payload.encode('utf-8'))
                headers['Content-Encoding'] = 'gzip'
            else:
                body = payload.encode('utf-8')

            resp = self.session.post(
                f"{self.base_url}{endpoint}",
                data=body,
                headers=headers,
                timeout=10
            )
            resp.raise_for_status()
            return resp.json()
        except Exception as e:
            logger.warning(f"发送失败 {endpoint}: {e}, 缓存到本地")
            self._cache_event(endpoint, payload)
            return None

    def get(self, endpoint: str, params: dict = None) -> Optional[dict]:
        """发送GET请求"""
        try:
            resp = self.session.get(
                f"{self.base_url}{endpoint}",
                params=params,
                timeout=10
            )
            resp.raise_for_status()
            return resp.json()
        except Exception as e:
            logger.warning(f"GET失败 {endpoint}: {e}")
            return None

    def heartbeat(self, asset_data: dict):
        """A-COMM-004: 心跳上报"""
        agent_id = get_agent_id()
        data = {**asset_data, 'agentId': agent_id}
        result = self.post('/api/v1/heartbeat', data, compress=False)
        if result:
            logger.info(f"心跳上报成功: {agent_id}")
        return result

    def report_event(self, event_type: str, event_data: dict, priority: str = 'LOW'):
        """A-COMM-003: 分级上报事件"""
        data = {
            'agentId': get_agent_id(),
            'eventType': event_type,
            'eventData': event_data,
            'priority': priority,
        }
        return self.post('/api/v1/events', data, compress=True)

    def flush_cache(self):
        """恢复网络后补传缓存数据"""
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.execute('SELECT id, endpoint, payload FROM event_cache ORDER BY created_at LIMIT 100')
        rows = cursor.fetchall()

        success_ids = []
        for row_id, endpoint, payload in rows:
            try:
                data = json.loads(payload)
                resp = self.session.post(
                    f"{self.base_url}{endpoint}",
                    json=data,
                    timeout=10
                )
                if resp.status_code < 400:
                    success_ids.append(row_id)
            except Exception:
                break  # 网络又断了，停止补传

        if success_ids:
            placeholders = ','.join('?' * len(success_ids))
            conn.execute(f'DELETE FROM event_cache WHERE id IN ({placeholders})', success_ids)
            conn.commit()
            logger.info(f"补传缓存数据 {len(success_ids)} 条")

        conn.close()

    def _cache_event(self, endpoint: str, payload: str):
        """缓存事件到本地SQLite"""
        conn = sqlite3.connect(DB_PATH)
        conn.execute(
            'INSERT INTO event_cache (endpoint, payload) VALUES (?, ?)',
            (endpoint, payload)
        )
        conn.commit()

        # 清理超过7天的缓存
        conn.execute("DELETE FROM event_cache WHERE created_at < datetime('now', '-7 days')")
        conn.commit()
        conn.close()
