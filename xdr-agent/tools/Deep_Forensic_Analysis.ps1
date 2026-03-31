<#
.SYNOPSIS
    深度无文件攻击取证分析脚本 (只读模式)
.DESCRIPTION
    针对 curl/powershell/msiexec 下载执行类攻击的深度排查。
    功能包括：
    1. 计划任务深度扫描与 Base64 自动解码
    2. 临时目录与可疑文件残留扫描
    3. 注册表持久化项检查
    4. WMI 事件订阅检查 (高隐蔽后门)
    5. 活跃网络连接与可疑进程分析
    6. 安全日志溯源 (任务创建/进程启动)
    
    ⚠️ 警告：本脚本仅用于信息收集，不会执行任何删除、停止或修改操作。
.NOTES
    Version: 2.0-Full
    Author: Security Expert System
#>

# ================= 配置区域 =================
$OutputDir = Get-Location
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFileName = "Forensic_Report_$Timestamp.log"
$LogPath = Join-Path $OutputDir $LogFileName

# 可疑关键词列表
$SuspiciousKeywords = @("curl", "wget", "powershell", "msiexec", "certutil", "bitsadmin", "regsvr32", "rundll32", "wscript", "cscript", ".jpg", ".png", ".txt", ".zip", "-enc", "-e ", "downloadstring", "iex", "invoke-expression", "hidden")

# ================= 初始化 =================
function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Add-Content -Path $LogPath -Value $Message
    if ($Color -eq "White") {
        Write-Host $Message
    } else {
        Write-Host $Message -ForegroundColor $Color
    }
}

function Write-Section {
    param([string]$Title)
    $Separator = "=" * 80
    $Header = "`n$Separator`n[+] $Title`n$Separator"
    Write-Log $Header "Cyan"
}

function Write-AnalysisTip {
    param([string]$Tip)
    $TipMsg = "🔍 分析建议: $Tip"
    Write-Log $TipMsg "Yellow"
}

# 检查管理员权限
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Log "[!] 错误：请以管理员身份运行此脚本！" "Red"
    Start-Sleep -Seconds 3
    exit
}

# 写入报告头
$ReportHeader = @"
=================================================================
         深度无文件攻击取证分析报告 (只读模式)
         生成时间: $(Get-Date)
         主机名: $env:COMPUTERNAME
         当前用户: $env:USERNAME
         日志路径: $LogPath
=================================================================
"@
Write-Log $ReportHeader "Cyan"

# ================= 模块 1: 计划任务深度解析 =================
Write-Section "模块 1: 计划任务深度解析与载荷提取"

