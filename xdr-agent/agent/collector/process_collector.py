import logging
import psutil
from datetime import datetime

logger = logging.getLogger('xdr-agent')
from .base import BaseCollector

class ProcessCollector(BaseCollector):
    """
    采集进程详情，包括命令行、用户、资源占用等
    """
    def name(self) -> str:
        return "PROCESS"

    def collect(self):
        processes = []
        for proc in psutil.process_iter(['pid', 'name', 'username', 'status', 'create_time', 'cpu_percent', 'memory_percent']):
            try:
                pinfo = proc.info
                # 额外获取命令行参数
                try:
                    pinfo['cmdline'] = " ".join(proc.cmdline())
                except (psutil.AccessDenied, psutil.NoSuchProcess):
                    pinfo['cmdline'] = ""
                
                # 转换启动时间为 ISO 格式
                if pinfo['create_time']:
                    pinfo['create_time'] = datetime.fromtimestamp(pinfo['create_time']).isoformat()
                
                processes.append(pinfo)
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass
        
        logger.debug(f"[PROCESS] 采集到 {len(processes)} 个进程")
        for p in processes[:10]:
            logger.debug(f"  -> PID={p.get('pid')} Name={p.get('name')} User={p.get('username')}")

        return {
            "processes": processes,
            "count": len(processes),
            "timestamp": datetime.now().isoformat()
        }
