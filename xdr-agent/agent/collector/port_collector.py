import logging
import psutil
from datetime import datetime
from .base import BaseCollector

logger = logging.getLogger(__name__)

class PortCollector(BaseCollector):
    """
    采集系统端口开放情况并关联进程名
    """
    def name(self) -> str:
        return "NETWORK" # Align with backend AssetType

    def collect(self):
        ports = []
        try:
            connections = psutil.net_connections(kind='inet')
            for conn in connections:
                if conn.status in ['LISTEN', 'ESTABLISHED']:
                    port_info = {
                        "localAddr": f"{conn.laddr.ip}:{conn.laddr.port}",
                        "remoteAddr": f"{conn.raddr.ip}:{conn.raddr.port}" if conn.raddr else None,
                        "status": conn.status,
                        "pid": conn.pid,
                        "protocol": "TCP" if conn.type == 1 else "UDP"
                    }
                    
                    # 尝试关联进程名
                    if conn.pid:
                        try:
                            port_info["process_name"] = psutil.Process(conn.pid).name()
                        except (psutil.NoSuchProcess, psutil.AccessDenied):
                            port_info["process_name"] = "unknown"
                    else:
                        port_info["process_name"] = "system"
                        
                    ports.append(port_info)
        except Exception:
            pass
            
        logger.info(f"[PortCollector] 采集到 {len(ports)} 条端口记录")
        for i, p in enumerate(ports[:10]):
            logger.info(f"  [{i+1}] {p.get('protocol')} {p.get('local_addr')} -> {p.get('remote_addr')} ({p.get('process_name')}) [{p.get('status')}]")

        return {
            "ports": ports,
            "timestamp": datetime.now().isoformat()
        }