try {
    $AllTasks = Get-ScheduledTask -ErrorAction SilentlyContinue
    $FoundSuspicious = $false
    
    foreach ($Task in $AllTasks) {
        $ActionExec = $Task.Actions.Execute
        $ActionArgs = $Task.Actions.Arguments
        $FullCommand = "$ActionExec $ActionArgs"
        
        # 匹配可疑关键词
        $IsSuspicious = $false
        foreach ($Key in $SuspiciousKeywords) {
            if ($FullCommand -like "*$Key*") {
                $IsSuspicious = $true
                break
            }
        }

        if ($IsSuspicious) {
            $FoundSuspicious = $true
            Write-Log "`n[!!!] 发现可疑任务: $($Task.TaskPath)$($Task.TaskName)" "Red"
            Write-Log "  状态: $($Task.State) | 作者: $($Task.Author)"
            Write-Log "  最后运行: $($Task.LastRunTime) | 下次运行: $($Task.NextRunTime)"
            Write-Log "  执行程序: $ActionExec"
            Write-Log "  原始参数: $ActionArgs"
            
            # 提取 URL
            $Urls = [regex]::Matches($FullCommand, 'https?://[^\s"''<>]+').Value
            if ($Urls) {
                Write-Log "  [提取到的 URL]:" "Green"
                foreach ($Url in $Urls) { Write-Log "    - $Url" "Green" }
                Write-AnalysisTip "即使URL无法访问，也请记录并在微步在线/VirusTotal查询域名信誉。"
            }

            # Base64 解码尝试
            if ($ActionArgs -match "-e(?:ncodedcommand)?\s+([A-Za-z0-9+/=]{20,})") {
                $B64String = $matches[1]
                Write-Log "  [检测到 Base64 编码命令]" "Magenta"
                try {
                    # 处理可能的 Unicode 编码
                    $Bytes = [System.Convert]::FromBase64String($B64String)
                    $Decoded = [System.Text.Encoding]::Unicode.GetString($Bytes)
                    Write-Log "  [解码结果]: $Decoded" "Green"
                    
                    # 递归检查解码后是否有更多 URL
                    $SubUrls = [regex]::Matches($Decoded, 'https?://[^\s"''<>]+').Value
                    if ($SubUrls) {
                        Write-Log "  [解码内容中包含 URL]:" "Green"
                        foreach ($Url in $SubUrls) { Write-Log "    - $Url" "Green" }
                    }
                } catch {
                    Write-Log "  [解码失败]: 可能是非标准编码或已压缩。" "Gray"
                }
                Write-AnalysisTip "仔细检查解码后的命令，寻找 DownloadFile, SaveToFile, Invoke-Expression 等关键字。"
            }
            
            # 检查是否包含隐藏窗口参数
            if ($ActionArgs -like "*-WindowStyle Hidden*" -or $ActionArgs -like "*-Hidden*") {
                Write-Log "  [警告]: 检测到隐藏窗口执行参数!" "Red"
            }
        }
    }
    
    if (-not $FoundSuspicious) {
        Write-Log "未发现包含常见可疑关键词的计划任务。" "Green"
    }
} catch {
    Write-Log "[错误] 扫描计划任务失败: $($_.Exception.Message)" "Red"
}

# ================= 模块 2: 文件残留扫描 =================
Write-Section "模块 2: 临时目录与可疑文件残留扫描"

$ScanPaths = @(
    "C:\Windows\Temp",
    "C:\Users\*\AppData\Local\Temp",
    "C:\ProgramData",
    "C:\Users\Public"
)

Write-Log "正在扫描最近 48 小时内创建的可疑文件..."
$RecentFiles = @()
foreach ($Path in $ScanPaths) {
    try {
        $Files = Get-ChildItem -Path $Path -Recurse -ErrorAction SilentlyContinue | 
                 Where-Object { 
                     $_.LastWriteTime -gt (Get-Date).AddHours(-48) -and 
                     -not $_.PSIsContainer 
                 }
        $RecentFiles += $Files
    } catch {}
}

if ($RecentFiles.Count -gt 0) {
    # 过滤出高风险扩展名
    $HighRiskExtensions = @(".exe", ".dll", ".vbs", ".js", ".ps1", ".bat", ".cmd", ".msi", ".jpg", ".png", ".txt")
    $SuspiciousFiles = $RecentFiles | Where-Object { $HighRiskExtensions -contains $_.Extension }
    
    if ($SuspiciousFiles.Count -gt 0) {
        Write-Log "发现 $($SuspiciousFiles.Count) 个近期创建的高风险文件:" "Yellow"
        $SuspiciousFiles | Sort-Object LastWriteTime -Descending | Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize | Out-String | Write-Log
        
        Write-AnalysisTip "重点检查 .jpg/.png (可能是伪装的脚本) 和不明 .exe/.dll。对比文件大小和创建时间是否与攻击时间吻合。"
    } else {
        Write-Log "未在临时目录发现明显的高风险扩展名文件。" "Green"
    }
} else {
    Write-Log "近期临时目录无新增文件。" "Green"
}

# ================= 模块 3: 注册表持久化检查 =================
Write-Section "模块 3: 注册表持久化项检查"

$RegPaths = @(
    "HKLM:\Software\Microsoft\Windows\CurrentVersion\Run",
    "HKLM:\Software\Microsoft\Windows\CurrentVersion\RunOnce",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\RunOnce",
    "HKLM:\Software\Microsoft\Windows NT\CurrentVersion\Winlogon\Userinit",
    "HKLM:\Software\Microsoft\Windows NT\CurrentVersion\Winlogon\Shell"
)

