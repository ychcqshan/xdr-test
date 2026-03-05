import os
import platform
import psutil
import subprocess
from datetime import datetime
from .base import BaseCollector

class ExtraCollector(BaseCollector):
    """
    采集软件列表、USB设备历史、登录审计
    """
    def name(self) -> str:
        return "EXTRA"

    def collect(self):
        return {
            "softwares": self._get_softwares(),
            "usb_devices": self._get_usb_devices(),
            "logins": self._get_logins(),
            "timestamp": datetime.now().isoformat()
        }

    def _get_softwares(self):
        softwares = []
        if platform.system() == "Windows":
```python
            import winreg
            reg_paths = [
                (winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall"),
                (winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"),
                (winreg.HKEY_CURRENT_USER, r"Software\Microsoft\Windows\CurrentVersion\Uninstall")
            ]
            for hive, key_path in reg_paths:
                try:
                    with winreg.OpenKey(hive, key_path) as key:
                        for i in range(winreg.QueryInfoKey(key)[0]):
                            try:
                                subkey
            try:
                import winreg
                keys = [
                    r"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall",
                    r"SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"
                ]
                for key_path in keys:
                    with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, key_path) as key:
                        for i in range(winreg.QueryInfoKey(key)[0]):
                            try:
                                subkey_name = winreg.EnumKey(key, i)
                                with winreg.OpenKey(key, subkey_name) as subkey:
                                    name, _ = winreg.QueryValueEx(subkey, "DisplayName")
                                    version, _ = winreg.QueryValueEx(subkey, "DisplayVersion")
                                    softwares.append({"name": name, "version": version})
                            except Exception:
                                continue
            except Exception:
                pass
        else:
            # Linux: 使用 dpkg-query 示例
            try:
                res = subprocess.check_output(["dpkg-query", "-W", "-f=${Package};${Version}\n"], text=True)
                for line in res.splitlines():
                    parts = line.split(";")
                    if len(parts) == 2:
                        softwares.append({"name": parts[0], "version": parts[1]})
            except Exception:
                pass
        return softwares

    def _get_usb_devices(self):
        devices = []
        if platform.system() == "Windows":
            # 模拟获取，实际可使用 WMI
            pass
        else:
            # Linux: 简单列出 USB 设备
            try:
                res = subprocess.check_output(["lsusb"], text=True)
                for line in res.splitlines():
                    devices.append({"raw": line})
            except Exception:
                pass
        return devices

    def _get_logins(self):
        logins = []
        try:
            # 使用 psutil 获取当前在线用户
            for user in psutil.users():
                logins.append({
                    "userName": user.name,
                    "terminal": user.terminal,
                    "host": user.host,
                    "loginTime": datetime.fromtimestamp(user.started).isoformat()
                })
        except Exception:
            pass
        return logins
