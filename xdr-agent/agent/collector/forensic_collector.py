import os
import platform
import subprocess
import json
import base64
import logging
from datetime import datetime
from .base import BaseCollector

logger = logging.getLogger('xdr-agent')

class ForensicCollector(BaseCollector):
    """
    深度取证采集器：识别入侵痕迹、解码恶意载荷、排查持久化项
    """
    def __init__(self, process_collector=None, port_collector=None):
        self._process_collector = process_collector
        self._port_collector = port_collector
        self.suspicious_keywords = ["curl", "wget", "powershell", "msiexec", "certutil", "bitsadmin", "regsvr32", "rundll32", "-enc", "downloadstring", "iex"]

    def name(self) -> str:
        return "INTRUSION_REPORT"

    def collect(self):
        start_time = datetime.now()
        report = {
            "summary": {
                "scanTime": start_time.isoformat(),
                "riskScore": 0,
                "foundAnomalies": 0
            },
            "persistence": self._check_persistence(),
            "processes": self._analyze_processes(),
            "network": self._analyze_network(),
            "fileResidue": self._check_file_residue(),
            "accounts": self._check_accounts()
        }
        
        # 简单后端分析预处理：统计异常数
        anomalies = 0
        # 遍历 dimensions
        dims = ["persistence", "processes", "network", "fileResidue"]
        for dim in dims:
            data = report.get(dim)
            if isinstance(data, list):
                anomalies += len([x for x in data if x.get("isSuspicious", False) or dim == "fileResidue"])
            elif isinstance(data, dict):
                # persistence 内部是字典
                for sub in data.values():
                    if isinstance(sub, list):
                        anomalies += len([x for x in sub if x.get("isSuspicious", False)])
        
        report["summary"]["foundAnomalies"] = anomalies
        report["summary"]["scanDuration"] = f"{(datetime.now() - start_time).total_seconds():.2f}s"
        
        return report

    def _check_persistence(self):
        """排查持久化项：计划任务、注册表、WMI"""
        res = {"scheduledTasks": [], "registry": [], "wmiSubscriptions": []}
        if platform.system() != "Windows":
            return res

        # 1. 计划任务 (过滤掉 Microsoft 签名)
        try:
            ps_cmd = (
                "Get-ScheduledTask | Where-Object { $_.Author -notlike '*Microsoft*' -and $_.State -ne 'Disabled' } | "
                "Select-Object TaskName, TaskPath, @{Name='Action'; Expression={$_.Actions.Execute + ' ' + $_.Actions.Arguments}}, Author | ConvertTo-Json"
            )
            raw = self._run_ps(ps_cmd)
            if raw:
                tasks = json.loads(raw)
                tasks = tasks if isinstance(tasks, list) else [tasks]
                for t in tasks:
                    item = {
                        "name": t.get("TaskName"),
                        "path": t.get("TaskPath"),
                        "action": t.get("Action"),
                        "author": t.get("Author"),
                        "isSuspicious": False
                    }
                    # 检查可疑关键词与 Base64
                    self._audit_payload(item, "action")
                    res["scheduledTasks"].append(item)
        except Exception as e:
            logger.error(f"Forensic: ScheduledTask check failed: {e}")

        # 2. 注册表启动项
        try:
            import winreg
            reg_paths = [
                (winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Windows\CurrentVersion\Run"),
                (winreg.HKEY_CURRENT_USER, r"Software\Microsoft\Windows\CurrentVersion\Run")
            ]
            for hive, path in reg_paths:
                try:
                    with winreg.OpenKey(hive, path) as key:
                        for i in range(winreg.QueryInfoKey(key)[1]):
                            name, value, _ = winreg.EnumValue(key, i)
                            item = {
                                "path": path,
                                "key": name,
                                "value": value,
                                "isSuspicious": False
                            }
                            self._audit_payload(item, "value")
                            res["registry"].append(item)
                except: pass
        except: pass

        return res

    def _analyze_processes(self):
        """分析进程异常：路径伪装等"""
        suspicious = []
        # 复用 ProcessCollector 数据
        if not self._process_collector:
            logger.warning("Forensic: ProcessCollector instance not provided!")
            return suspicious
            
        data = self._process_collector.collect()
        procs = data.get("processes", [])
        
        sys_names = ["svchost.exe", "lsass.exe", "services.exe", "wininit.exe", "smss.exe"]
        for p in procs:
            path = p.get("path", "").lower()
            name = p.get("name", "").lower()
            
            # 1. 路径伪装检查
            if name in sys_names and "system32" not in path and path != "":
                suspicious.append({
                    "pid": p.get("pid"),
                    "name": name,
                    "path": path,
                    "isSuspicious": True,
                    "reason": "System process running from non-system directory"
                })
            
            # 2. 命令行可疑检查 (LotL)
            cmdline = p.get("cmdline", "")
            if any(k in cmdline.lower() for k in self.suspicious_keywords):
                item = {"pid": p.get("pid"), "name": name, "cmdline": cmdline, "isSuspicious": False}
                self._audit_payload(item, "cmdline")
                if item.get("isSuspicious"): # 如果审计后确实可疑
                     suspicious.append(item)

        return suspicious

    def _analyze_network(self):
        """分析网络异常：反弹 Shell 等"""
        anomalies = []
        if not self._port_collector:
            return anomalies
            
        data = self._port_collector.collect()
        conns = data.get("ports", [])
        
        # 常见反弹 Shell 进程
        shell_names = ["cmd.exe", "powershell.exe", "sh", "bash", "nc.exe"]
        
        for c in conns:
            if c.get("status") == "ESTABLISHED":
                remote = c.get("remoteAddr", "")
                # 过滤掉内网和本地
                if remote.startswith("127.") or remote.startswith("192.168.") or remote.startswith("10."):
                    continue
                
                # 查找所属进程名 (如果没在数据里，尝试动态实时获取)
                pid = c.get("pid")
                pname = ""
                try:
                    import psutil
                    pname = psutil.Process(pid).name().lower()
                except: pass
                
                if pname in shell_names:
                    anomalies.append({
                        "localAddr": c.get("localAddr"),
                        "remoteAddr": remote,
                        "pid": pid,
                        "processName": pname,
                        "isSuspicious": True,
                        "reason": "Reverse shell pattern detected (cmd/ps connected to external IP)"
                    })
        return anomalies

    def _check_file_residue(self):
        """检查敏感目录近期创建的可疑文件"""
        residue = []
        extensions = [".exe", ".ps1", ".vbs", ".js", ".jsp", ".bat"]
        dirs = []
        if platform.system() == "Windows":
            dirs = ["C:\\Windows\\Temp", os.path.join(os.environ.get("TEMP", ""), ""), "C:\\Users\\Public"]
        
        for d in dirs:
            if not os.path.exists(d): continue
            try:
                for f in os.listdir(d):
                    fpath = os.path.join(d, f)
                    if not os.path.isfile(fpath): continue
                    ext = os.path.splitext(f)[1].lower()
                    if ext in extensions:
                        mtime = os.path.getmtime(fpath)
                        # 检查最近 48 小时
                        if (datetime.now().timestamp() - mtime) < 48 * 3600:
                            residue.append({
                                "path": fpath,
                                "name": f,
                                "creationTime": datetime.fromtimestamp(mtime).isoformat(),
                                "size": os.path.getsize(fpath)
                            })
            except: pass
        return residue

    def _check_accounts(self):
        """简单账号异常检查"""
        res = {"users": [], "anomalies": []}
        if platform.system() == "Windows":
            try:
                # 获取 LocalUser
                raw = self._run_ps("Get-LocalUser | Select-Object Name, Enabled | ConvertTo-Json")
                if raw:
                    import json
                    users = json.loads(raw)
                    res["users"] = users if isinstance(users, list) else [users]
            except: pass
        return res

    def _audit_payload(self, item, field_name):
        """审计载荷：Base64 解码与关键词二次确认"""
        val = item.get(field_name, "")
        if not val: return
        
        # 1. 尝试寻找并还原 Base64 (Windows 常用 -enc)
        import re
        match = re.search(r"-e(?:ncodedcommand)?\s+([A-Za-z0-9+/=]{20,})", val, re.I)
        if match:
            b64_str = match.group(1)
            try:
                import base64
                # PS 默认 utf-16le 编码
                decoded_bytes = base64.b64decode(b64_str)
                # 尝试 utf-16le (Windows PS 默认), 失败则 utf-8
                try:
                    decoded = decoded_bytes.decode('utf-16le')
                except:
                    decoded = decoded_bytes.decode('utf-8', errors='ignore')
                
                item["decodedCommand"] = decoded
                item["isSuspicious"] = True
                item["reason"] = f"Detected hidden payload (Base64)"
                # 检查解码后是否包含高风险词
                if "downloadstring" in decoded.lower() or "iex" in decoded.lower():
                    item["reason"] += " + DownloadExec pattern"
            except: pass
            
        # 2. 关键词命中所占权重
        low_val = val.lower()
        if any(k in low_val for k in ["downloadstring", "invoke-expression", "iex", "Invoke-WebRequest"]):
            item["isSuspicious"] = True
            item["reason"] = item.get("reason", "") + " | High Risk Keywords"

    def _run_ps(self, cmd):
        try:
            return subprocess.check_output(["powershell", "-Command", cmd], text=True, stderr=subprocess.DEVNULL, timeout=15)
        except:
            return ""