foreach ($Path in $RegPaths) {
    if (Test-Path $Path) {
        $Items = Get-ItemProperty -Path $Path -ErrorAction SilentlyContinue
        $Props = $Items.PSObject.Properties | Where-Object { $_.Name -notmatch "^PS" }
        foreach ($Prop in $Props) {
            $Val = $Prop.Value
            # 检查值中是否包含可疑关键词
            foreach ($Key in $SuspiciousKeywords) {
                if ($Val -like "*$Key*") {
                    Write-Log "[警告] 发现可疑注册表启动项:" "Red"
                    Write-Log "  路径: $Path"
                    Write-Log "  键名: $($Prop.Name)"
                    Write-Log "  键值: $Val"
                    Write-AnalysisTip "该注册表项指向了可疑程序，需立即核实。"
                    break
                }
            }
        }
    }
}
Write-Log "注册表常规启动项扫描完成。" "Gray"

# ================= 模块 4: WMI 事件订阅检查 (高危) =================
Write-Section "模块 4: WMI 事件订阅检查 (高隐蔽后门)"

try {
    $Consumers = Get-WmiObject -Namespace root\subscription -Class __EventConsumer -ErrorAction SilentlyContinue
    $Filters = Get-WmiObject -Namespace root\subscription -Class __EventFilter -ErrorAction SilentlyContinue
    $Bindings = Get-WmiObject -Namespace root\subscription -Class __FilterToConsumerBinding -ErrorAction SilentlyContinue

    if ($Consumers -or $Filters) {
        Write-Log "[警告] 发现 WMI 事件订阅 (通常用于无文件后门):" "Red"
        
        if ($Consumers) {
            Write-Log "`n--- Event Consumers (执行动作) ---"
            foreach ($C in $Consumers) {
                Write-Log "名称: $($C.Name)"
                Write-Log "类型: $($C.__CLASS)"
                if ($C.CommandLineExecutable) { Write-Log "执行命令: $($C.CommandLineExecutable)" }
                if ($C.ScriptText) { Write-Log "脚本内容: $($C.ScriptText)" }
            }
        }
        
        if ($Filters) {
            Write-Log "`n--- Event Filters (触发条件) ---"
            foreach ($F in $Filters) {
                Write-Log "名称: $($F.Name)"
                Write-Log "查询语句: $($F.EventNamespace) - $($F.Query)"
            }
        }
        
        Write-AnalysisTip "正常的 WMI 订阅极少见。如果发现指向 powershell/cmd 的订阅，极大概率为后门。记录名称以便后续清除。"
    } else {
        Write-Log "未发现 WMI 事件订阅。" "Green"
    }
} catch {
    Write-Log "WMI 检查出错: $($_.Exception.Message)" "Red"
}

# ================= 模块 5: 进程与网络分析 =================
Write-Section "模块 5: 活跃进程与网络连接分析"

# 检查可疑进程
Write-Log "检查活跃的可疑进程 (PowerShell, Cmd, Msiexec 等)..."
$SusProcs = Get-Process -Name "powershell", "cmd", "msiexec", "wmic", "certutil", "bitsadmin", "rundll32" -ErrorAction SilentlyContinue
foreach ($Proc in $SusProcs) {
    $CmdLine = ""
    try {
        $CmdLine = (Get-WmiObject Win32_Process -Filter "ProcessId=$($Proc.Id)").CommandLine
    } catch {}
    
    if ($CmdLine) {
        $IsBad = $false
        foreach ($Key in $SuspiciousKeywords) {
            if ($CmdLine -like "*$Key*") { $IsBad = $true; break }
        }
        
        if ($IsBad) {
            Write-Log "[警告] 发现可疑进程命令行:" "Red"
            Write-Log "  PID: $($Proc.Id) | 名称: $($Proc.ProcessName)"
            Write-Log "  启动时间: $($Proc.StartTime)"
            Write-Log "  命令行: $CmdLine"
        }
    }
}

