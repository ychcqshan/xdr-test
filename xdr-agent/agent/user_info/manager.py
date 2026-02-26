"""
A-USER-002~004: 用户信息管理与加密存储
"""
import sqlite3
import json
import logging
import os
from cryptography.fernet import Fernet
from agent.user_info.collector_gui import UserInfoCollectorGUI
from agent.core.config import get_agent_id

logger = logging.getLogger('xdr-agent')
DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'cache.db')

# 简单的密钥生成/管理（实际应更安全）
SECRET_KEY_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), '.secret')

class UserInfoManager:
    def __init__(self, comm_manager):
        self.comm = comm_manager
        self.key = self._get_or_create_key()
        self.cipher = Fernet(self.key)
        self._init_db()

    def _get_or_create_key(self):
        if os.path.exists(SECRET_KEY_PATH):
            with open(SECRET_KEY_PATH, 'rb') as f:
                return f.read()
        else:
            key = Fernet.generate_key()
            with open(SECRET_KEY_PATH, 'wb') as f:
                f.write(key)
            return key

    def _init_db(self):
        conn = sqlite3.connect(DB_PATH)
        conn.execute('''
            CREATE TABLE IF NOT EXISTS user_info (
                agent_id TEXT PRIMARY KEY,
                encrypted_data BLOB NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()
        conn.close()

    def check_and_collect(self):
        """检查是否存在用户信息，不存在则弹窗采集"""
        agent_id = get_agent_id()
        if not agent_id:
            return

        conn = sqlite3.connect(DB_PATH)
        row = conn.execute('SELECT encrypted_data FROM user_info WHERE agent_id=?', (agent_id,)).fetchone()
        conn.close()

        if not row:
            logger.info("未发现用户信息，启动采集GUI...")
            gui = UserInfoCollectorGUI(self.save_and_report)
            gui.run()
        else:
            logger.debug("已存在用户信息")

    def save_and_report(self, data):
        """加密存储并上报后端"""
        agent_id = get_agent_id()
        encrypted_data = self.cipher.encrypt(json.dumps(data).encode())
        
        conn = sqlite3.connect(DB_PATH)
        conn.execute('INSERT OR REPLACE INTO user_info (agent_id, encrypted_data) VALUES (?, ?)', 
                     (agent_id, encrypted_data))
        conn.commit()
        conn.close()
        
        # 上报后端 S-ASSET-008
        self.comm.post(f'/api/v1/assets/{agent_id}/user-info', data)
        logger.info("用户信息已保存并上报")

    def get_user_info(self):
        agent_id = get_agent_id()
        conn = sqlite3.connect(DB_PATH)
        row = conn.execute('SELECT encrypted_data FROM user_info WHERE agent_id=?', (agent_id,)).fetchone()
        conn.close()
        
        if row:
            decrypted = self.cipher.decrypt(row[0])
            return json.loads(decrypted)
        return None
