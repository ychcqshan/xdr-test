"""
A-COLL-001: 进程采集器
"""
import hashlib
import logging
from typing import List

import psutil

from agent.collector.base import BaseCollector

logger = logging.getLogger('xdr-agent')


class ProcessCollector(BaseCollector):

    def name(self) -> str:
        return "PROCESS"

    def collect(self) -> List[dict]:
        result = []
        for proc in psutil.process_iter(
            ['pid', 'name', 'exe', 'cmdline', 'username',
             'ppid', 'create_time', 'cpu_percent', 'memory_percent']
        ):
            try:
                info = proc.info
                exe_path = info.get('exe') or ''
                file_hash = ''
                if exe_path:
                    try:
                        with open(exe_path, 'rb') as f:
                            # 只读前64KB算hash，避免大文件阻塞
                            file_hash = hashlib.md5(f.read(65536)).hexdigest()
                    except (PermissionError, FileNotFoundError, OSError):
                        pass

                result.append({
                    'pid': info['pid'],
                    'name': info['name'],
                    'path': exe_path,
                    'cmdline': ' '.join(info.get('cmdline') or []),
                    'user': info.get('username', ''),
                    'ppid': info.get('ppid', 0),
                    'fileHash': file_hash,
                    'cpuPercent': info.get('cpu_percent', 0),
                    'memoryPercent': round(info.get('memory_percent', 0), 2),
                })
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                continue

        logger.debug(f"进程采集完成, 共 {len(result)} 个进程")
        return result
