# XDR-Agent 采集数据结构字典 (竞品 CSV 模板版)

本数据字典在保持与友商一致的字段标准（包含 5 列标准元数据）基础上，在架构维度严格拆分为 **[A. 实时轻量采集队列]** 与 **[B. 主动深度取证队列]**。这使得该规范完美兼顾了高维度排查与极低 CPU 占用的要求。

---

## [A. 实时轻量采集队列] (CPU≤5%)
*触发条件*: 增量行为触发，或超低频定时上报。

| 序号 | 数据类别 | 采集项 | 字段名称 | 字段示例 |
|:---|:---|:---|:---|:---|
| 1 | 系统信息 | 探针唯一标识 | uuid | 157abda2a91a2a21a76d73b2865767d2a185... |
| 2 | | 主机名 | machine | WIN-U1NDUN9Q0IL |
| 3 | | 操作系统版本 | osversion | 10.0.20348.502 |
| 4 | | 系统架构 | arch | x64 |
| 5 | | CPU利用率监控 | cpu_usage | 3.5 |
| 6 | | 内存利用率监控 | mem_usage | 45.2 |
| 7 | | 采集时间戳 | timestamp | 1649095190 |
| 8 | 进程信息 | 进程名称 | processname | sihost.exe |
| 9 | | PID | pid | 4884 |
| 10 | | 运行用户 | processuser | WIN-U1NDUN9O0IL\Administrator |
| 11 | | 进程路径 | processpath | C:\Windows\system32\sihost.exe |
| 12 | | 启动参数 | processcmdline | sihost.exe -param |
| 13 | | 进程 MD5 | processmd5 | aeb6b1d4eae4377e44d916f60269a401 |
| 14 | | 父进程名 | parentname | svchost.exe |
| 15 | | 父进程 ID | ppid | 1884 |
| 16 | | 可信厂商签名 | signature_vendor | Microsoft Corporation |
| 17 | | 采集时间戳 | timestamp | 1649249980 |
| 18 | 网卡通信信息 | 本地地址及端口 | srcipport | 192.168.1.5:3389 |
| 19 | | 远程地址及端口 | dstipport | 101.201.233.121:443 |
| 20 | | 协议 | protocol | TCP |
| 21 | | 状态 | status | ESTABLISHED |
| 22 | | 关联进程名 | processname | svchost.exe |
| 23 | | 关联进程ID | pid | 908 |
| 24 | | 采集时间戳 | timestamp | 1649095190 |
| 25 | 域名信息 | 查询域名 | querydnsname | malicious-c2.com |
| 26 | | 解析结果 | querydnsresult | ["101.201.233.121"] |
| 27 | File IO信息 | 文件路径 | filepath | C:\Users\Admin\Downloads |
| 28 | | 文件名 | filename | TCPPortForwarding.exe |
| 29 | | 操作行为 | action | CREATE |
| 30 | Shell信息 | 来源 | source | PowerShell (EventLog-4104) |
| 31 | | 事件ID | eventid | 4104 |
| 32 | | 任务类别 | type | 执行远程命令/脚本块执行 |
| 33 | | 执行明文脚本 | details | `Invoke-WebRequest -Uri http://c2/` |
| 34 | 注册表事件 | 操作行为 | registry_action | CREATE_KEY / SET_VALUE |
| 35 | | 目标键值路径 | target_key | `HKLM\Software\Microsoft\Windows\CurrentVersion\Run` |
| 36 | | 修改数据内容 | value_data | `C:\Users\Public\malware.exe` |

---

## [B. 主动深度取证队列] (On-Demand Forensics / 漏洞狩猎)
*触发条件*: 管理员通过控制台主动下发指令，或定时在凌晨超低峰期执行盘点任务。无 CPU 限制。

| 序号 | 数据类别 | 采集项 | 字段名称 | 字段示例 |
|:---|:---|:---|:---|:---|
| 34 | 取证_资产盘点 | 补丁信息 | patchinfo （数组） | `["KB5009470", "KB5009608", "KB5009639"]` |
| 35 | | 安装软件列表 | software （数组） | `[{"name": "Microsoft Edge", "version": "99.0.1", "vendor": "Microsoft...", "installpath": "C:\\Program Files..."}]` |
| 36 | | 本地用户清单 | local_users （数组） | `[{"username": "Administrator", "role": 1, "status": "active"}]` |
| 37 | 取证_高阶进程 | 加载模块清单 | processmodule（数组）| `[{"module":"C:\\Windows\\System32\\ntdll.dll","md5":"f268ecef...","is_signed": true}]` |
| 38 | 取证_高阶文件 | 检出目标文件路径 | target_filepath | C:\Temp\ransomware.exe |
| 39 | | 文件模糊哈希 | ssdeep | `12288:rna9+5e7FSbe7Yh4A0IGF4O2DnM2xk...` |
| 40 | | PE导入表哈希 | imphash | `884310b1928934402ea6fec1dbd3cf5e` |
| 41 | | 局部敏感哈希 | tlsh | `T1DEC41312EBD79436D0A199B4EF26...` |
| 45 | 取证_隐蔽后门 | 计划任务清单 | scheduled_tasks（数组） | `[{"task_name": "Update", "registry_path": "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Schedule\\TaskCache\\Tasks", "trigger": "OnLogon"}]` |
| 46 | | 隐蔽自启动项 | autorun_registry（数组） | `[{"key_path": "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "value_name": "WinUpdate", "value_data": "C:\\Temp\\backdoor.exe"}]` |
| 47 | | 服务后门检查 | services_registry (数组) | `[{"key_path": "HKLM\\SYSTEM\\CurrentControlSet\\Services\\EventLog", "image_path": "C:\\malware.sys"}]` |
| 48 | | WMI后门探查 | wmi_subscriptions（数组）| `[{"consumer_name": "MaliciousConsumer", "filter_query": "SELECT * FROM __InstanceModificationEvent..."}]` |
| 49 | 取证_动态踪迹 | PowerShell执行历史 | cmdHistory（数组）| `["ping baidu.com", "nslookup malicious.com", "whoami /priv"]` |
| 50 | | 本地DNS缓存探录 | dnsCache (数组) | `[{"Entry": "malicious.com", "TimeToLive": 38, "Data": "1.1.1.1"}]` |

> **隐私说明**:
> 完全对齐竞品隐私标准。除白名单内可执行文件（exe、dll、sys、ps1、vbs 等二进制及脚本后缀）外，不拦截和上报业务产生的任何文档（doc、xls、pdf 等）内容及文件名；白名单数字签名进程不执行深度哈希，从而极大程度保护系统隐私同时提升运行效能。
