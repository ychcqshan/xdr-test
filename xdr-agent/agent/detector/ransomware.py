import os
import random
import string
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from datetime import datetime

class HoneyfileHandler(FileSystemEventHandler):
    def __init__(self, callback, file_path):
        self.callback = callback
        self.file_path = file_path

    def on_modified(self, event):
        if not event.is_directory and os.path.abspath(event.src_path) == os.path.abspath(self.file_path):
            self.callback("MODIFIED", event.src_path)

    def on_deleted(self, event):
        if not event.is_directory and os.path.abspath(event.src_path) == os.path.abspath(self.file_path):
            self.callback("DELETED", event.src_path)

class RansomwareDetector:
    """
    诱饵文件检测模块 (Honeyfiles)
    """
    def __init__(self, alert_callback):
        self.alert_callback = alert_callback
        self.honey_path = os.path.join(os.path.expanduser("~"), "Documents", ".sys_cache_info")
        self.observer = Observer()

    def deploy(self):
        # 如果诱饵文件不存在，则创建
        if not os.path.exists(self.honey_path):
            try:
                with open(self.honey_path, "w") as f:
                    f.write("".join(random.choices(string.ascii_letters + string.digits, k=1024)))
                # 设置隐藏属性 (Windows)
                if os.name == 'nt':
                    import ctypes
                    ctypes.windll.kernel32.SetFileAttributesW(self.honey_path, 0x02) # FILE_ATTRIBUTE_HIDDEN
            except Exception as e:
                print(f"Failed to deploy honeyfile: {e}")

    def start(self):
        self.deploy()
        event_handler = HoneyfileHandler(self._on_honey_event, self.honey_path)
        self.observer.schedule(event_handler, os.path.dirname(self.honey_path), recursive=False)
        self.observer.start()

    def _on_honey_event(self, action, path):
        alert = {
            "type": "RANSOMWARE_ALERT",
            "severity": "CRITICAL",
            "message": f"Honeyfile {action}: {path}",
            "timestamp": datetime.now().isoformat()
        }
        self.alert_callback(alert)

    def stop(self):
        self.observer.stop()
        self.observer.join()
