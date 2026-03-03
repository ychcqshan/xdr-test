import psutil
from datetime import datetime
from .base import BaseCollector

class PortCollector(BaseCollector):
    """
    采集系统端口开放情况并关联进程名
    """
    def collect(self):
        ports = []
        try:
            connections = psutil.net_connections(kind='inet')
            for conn in connections:
                if conn.status in ['LISTEN', 'ESTABLISHED']:
                    port_info = {
                        "local_addr": f"{conn.laddr.ip}:{conn.laddr.port}",
                        "remote_addr": f"{conn.raddr.ip}:{conn.raddr.port}" if conn.raddr else None,
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
            
        return {
            "ports": ports,
            "timestamp": datetime.now().isoformat()
        }
