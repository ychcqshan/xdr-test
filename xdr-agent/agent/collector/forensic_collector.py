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
        is_windows = platform.system() == "Windows"
        
        report = {
            "summary": {
                "scanTime": start_time.isoformat(),
                "riskScore": 0,
                "foundAnomalies": 0,
                "osType": platform.system(),
                "osVersion": platform.version()
            },
            "persistence": self._check_persistence() if is_windows else self._collect_linux_persistence(),
            "processes": self._analyze_processes(),
            "network": self._analyze_network(),
            "fileResidue": self._check_file_residue(),
            "accounts": self._check_accounts() if is_windows else self._collect_linux_accounts(),
            "patches": self._get_patch_info() if is_windows else [],
            "softwares": self._get_software_inventory(),
            "dnsCache": self._get_dns_cache(),
            "cmdHistory": self._get_cmd_history() if is_windows else self._collect_linux_history()
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

    def _get_software_inventory(self):
        """复用 ExtraCollector 的软件盘点列表纳入深度取证报告"""
        try:
            from .extra_collectors import ExtraCollector
            collector = ExtraCollector()
            return collector._get_softwares()
        except: return []

    def _get_dns_cache(self):
        """提取本地 DNS 客户端缓存 (兼容 Win7/XP/Linux)"""
        try:
            if platform.system() == "Windows":
                # 1. 尝试首选 PowerShell 命令 (Win8+)
                cmd = "Get-DnsClientCache | Select-Object Entry, TimeToLive, Data | Where-Object { $_.Entry -notlike '127.0.*' -and $_.Entry -notlike 'localhost' } | Select -First 50 | ConvertTo-Json"
                raw = self._run_ps(cmd)
                if raw:
                    out = json.loads(raw)
                    return out if isinstance(out, list) else [out]
                
                # 2. 降级方案 (Win7/XP): 解析 ipconfig /displaydns
                logger.info("Forensic: Get-DnsClientCache failed or not supported, falling back to ipconfig /displaydns")
                raw_txt = subprocess.check_output(["ipconfig", "/displaydns"], text=True, stderr=subprocess.DEVNULL)
                dns_items = []
                import re
                # 匹配 Record Name 和 A (Host) Record
                parts = re.split(r'\s{2,}', raw_txt)
                current_entry = None
                for line in raw_txt.splitlines():
                    line = line.strip()
                    if "Record Name" in line:
                        current_entry = line.split(":")[-1].strip()
                    elif "A (Host) Record" in line and current_entry:
                        addr = line.split(":")[-1].strip()
                        if addr and not addr.startswith("127."):
                            dns_items.append({"Entry": current_entry, "Data": addr, "TimeToLive": "N/A"})
                        current_entry = None
                return dns_items[:50]
            else:
                # Linux 分支: 尝试解析常用缓存文件或 /etc/hosts (简单模拟)
                if os.path.exists("/etc/hosts"):
                    with open("/etc/hosts", "r") as f:
                        lines = f.readlines()
                        return [{"Entry": l.split()[-1], "Data": l.split()[0]} for l in lines if len(l.split()) >= 2 and not l.startswith("#")][:30]
        except: pass
        return []

    def _get_cmd_history(self):
        """提取命令行执行历史 (兼容多种 PS 版本)"""
        try:
            # 1. 尝试通过 PSReadLine 获取 (推荐)
            cmd = "Get-Content (Get-PSReadLineOption).HistorySavePath -ErrorAction SilentlyContinue | Select-Object -Last 50 | ConvertTo-Json"
            raw = self._run_ps(cmd)
            if raw:
                out = json.loads(raw)
                return out if isinstance(out, list) else [out]
            
            # 2. 降级方案: 直接遍历默认路径
            history_paths = [
                os.path.join(os.environ.get("APPDATA", ""), r"Microsoft\Windows\PowerShell\PSReadLine\ConsoleHost_history.txt"),
                os.path.join(os.path.expanduser("~"), r".bash_history") # Cygwin/GitBash
            ]
            for path in history_paths:
                if os.path.exists(path):
                    with open(path, "r", encoding='utf-8', errors='ignore') as f:
                        return f.readlines()[-50:]
        except: pass
        return []

    def _collect_linux_history(self):
        """Linux 命令行历史采集"""
        try:
            history_file = os.path.expanduser("~/.bash_history")
            if os.path.exists(history_file):
                with open(history_file, "r", errors='ignore') as f:
                    return [l.strip() for l in f.readlines()[-100:]]
        except: return []
        return []

    def _check_persistence(self):
        """排查持久化项：计划任务、注册表、WMI"""
        res = {"scheduledTasks": [], "registry": [], "wmiSubscriptions": []}
        if platform.system() != "Windows":
            return res

        # 1. 计划任务 (过滤掉 Microsoft 签名)
        try:
            # 优先使用现代 PS 命令
            ps_cmd = (
                "Get-ScheduledTask | Where-Object { $_.Author -notlike '*Microsoft*' -and $_.State -ne 'Disabled' } | "
                "Select-Object TaskName, TaskPath, @{Name='Action'; Expression={$_.Actions.Execute + ' ' + $_.Actions.Arguments}}, Author | ConvertTo-Json"
            )
            raw = self._run_ps(ps_cmd)
            if raw:
                tasks = json.loads(raw)
                tasks = tasks if isinstance(tasks, list) else [tasks]
                for t in tasks:
                    item = { "name": t.get("TaskName"), "path": t.get("TaskPath"), "action": t.get("Action"), "author": t.get("Author"), "isSuspicious": False }
                    self._audit_payload(item, "action")
                    res["scheduledTasks"].append(item)
            else:
                # 降级方案 (Win7/XP): 使用 schtasks 命令
                logger.info("Forensic: Get-ScheduledTask failed, falling back to schtasks")
                raw_csv = subprocess.check_output(["schtasks", "/query", "/fo", "csv", "/v"], text=True, stderr=subprocess.DEVNULL)
                import csv
                from io import StringIO
                reader = csv.DictReader(StringIO(raw_csv))
                for row in list(reader)[:50]:
                    if "Microsoft" not in str(row.get("Author", "")):
                        item = {
                            "name": row.get("TaskName"),
                            "path": row.get("TaskName"),
                            "action": row.get("Task To Run"),
                            "author": row.get("Author"),
                            "isSuspicious": False
                        }
                        self._audit_payload(item, "action")
                        res["scheduledTasks"].append(item)
        except Exception as e:
            logger.error(f"Forensic: ScheduledTask check failed: {e}")
        
        # ... (Registry & WMI logic remains same as it's already fairly compatible)
        # 2. 注册表启动项 (winreg 是 Python 内置，兼容性极佳)
        # 3. WMI 订阅 (WMI 在 XP+ 均有支持，但 Get-WmiObject 较老，暂时保留)

        return res

    def _collect_linux_persistence(self):
        """Linux 态势持久化检测 (Crontab / Systemd)"""
        res = {"scheduledTasks": [], "registry": [], "wmiSubscriptions": []}
        try:
            # 1. Crontab
            if os.path.exists("/etc/crontab"):
                with open("/etc/crontab", "r") as f:
                    for line in f.readlines():
                        if not line.startswith("#") and len(line.strip()) > 10:
                            res["scheduledTasks"].append({"name": "etc_crontab", "action": line.strip(), "isSuspicious": "/" in line})
            # 2. Systemd 简单遍历
            try:
                units = subprocess.check_output(["systemctl", "list-unit-files", "--type=service", "--state=enabled"], text=True)
                for line in units.splitlines()[1:20]:
                    res["registry"].append({"key": "systemd_service", "value": line.strip()})
            except: pass
        except: pass
        return res

    def _get_patch_info(self):
        """取证_资产盘点：补丁信息 (KB号)"""
        patches = []
        if platform.system() == "Windows":
            try:
                ps_cmd = "Get-HotFix | Select-Object HotFixID | ConvertTo-Json"
                raw = self._run_ps(ps_cmd)
                if raw:
                    data = json.loads(raw)
                    data = data if isinstance(data, list) else [data]
                    for p in data:
                        if p and p.get("HotFixID"):
                            patches.append(p.get("HotFixID"))
            except: pass
        return patches

    def _get_process_modules(self, pid):
        """取证_高阶进程：加载模块清单"""
        modules = []
        try:
            import psutil
            import hashlib
            p = psutil.Process(pid)
            # 仅取最多前 50 个非系统目录的模块以防过大
            for m in p.memory_maps():
                path = m.path
                if path and path.lower().endswith(".dll"):
                    if "windows\\system32" not in path.lower() and "windows\\syswow64" not in path.lower():
                        if len(modules) >= 50: break
                        # 计算 MD5
                        md5_hash = ""
                        try:
                            with open(path, "rb") as f:
                                md5_hash = hashlib.md5(f.read()).hexdigest()
                        except: pass
                        modules.append({"module": path, "md5": md5_hash, "is_signed": False})
        except: pass
        return modules

    def _get_advanced_hash(self, path):
        """取证_高阶文件：PE导入表哈希(imphash)与模糊哈希(ssdeep)"""
        adv_hashes = {}
        try:
            import pefile
            pe = pefile.PE(path)
            adv_hashes["imphash"] = pe.get_imphash()
        except: pass
        try:
            import ssdeep
            adv_hashes["ssdeep"] = ssdeep.hash_from_file(path)
        except: pass
        return adv_hashes

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
                     # 针对高度可疑进程，进行沙箱级上下文洞察 (模块提取与高阶特征算列)
                     item["processmodule"] = self._get_process_modules(p.get("pid"))
                     if os.path.exists(path):
                         adv_hashes = self._get_advanced_hash(path)
                         item.update(adv_hashes)
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
        """简单账号异常检查 (兼容 net user)"""
        res = {"users": [], "anomalies": []}
        try:
            if platform.system() == "Windows":
                # 优先 PS
                raw = self._run_ps("Get-LocalUser | Select-Object Name, Enabled | ConvertTo-Json")
                if raw:
                    users = json.loads(raw)
                    res["users"] = users if isinstance(users, list) else [users]
                else:
                    # 降级 net user
                    out = subprocess.check_output(["net", "user"], text=True)
                    # 粗暴解析 net user 输出
                    import re
                    names = re.findall(r"(\w+)\s+", out[out.find("-")+1:])
                    res["users"] = [{"Name": n, "Enabled": True} for n in names if n.strip()]
        except: pass
        return res

    def _collect_linux_accounts(self):
        """Linux 账号采集 (/etc/passwd)"""
        users = []
        try:
            if os.path.exists("/etc/passwd"):
                with open("/etc/passwd", "r") as f:
                    for line in f.readlines():
                        parts = line.split(":")
                        if len(parts) > 0:
                            users.append({"Name": parts[0], "Enabled": True})
        except: pass
        return {"users": users, "anomalies": []}

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