# 检查网络连接
Write-Log "`n检查 ESTABLISHED 状态的对外连接..."
$NetStats = netstat -ano | Select-String "ESTABLISHED"
$ExternalConnections = @()
foreach ($Line in $NetStats) {
    $Parts = $Line.ToString().Trim() -split '\s+'
    if ($Parts.Count -ge 5) {
        $RemoteIP = $Parts[2].Split(':')[0]
        $PID = $Parts[4]
        # 排除内网
        if ($RemoteIP -notmatch "^127." -and $RemoteIP -notmatch "^192.168." -and $RemoteIP -notmatch "^10." -and $RemoteIP -notmatch "^172.(1[6-9]|2[0-9]|3[01]).") {
            $ExternalConnections += [PSCustomObject]@{
                PID = $PID
                RemoteIP = $RemoteIP
                FullAddr = $Parts[2]
            }
        }
    }
}

if ($ExternalConnections.Count -gt 0) {
    Write-Log "发现 $($ExternalConnections.Count) 个对外连接:" "Yellow"
    $ExternalConnections | Format-Table -AutoSize | Out-String | Write-Log
    Write-AnalysisTip "核对上述 IP 是否为业务所需。未知 IP 请立即在威胁情报平台查询，并关联 PID 查找进程。"
} else {
    Write-Log "未发现异常的对外 ESTABLISHED 连接。" "Green"
}

# ================= 模块 6: 日志溯源 =================
Write-Section "模块 6: 关键安全日志溯源 (最近 100 条)"

Write-Log "提取计划任务操作日志 (ID 106-创建, 140-更新, 141-删除)..."
try {
    $TaskLogs = Get-WinEvent -FilterHashtable @{LogName='Microsoft-Windows-TaskScheduler/Operational'; Id=106,140,141} -MaxEvents 50 -ErrorAction SilentlyContinue
    if ($TaskLogs) {
        $TaskLogs | ForEach-Object {
            Write-Log "[$($_.TimeCreated)] ID:$($_.Id) - $($_.Message)" -Color Gray
        }
    } else {
        Write-Log "无相关计划任务日志。"
    }
} catch { Write-Log "无法读取任务日志。" }

Write-Log "`n提取可疑进程创建日志 (Security ID 4688)..."
try {
    $ProcLogs = Get-WinEvent -FilterHashtable @{LogName='Security'; Id=4688} -MaxEvents 100 -ErrorAction SilentlyContinue |
                Where-Object { $_.Message -match "powershell|cmd.exe|msiexec|certutil" }
    if ($ProcLogs) {
        $ProcLogs | ForEach-Object {
            # 简单提取关键信息
            $Time = $_.TimeCreated
            $User = if ($_.Message -match "Account Name:\s+(\S+)") { $matches[1] } else { "Unknown" }
            Write-Log "[$Time] 用户: $User - 进程启动 (详情见原始日志)" -Color Gray
        }
        Write-AnalysisTip "详细日志请查看 Windows 事件查看器 -> 安全日志 -> 筛选 ID 4688。"
    } else {
        Write-Log "近期无可疑进程创建日志。"
    }
} catch { Write-Log "无法读取安全日志。" }

# ================= 结束 =================
$Footer = @"
=================================================================
分析完成。
日志已保存至: $LogPath

【后续行动指南】
1. 针对发现的 URL，即使无法访问，也请在微步在线 (ThreatBook) 查询域名信誉。
2. 针对解码出的 PowerShell 命令，搜索其中的文件路径，手动检查是否存在残留。
3. 如果发现 WMI 订阅或可疑注册表项，请记录名称，使用专用工具或手动命令清除。
4. 若无法确认系统完全干净，建议修改所有管理员密码，并考虑重装系统。
=================================================================
"@
Write-Log $Footer "Cyan"
Write-Host "`n✅ 取证分析完成！请查看当前目录下的日志文件：$LogFileName" -ForegroundColor Green