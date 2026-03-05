import platform
import psutil
import socket
from datetime import datetime
from .base import BaseCollector

class HostCollector(BaseCollector):
    """
    采集主机的基础信息与硬件参数
    """
    def name(self) -> str:
        return "HOST"

    def collect(self):
        try:
            cpu_info = {
                "brand": platform.processor(),
                "arch": platform.machine(),
                "physical_cores": psutil.cpu_count(logical=False),
                "total_cores": psutil.cpu_count(logical=True),
                "freq_mhz": psutil.cpu_freq().max if psutil.cpu_freq() else 0
            }
            
            mem = psutil.virtual_memory()
            memory_info = {
                "total": mem.total,
                "available": mem.available,
                "percent": mem.percent
            }
            
            # 获取磁盘信息
            disks = []
            for part in psutil.disk_partitions():
                try:
                    usage = psutil.disk_usage(part.mountpoint)
                    disks.append({
                        "device": part.device,
                        "mountpoint": part.mountpoint,
                        "fstype": part.fstype,
                        "total": usage.total,
                        "percent": usage.percent
                    })
                except Exception:
                    continue

            return {
                "hostname": socket.gethostname(),
                "os": platform.system(),
                "os_version": platform.version(),
                "os_release": platform.release(),
                "boot_time": datetime.fromtimestamp(psutil.boot_time()).isoformat(),
                "cpu": cpu_info,
                "memory": memory_info,
                "disks": disks,
                "timestamp": datetime.now().isoformat()
            }
        except Exception as e:
            return {"error": f"Host collection failed: {str(e)}"}
