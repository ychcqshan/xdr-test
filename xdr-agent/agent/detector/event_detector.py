import threading
import time
import subprocess
import logging
from datetime import datetime, timedelta
import xml.etree.ElementTree as ET

logger = logging.getLogger('xdr-agent')

class EventDetector(threading.Thread):
    """
    基于 Windows EventViewer 的实时事件检测引擎 (Queue A)
    - 负责拉取 4104 (PowerShell Script Block)
    - 负责拉取关键注册表/服务创建事件
    """
    def __init__(self, callback):
        super().__init__(daemon=True)
        self.callback = callback
        self.running = False
        self.poll_interval = 10
        self.last_ts_powershell = datetime.utcnow() - timedelta(seconds=self.poll_interval)
        self.last_record_id = 0

    def start(self):
        self.running = True
        logger.info("启动 EventDetector (4104 事件与行为拦截)...")
        super().start()

    def stop(self):
        self.running = False

    def run(self):
        import platform
        if platform.system() != "Windows":
            logger.info("EventDetector 只在 Windows 平台运行.")
            return

        while self.running:
            try:
                self._poll_powershell_4104()
            except Exception as e:
                logger.error(f"EventDetector 轮询失败: {e}")
            time.sleep(self.poll_interval)

    def _poll_powershell_4104(self):
        """拉取 Microsoft-Windows-PowerShell/Operational 中的 4104 事件"""
        # query = f"*[System[(EventID=4104) and TimeCreated[timediff(@SystemTime) <= {self.poll_interval * 1000}]]]"
        # 简单使用 wevtutil 根据 Record Number 查询增量，或 TimeCreated 进行查询
        # 为了兼容性，我们直接查最近 10 条并用时间戳过滤
        cmd = 'wevtutil qe Microsoft-Windows-PowerShell/Operational /q:"*[System[(EventID=4104)]]" /f:xml /rd:true /c:15'
        try:
            output = subprocess.check_output(cmd, shell=True, text=True, stderr=subprocess.DEVNULL)
            if not output.strip():
                return

            # wevtutil 输出结构可能是独立的 <Event> 节点拼在一起，需要伪装成根节点才能解析 XML
            events_xml = f"<Events>{output}</Events>"
            root = ET.fromstring(events_xml)
            
            ns = {'ns': 'http://schemas.microsoft.com/win/2004/08/events/event'}
            
            for event in root.findall('ns:Event', ns):
                system = event.find('ns:System', ns)
                if system is None: continue
                
                # 获取时间与 RecordID 避免重复处理
                time_created = system.find('ns:TimeCreated', ns)
                record_id_elem = system.find('ns:EventRecordID', ns)
                
                if time_created is None or record_id_elem is None: continue
                
                record_id = int(record_id_elem.text)
                if record_id <= self.last_record_id:
                    continue
                
                sys_time_str = time_created.attrib.get('SystemTime', '')
                if not sys_time_str: continue

                # 解析 2024-03-29T10:00:00.000000Z
                try:
                    sys_time = datetime.strptime(sys_time_str[:19], "%Y-%m-%dT%H:%M:%S")
                    if sys_time <= self.last_ts_powershell:
                        continue
                except:
                    pass

                # 更新最新游标
                if record_id > self.last_record_id:
                    self.last_record_id = record_id
                    
                # 提取明文脚本 (4104 的明文通常在 EventData 的 ScriptBlockText)
                event_data = event.find('ns:EventData', ns)
                script_block = ""
                if event_data is not None:
                    for data in event_data.findall('ns:Data', ns):
                        if data.attrib.get('Name') == 'ScriptBlockText':
                            script_block = data.text
                            break
                            
                if not script_block or not script_block.strip():
                    continue
                    
                # 触发告警 / 行为数据上报 Queue A
                # 针对高危指令做初步匹配，如果是常规业务直接上报 INFO 级别日志
                alert = {
                    "type": "POWERSHELL_SCRIPT_BLOCK",
                    "event_id": 4104,
                    "module": "PowerShell",
                    "cmd": script_block[:1000],  # 取前1000防爆
                    "details": script_block,
                    "timestamp": sys_time_str,
                    "action": "DETECTED",
                    "severity": "LOW"
                }

                # 简单过滤常见合法噪音 (如系统内部模块加载)
                lower_script = script_block.lower()
                whitelist = ["get-wmiobject", "get-process", "prompt", "microsoft.powershell"]
                if any(w in lower_script for w in whitelist) and len(lower_script) < 200:
                    continue

                if "iex" in lower_script or "downloadstring" in lower_script or "bypass" in lower_script or "enc" in lower_script:
                    alert["severity"] = "HIGH"
                    alert["message"] = "Suspicious PowerShell ScriptBlock Detected"

                if self.callback:
                    # 复用 main 中的 report_alert 上报
                    self.callback(alert)
                    
            self.last_ts_powershell = datetime.utcnow()
        except Exception:
            pass
