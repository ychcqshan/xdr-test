import os
import platform
import threading
from datetime import datetime

class CommandMonitor:
    """
    指令执行监控：Windows (Event 4688), Linux (Auditd)
    """
    def __init__(self, alert_callback):
        self.alert_callback = alert_callback
        self._stop_event = threading.Event()

    def start(self):
        if platform.system() == "Windows":
            threading.Thread(target=self._monitor_windows_4688, daemon=True).start()
        else:
            threading.Thread(target=self._monitor_linux_auditd, daemon=True).start()

    def _monitor_windows_4688(self):
        """
        监听 Windows Security Event 4688
        注意：生产环境需使用 pywin32.winevt.EvtSubscribe
        此为逻辑示意实现
        """
        try:
            import win32evtlog
            query = "*[System[EventID=4688]]"
            # 订阅 Security 通道
            handle = win32evtlog.EvtSubscribe("Security", win32evtlog.EvtSubscribeStartAtOldestEvents, None, query)
            while not self._stop_event.is_set():
                events = win32evtlog.EvtNext(handle, 10)
                for event in events:
                    # 解析 XML 提取 ProcessCommandLine
                    xml = win32evtlog.EvtRender(event, win32evtlog.EvtRenderEventXml)
                    # 简单字符串提取逻辑
                    if "ProcessCommandLine" in xml:
                        cmd_line = xml.split("ProcessCommandLine'>")[1].split("</Data>")[0]
                        self._process_command("WINDOWS_CMD", cmd_line)
        except Exception:
            pass

    def _monitor_linux_auditd(self):
        """
        监听 Linux /var/log/audit/audit.log
        """
        audit_log = "/var/log/audit/audit.log"
        if not os.path.exists(audit_log):
            return

        try:
            with open(audit_log, "r") as f:
                f.seek(0, 2) # Go to end
                while not self._stop_event.is_set():
                    line = f.readline()
                    if not line:
                        threading.Event().wait(0.1)
                        continue
                    if "type=EXECVE" in line:
                        # 提取参数
                        parts = line.split(" ")
                        cmd_parts = []
                        for p in parts:
                            if p.startswith("a0=") or p.startswith("a1=") or p.startswith("a2="):
                                cmd_parts.append(p.split("=")[1].strip('"'))
                        self._process_command("LINUX_CMD", " ".join(cmd_parts))
        except Exception:
            pass

    def _process_command(self, source, cmd_line):
        if not cmd_line: return
        alert = {
            "type": "COMMAND_EXEC",
            "source": source,
            "cmd": cmd_line,
            "timestamp": datetime.now().isoformat()
        }
        self.alert_callback(alert)

    def stop(self):
        self._stop_event.set()
