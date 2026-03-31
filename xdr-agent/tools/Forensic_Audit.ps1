<#
.SYNOPSIS
    针对发现的恶意计划任务进行深度取证排查 (只读模式)
.DESCRIPTION
    本脚本用于在发现可疑计划任务（如 curl/powershell/msiexec 下载执行）后，
    进行全面的只读式安全排查。脚本不会删除、停止或修改任何系统内容。
    所有结果将输出到屏幕并记录到日志文件中。
.NOTES
    Author: Security Expert
    Version: 1.0
#>

# 检查是否以管理员身份运行
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "[!] 警告: 此脚本需要以管理员身份运行才能获取完整信息。" -ForegroundColor Red
    Start-Sleep -Seconds 3
}

# 初始化日志
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFileName = "Forensic_Audit_Report_$Timestamp.log"
$LogPath = Join-Path $PWD $LogFileName

$Header = @"
=================================================================
         恶意计划任务深度取证排查报告 (只读模式)
         报告生成时间: $(Get-Date)
         主机名: $env:COMPUTERNAME
         用户: $env:USERNAME
=================================================================

"@
Add-Content -Path $LogPath -Value $Header
Write-Host $Header -ForegroundColor Cyan

function Write-Section {
    param([string]$Title)
    $Separator = "-" * 80
    $SectionHeader = "`n$Separator`n[+] $Title`n$Separator"
    Add-Content -Path $LogPath -Value $SectionHeader
    Write-Host $SectionHeader -ForegroundColor Yellow
}

function Log-Info {
    param([string]$Message)
    Add-Content -Path $LogPath -Value $Message
    Write-Host $Message
}

# ==================== 第一部分：提取并分析可疑计划任务 ====================
Write-Section "1. 可疑计划任务详情提取"

# 定义常见的可疑关键词
$SuspiciousKeywords = @("curl", "wget", "powershell", "msiexec", "certutil", "bitsadmin", "regsvr3", "rundll32", "cmd.exe", ".jpg", ".png", ".txt")

# 获取所有计划任务
try {
    $AllTasks = Get-ScheduledTask -ErrorAction SilentlyContinue
    $SuspiciousTasks = @()
    foreach ($Task in $AllTasks) {
        # 检查任务的操作（Action）
        if ($Task.Actions.Execute -or $Task.Actions.Arguments) {
            $CommandString = "$($Task.Actions.Execute) $($Task.Actions.Arguments)"
            foreach ($Keyword in $SuspiciousKeywords) {
                if ($CommandString -like "*$Keyword*") {
                    $SuspiciousTasks += $Task
                    break
                }
            }
        }
    }

    if ($SuspiciousTasks.Count -eq 0) {
        $Msg = "未发现包含可疑关键词的计划任务。"
        Log-Info $Msg
    } else {
        $Msg = "发现 $($SuspiciousTasks.Count) 个可疑计划任务:"
        Log-Info $Msg
        foreach ($Task in $SuspiciousTasks) {
            $TaskInfo = @"
任务名称: $($Task.TaskName)
任务路径: $($Task.TaskPath)
创建者:   $($Task.Author)
状态:     $($Task.State)
执行程序: $($Task.Actions.Execute)
参数:     $($Task.Actions.Arguments)
最后运行时间: $($Task.LastRunTime)
下次运行时间: $($Task.NextRunTime)

"@
            Log-Info $TaskInfo
            
            # 尝试从参数中提取URL
            if ($Task.Actions.Arguments) {
                $Urls = [regex]::Matches($Task.Actions.Arguments, 'https?://[^\s]+').Value
                if ($Urls) {
                    Log-Info ">>> 提取到的潜在恶意URL:"
                    foreach ($Url in $Urls) {
                        Log-Info "    - $Url"
                    }
                }
            }
        }
    }
} catch {
    Log-Info "[错误] 获取计划任务时出错: $($_.Exception.Message)"
}

# ==================== 第二部分：检查当前运行的可疑进程与网络连接 ====================
Write-Section "2. 当前可疑进程与网络连接检查"

