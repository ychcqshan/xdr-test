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
        seen = set()
        if platform.system() == "Windows":
            try:
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
                                    subkey_name = winreg.EnumKey(key, i)
                                    with winreg.OpenKey(key, subkey_name) as subkey:
                                        name, _ = winreg.QueryValueEx(subkey, "DisplayName")
                                        version = ""
                                        install_date = ""
                                        publisher = ""
                                        try:
                                            version, _ = winreg.QueryValueEx(subkey, "DisplayVersion")
                                        except FileNotFoundError:
                                            pass
                                        try:
                                            raw_date, _ = winreg.QueryValueEx(subkey, "InstallDate")
                                            if raw_date and len(raw_date) == 8:
                                                install_date = f"{raw_date[:4]}-{raw_date[4:6]}-{raw_date[6:8]}"
                                            else:
                                                install_date = str(raw_date) if raw_date else ""
                                        except FileNotFoundError:
                                            pass
                                        try:
                                            publisher, _ = winreg.QueryValueEx(subkey, "Publisher")
                                        except FileNotFoundError:
                                            pass
                                        dedup_key = f"{name}|{version}"
                                        if dedup_key not in seen:
                                            seen.add(dedup_key)
                                            softwares.append({
                                                "name": name,
                                                "version": version,
                                                "installDate": install_date,
                                                "publisher": publisher
                                            })
                                except Exception:
                                    continue
                    except FileNotFoundError:
                        continue
            except Exception:
                pass
        else:
            try:
                res = subprocess.check_output(["dpkg-query", "-W", "-f=${Package};${Version}\n"], text=True)
                for line in res.splitlines():
                    parts = line.split(";")
                    if len(parts) == 2:
                        softwares.append({"name": parts[0], "version": parts[1], "installDate": "", "publisher": ""})
            except Exception:
                pass
        return softwares

    def _get_usb_devices(self):
        devices = []
        now_ts = datetime.now().isoformat()
        if platform.system() == "Windows":
            try:
                # 升级：改用 PowerShell 获取磁盘驱动器、U盘及光驱
                # 监控类目：USB (通用)、DiskDrive (移动硬盘/U盘)、CDROM (外置光驱)
                ps_cmd = (
                    "Get-PnpDevice -Class 'USB', 'DiskDrive', 'CDROM' | "
                    "Where-Object { $_.Status -eq 'OK' -or $_.Present -eq $true } | "
                    "Select-Object FriendlyName, InstanceId, Status, Class | ConvertTo-Json"
                )
                res = subprocess.check_output(
                    ['powershell', '-Command', ps_cmd],
                    text=True, stderr=subprocess.DEVNULL, timeout=15
                )
                
                if res.strip():
                    import json
                    raw_data = json.loads(res)
                    # 处理单个对象或对象列表
                    items = raw_data if isinstance(raw_data, list) else [raw_data]
                    
                    for item in items:
                        name = item.get("FriendlyName")
                        device_id = item.get("InstanceId")
                        status = item.get("Status")
                        pnp_class = item.get("Class")
                        
                        if name and device_id:
                            # 过滤掉一些基础的控制器，保留具体的设备
                            if "Host Controller" in name or "Root Hub" in name:
                                continue
                                
                            devices.append({
                                "name": name,
                                "deviceId": device_id,
                                "lastSeen": now_ts,
                                "status": "CONNECTED" if status == "OK" else "HISTORY",
                                "class": pnp_class
                            })
            except Exception:
                pass
        else:
            try:
                res = subprocess.check_output(["lsusb"], text=True)
                for line in res.splitlines():
                    devices.append({
                        "name": line.strip(), 
                        "deviceId": line.strip()[:30], 
                        "lastSeen": now_ts,
                        "status": "CONNECTED"
                    })
            except Exception:
                pass
        return devices

    def _get_logins(self):
        logins = []
        if platform.system() == "Windows":
            # 1. 获取当前登录用户 (psutil)
            try:
                for user in psutil.users():
                    logins.append({
                        "userName": user.name,
                        "terminal": user.terminal or "CONSOLE",
                        "host": user.host or "localhost",
                        "loginTime": datetime.fromtimestamp(user.started).isoformat(),
                        "status": "ACTIVE"
                    })
            except Exception:
                pass
            
            # 2. 尝试获取历史登录记录 (通过 wevtutil 读取安全日志 EventID 4624)
            # 仅取最近 10 条历史以防上报过大
            try:
                cmd = 'wevtutil qe Security /q:"*[System[(EventID=4624)]]" /f:text /rd:true /c:10'
                output = subprocess.check_output(cmd, shell=True, text=True, stderr=subprocess.DEVNULL)
                # 简单解析文本格式输出
                current_event = {}
                for line in output.splitlines():
                    line = line.strip()
                    if "Event[" in line: 
                        if current_event: logins.append(current_event)
                        current_event = {"status": "HISTORICAL"}
                    elif "Date:" in line:
                        dt_str = line.replace("Date:", "").strip()
                        try:
                            # 转换格式或直接保留
                            current_event["loginTime"] = dt_str
                        except: pass
                    elif "TargetUserName:" in line:
                        current_event["userName"] = line.split(":")[-1].strip()
                    elif "IpAddress:" in line:
                        current_event["host"] = line.split(":")[-1].strip()
                if current_event and "userName" in current_event:
                    logins.append(current_event)
            except Exception:
                pass
        else:
            # Linux: 通过 last 命令或 psutil
            try:
                for user in psutil.users():
                    logins.append({
                        "userName": user.name,
                        "terminal": user.terminal,
                        "host": user.host,
                        "loginTime": datetime.fromtimestamp(user.started).isoformat(),
                        "status": "ACTIVE"
                    })
            except Exception:
                pass
        
        # 去重：以 userName + loginTime 为准
        unique_logins = []
        seen = set()
        for l in logins:
            key = f"{l.get('userName')}|{l.get('loginTime')}"
            if key not in seen:
                seen.add(key)
                unique_logins.append(l)
        
        return unique_logins