# 检查可疑进程
$SuspiciousProcesses = Get-Process -Name "powershell", "cmd", "msiexec", "wmic", "certutil", "bitsadmin" -ErrorAction SilentlyContinue
if ($SuspiciousProcesses) {
    Log-Info "发现以下可疑进程正在运行:"
    $SuspiciousProcesses | ForEach-Object {
        $ProcInfo = "PID: $($_.Id), 进程名: $($_.ProcessName), 启动时间: $($_.StartTime)"
        Log-Info "  - $ProcInfo"
        
        # 尝试获取命令行 (需要 SeDebugPrivilege, 管理员通常有)
        try {
            $CommandLine = (Get-WmiObject Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine
            if ($CommandLine) {
                Log-Info "    命令行: $CommandLine"
            }
        } catch {
            # 忽略错误
        }
    }
} else {
    Log-Info "未发现常见的可疑进程 (powershell, cmd, msiexec等) 在运行。"
}

# 检查网络连接
Log-Info "`n检查所有ESTABLISHED网络连接..."
$NetstatOutput = netstat -ano | Select-String "ESTABLISHED"
if ($NetstatOutput) {
    Log-Info "活动的网络连接列表 (PID -> 远程地址):"
    $NetstatOutput | ForEach-Object {
        $line = $_.ToString().Trim()
        # 解析 netstat 输出
        $parts = $line -split '\s+'
        if ($parts.Count -ge 5) {
            $localAddr = $parts[1]
            $remoteAddr = $parts[2]
            $pid = $parts[4]
            Log-Info "  PID: $pid -> $remoteAddr"
        }
    }
} else {
    Log-Info "当前没有活动的ESTABLISHED网络连接。"
}

# ==================== 第三部分：检查其他持久化后门 ====================
Write-Section "3. 其他高危持久化位置快速扫描"

# 检查 WMI 事件订阅
Log-Info "检查 WMI 事件订阅 (高隐蔽性后门):"
$WMIConsumers = Get-WmiObject -Namespace root\subscription -Class __EventConsumer -ErrorAction SilentlyContinue
$WMIFilters = Get-WmiObject -Namespace root\subscription -Class __EventFilter -ErrorAction SilentlyContinue

if ($WMIConsumers -or $WMIFilters) {
    Log-Info "发现 WMI 订阅项，详情已记录。请人工重点审查！"
    Add-Content -Path $LogPath -Value "`nWMI Consumers:`n$($WMIConsumers | Format-List | Out-String)"
    Add-Content -Path $LogPath -Value "`nWMI Filters:`n$($WMIFilters | Format-List | Out-String)"
} else {
    Log-Info "未发现 WMI 事件订阅。"
}

# 检查可疑服务
Log-Info "`n检查非标准路径的服务:"
$Services = Get-WmiObject Win32_Service -ErrorAction SilentlyContinue
$SuspiciousServices = $Services | Where-Object {
    $_.PathName -like "*Temp*" -or 
    $_.PathName -like "*AppData*" -or
    $_.PathName -like "*\.exe*" -and $_.PathName -notlike "*Windows*"
}
if ($SuspiciousServices) {
    Log-Info "发现以下路径可疑的服务:"
    $SuspiciousServices | ForEach-Object {
        Log-Info "  - 服务名: $($_.Name), 路径: $($_.PathName)"
    }
} else {
    Log-Info "未发现明显路径异常的服务。"
}

# ==================== 第四部分：日志溯源 ====================
Write-Section "4. 关键安全日志片段 (最近50条相关事件)"

# 查询计划任务创建/更新日志
Log-Info "最近的计划任务注册/更新事件 (Event ID 106, 140, 141):"
$TaskLogs = Get-WinEvent -FilterHashtable @{LogName='Microsoft-Windows-TaskScheduler/Operational'; Id=106,140,141} -MaxEvents 20 -ErrorAction SilentlyContinue
if ($TaskLogs) {
    $TaskLogs | ForEach-Object {
        $LogEntry = "[$($_.TimeCreated)] $($_.Message)"
        Log-Info $LogEntry
    }
} else {
    Log-Info "未找到近期的计划任务操作日志。"
}

# 查询可疑进程创建日志
Log-Info "`n最近的可疑进程创建事件 (Event ID 4688, 包含 powershell/cmd):"
$ProcessLogs = Get-WinEvent -FilterHashtable @{LogName='Security'; Id=4688} -MaxEvents 50 -ErrorAction SilentlyContinue |
    Where-Object { $_.Message -like "*powershell*" -or $_.Message -like "*cmd.exe*" -or $_.Message -like "*msiexec*" }
if ($ProcessLogs) {
    $ProcessLogs | ForEach-Object {
        $LogEntry = "[$($_.TimeCreated)] $($_.Message)"
        Log-Info $LogEntry
    }
} else {
    Log-Info "未找到近期的可疑进程创建日志。"
}

# ==================== 报告结束 ====================
$Footer = @"
=================================================================
取证排查完成。
日志文件: $LogPath
请注意：
1.  本脚本仅用于信息收集，不会对系统进行任何修改。
2.  请仔细审查日志中的 '可疑' 条目，特别是 WMI 订阅和提取出的 URL。
3.  强烈建议将提取到的 URL 在微步在线 (ThreatBook) 或 VirusTotal 上进行威胁情报查询。
=================================================================
"@
Add-Content -Path $LogPath -Value $Footer
Write-Host $Footer -ForegroundColor Green