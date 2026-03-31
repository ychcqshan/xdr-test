<#
.SYNOPSIS
    长周期定向攻击痕迹采集脚本 (2024-至今)
.DESCRIPTION
    本脚本仅执行信息采集和历史数据分析，绝不执行任何删除、停止、修改或清除操作。
    也不包含任何处置建议。
    
    目标：
    1. 检索 2024-01-01 至今所有与 target domains 相关的痕迹。
    2. 构建完整的时间线 (DNS, Tasks, Processes, Network, Logs)。
    3. 还原攻击者活动轨迹。
    
    ⚠️ 模式：READ-ONLY (只读)
    ⚠️ 范围：Past 2 Years
    #>

# ================= 配置区域 =================
$TargetDomains = @("newfloralboutique.com", "shioyuilubiz.com")
$StartDate = (Get-Date).AddYears(-2) # 向前追溯 2 年，覆盖 2024
$OutputDir = Get-Location
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFileName = "Long_Term_Analysis_Report_$Timestamp.log"
$LogPath = Join-Path $OutputDir $LogFileName

# 构建搜索正则
$SearchPattern = ($TargetDomains | ForEach-Object { [regex]::Escape($_) }) -join "|"

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

function Write-DataBlock {
    param([string]$Title, [string]$Content)
    $Box = "`n--- $Title ---`n$Content`n----------------`n"
    Write-Log $Box "Yellow"
}

# 检查管理员权限
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Log "[!] 警告：请以管理员身份运行以读取历史安全日志！" "Red"
    Write-Log "[!] 当前权限可能无法获取 2024 年的完整日志记录。" "Red"
}

# 报告头
$Header = @"
=================================================================
         长周期定向攻击痕迹采集报告 (只读模式)
         目标域名: $($TargetDomains -join ", ")
         排查时间范围: $StartDate 至 $(Get-Date)
         采集时间: $(Get-Date)
         主机名: $env:COMPUTERNAME
         操作员: $env:USERNAME
=================================================================
⚠️ 声明：本脚本未执行任何删除、停止、修改或清除操作。
⚠️ 声明：本报告不包含任何处置建议或修复步骤。
=================================================================
"@
Write-Log $Header "Cyan"

# ================= 模块 1: 计划任务历史关联 =================
Write-Section "模块 1: 计划任务中的域名关联 (全量扫描)"

$FoundTasks = @()
try {
    $AllTasks = Get-ScheduledTask -ErrorAction SilentlyContinue
    foreach ($Task in $AllTasks) {
        $Cmd = "$($Task.Actions.Execute) $($Task.Actions.Arguments)"
        # 即使任务当前不存在，如果 XML 定义中包含域名也能被检测到（如果是活跃任务）
        # 注意：Get-ScheduledTask 只能看到当前存在的任务。
        # 如果任务已被攻击者删除，此模块将无法看到，需依赖下方的事件日志模块。
        if ($Cmd -match $SearchPattern) {
            $FoundTasks += $Task
            
            Write-DataBlock "发现现存可疑任务: $($Task.TaskPath)$($Task.TaskName)" @"
任务名称: $($Task.TaskName)
路径: $($Task.TaskPath)
作者: $($Task.Author)
状态: $($Task.State)
创建时间 (任务属性): $($Task.Date)
最后运行时间: $($Task.LastRunTime)
下次运行时间: $($Task.NextRunTime)
原始命令: $Cmd
"@
            
            # Base64 解码
            if ($Cmd -match "-e(?:ncodedcommand)?\s+([A-Za-z0-9+/=]{20,})") {
                $B64 = $matches[1]
                try {
                    $Decoded = [System.Text.Encoding]::Unicode.GetString([System.Convert]::FromBase64String($B64))
                    Write-DataBlock "Base64 解码内容" $Decoded
                } catch {
                    Write-Log "  [i] Base64 解码失败。" "Gray"
                }
            }
            
            $Urls = [regex]::Matches($Cmd, 'https?://[^\s"''<>]+').Value
            if ($Urls) {
                Write-DataBlock "提取到的完整 URL" ($Urls -join "`n")
            }
        }
    }
    
    if ($FoundTasks.Count -eq 0) {
        Write-Log "未在【当前存活】的计划任务中找到直接包含目标域名的记录。" "Yellow"
        Write-Log "  [注] 攻击者可能已删除任务。请查阅下方 '模块 5: 事件日志溯源' 以查找历史任务创建记录。" "Gray"
    }
} catch {
    Write-Log "[错误] 读取计划任务失败: $($_.Exception.Message)" "Red"
}

# ================= 模块 2: DNS 解析历史 (缓存与日志) =================
Write-Section "模块 2: DNS 解析痕迹分析"

# 2.1 当前 DNS 缓存 (仅显示当前残留)
Write-Log "`n[检查] 本地 DNS 缓存 (仅显示未过期的记录)..."
$DnsOutput = ipconfig /displaydns 2>$null
$DnsMatches = $DnsOutput | Select-String $SearchPattern -Context 5,0
if ($DnsMatches) {
    Write-DataBlock "DNS 缓存中的记录" ($DnsMatches | Out-String)
} else {
    Write-Log "  本地 DNS 缓存中未发现目标域名 (记录可能已过期)。" "Gray"
}

# 2.2 DNS 客户端日志 (Microsoft-Windows-DNS-Client/Operational)
# 此日志默认可能未开启，但如果开启了，能查到历史记录
Write-Log "`n[检查] DNS 客户端操作日志 (Event ID 3008 - 查询)..."
try {
    # 尝试获取日志，如果日志未启用会报错
    $DnsLogs = Get-WinEvent -FilterHashtable @{LogName='Microsoft-Windows-DNS-Client/Operational'; Id=3008} -StartTime $StartDate -ErrorAction SilentlyContinue | 
               Where-Object { $_.Message -match $SearchPattern } |
               Select-Object -First 100 # 限制数量防止日志过大
    
    if ($DnsLogs) {
        Write-DataBlock "历史 DNS 查询记录 (最多 100 条)" @($DnsLogs | ForEach-Object {
            "[$($_.TimeCreated)] 查询: $($_.Message)"
        }) -join "`n"
        Write-Log "  [注] 以上时间为域名被解析的确切时间，可用于构建时间线。" "Cyan"
    } else {
        Write-Log "  未在 DNS 客户端操作日志中找到相关记录 (可能日志未启用或已被覆盖)。" "Gray"
    }
} catch {
    Write-Log "  无法读取 DNS 客户端日志: $($_.Exception.Message)" "Gray"
}

# ================= 模块 3: 文件与 MSI 安装痕迹 (长周期) =================
Write-Section "模块 3: 文件系统与安装痕迹 (2024 至今)"

# 3.1 扫描特定目录中 2024 年后创建的文件
Write-Log "`n[检查] 临时目录中 2024 年之后创建的可疑文件..."
$SuspiciousExts = @(".exe", ".dll", ".vbs", ".js", ".ps1", ".bat", ".jpg", ".png", ".tmp", ".msi")
$TempPaths = @("C:\Windows\Temp", "C:\Users\*\AppData\Local\Temp", "C:\ProgramData", "C:\Users\Public")
$FoundFiles = @()

foreach ($Path in $TempPaths) {
    try {
        $Files = Get-ChildItem -Path $Path -Recurse -ErrorAction SilentlyContinue | 
                 Where-Object { 
                     $_.LastWriteTime -gt $StartDate -and 
                     $SuspiciousExts -contains $_.Extension 
                 }
        # 进一步过滤：虽然不能直接搜文件内容（太慢），但可以文件名是否包含域名关键词（极少见）
        # 这里主要靠人工后续比对时间
        $FoundFiles += $Files
    } catch {}
}

if ($FoundFiles.Count -gt 0) {
    # 按时间排序，展示最近 50 个，并提示总数量
    $RecentFiles = $FoundFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 50
    Write-DataBlock "近期创建的可疑文件样本 (共 $($FoundFiles.Count) 个，仅展示前 50)" ($RecentFiles | Select-Object FullName, Length, LastWriteTime | Format-Table -AutoSize | Out-String)
    Write-Log "  [注] 完整文件列表已省略，请根据报告末尾的时间线自行核对。" "Gray"
} else {
    Write-Log "  未在指定临时目录发现 2024 年后创建的高风险扩展名文件。" "Green"
}

# 3.2 MSI 安装日志搜索
Write-Log "`n[检查] Windows Installer 日志 (搜索域名关键词)..."
$MsiLogs = Get-ChildItem -Path "C:\Windows\Temp" -Filter "MSI*.log" -ErrorAction SilentlyContinue | 
           Where-Object { $_.LastWriteTime -gt $StartDate }
$FoundMsiContent = $false

foreach ($Log in $MsiLogs) {
    try {
        # 逐行读取以避免大文件爆内存，找到即停
        $Content = Get-Content $Log.FullName -Raw -ErrorAction SilentlyContinue
        if ($Content -match $SearchPattern) {
            $FoundMsiContent = $true
            $Matches = $Content | Select-String $SearchPattern -Context 3,3
            Write-DataBlock "发现包含目标域名的 MSI 日志: $($Log.Name) (时间: $($Log.LastWriteTime))" ($Matches | Out-String)
        }
    } catch {}
}

if (-not $FoundMsiContent) {
    Write-Log "  未在 Temp 目录的 MSI 日志中发现直接域名引用。" "Gray"
}

# ================= 模块 4: 网络与进程现状 =================
Write-Section "模块 4: 当前网络连接与进程状态 (快照)"

# 4.1 当前连接
Write-Log "`n[检查] 当前活跃的网络连接 (ESTABLISHED)..."
$NetStat = netstat -ano | Select-String "ESTABLISHED"
$ExternalConns = @()
foreach ($Line in $NetStat) {
    $Parts = $Line.ToString().Trim() -split '\s+'
    if ($Parts.Count -ge 5) {
        $Remote = $Parts[2].Split(':')[0]
        if ($Remote -notmatch "^127.|^192.168.|^10.|^172.(1[6-9]|2[0-9]|3[01]).") {
            $ExternalConns += "$($Parts[4]) -> $Remote"
        }
    }
}
if ($ExternalConns) {
    Write-DataBlock "活跃的外网连接 (PID -> IP)" ($ExternalConns -join "`n")
} else {
    Write-Log "  当前无活跃的外网 ESTABLISHED 连接。" "Green"
}

# 4.2 当前进程
Write-Log "`n[检查] 包含目标域名的活跃进程命令行..."
$Procs = Get-WmiObject Win32_Process -ErrorAction SilentlyContinue | 
         Where-Object { $_.CommandLine -match $SearchPattern } |
         Select-Object ProcessId, Name, CommandLine, CreationDate

if ($Procs) {
    Write-DataBlock "发现包含目标域名的活跃进程" ($Procs | Format-Table -AutoSize | Out-String)
} else {
    Write-Log "  未发现包含目标域名的活跃进程。" "Green"
}

# ================= 模块 5: 事件日志深度溯源 (核心时间线) =================
Write-Section "模块 5: 安全与系统日志深度溯源 (2024 至今)"
Write-Log "正在检索海量日志，这可能需要几分钟..."

$TimelineEvents = @()

# 5.1 计划任务操作日志 (TaskScheduler/Operational)
# ID 106 (Created), 140 (Updated), 141 (Deleted), 100 (Executed)
Write-Log "`n[检索] 计划任务操作日志 (ID 106, 140, 141, 100)..."
try {
    $TaskLogs = Get-WinEvent -FilterHashtable @{LogName='Microsoft-Windows-TaskScheduler/Operational'; Id=106,140,141,100} -StartTime $StartDate -ErrorAction SilentlyContinue |
                Where-Object { $_.Message -match $SearchPattern } |
                Select-Object TimeCreated, Id, Message
    
    if ($TaskLogs) {
        $TimelineEvents += $TaskLogs | ForEach-Object {
            [PSCustomObject]@{
                Time = $_.TimeCreated
                Source = "TaskScheduler"
                EventID = $_.Id
                Detail = $_.Message
            }
        }
        Write-Log "  找到 $($TaskLogs.Count) 条相关计划任务日志。" "Yellow"
    }
} catch {
    Write-Log "  读取计划任务日志失败或日志已清空: $($_.Exception.Message)" "Gray"
}

# 5.2 安全日志 - 进程创建 (Security 4688)
# 需要开启 "Include Command Line in Process Creation Events" 才能看到命令行中的域名
Write-Log "`n[检索] 安全日志 - 进程创建 (ID 4688)..."
try {
    $ProcLogs = Get-WinEvent -FilterHashtable @{LogName='Security'; Id=4688} -StartTime $StartDate -ErrorAction SilentlyContinue |
                Where-Object { $_.Message -match $SearchPattern } |
                Select-Object -First 200 # 限制数量以防内存溢出
                
    if ($ProcLogs) {
        $TimelineEvents += $ProcLogs | ForEach-Object {
            [PSCustomObject]@{
                Time = $_.TimeCreated
                Source = "Security-Process"
                EventID = 4688
                Detail = $_.Message
            }
        }
        Write-Log "  找到 $($ProcLogs.Count) 条包含域名的进程创建记录。" "Yellow"
    }
} catch {
    Write-Log "  读取安全进程日志失败: $($_.Exception.Message)" "Gray"
}

# 5.3 安全日志 - 登录事件 (Security 4624) - 辅助关联
# 不直接搜域名，但提取任务创建时间点附近的登录
Write-Log "`n[检索] 安全日志 - 登录事件 (ID 4624) 辅助关联..."
# 策略：如果有任务日志，提取任务创建时间前后 1 小时的登录
if ($TimelineEvents.Count -gt 0) {
    $CriticalTimes = $TimelineEvents | Where-Object { $_.Source -eq "TaskScheduler" -and $_.EventID -eq 106 } | Select-Object -ExpandProperty Time
    $LoginEvents = @()
    foreach ($T in $CriticalTimes) {
        $Start = $T.AddHours(-1)
        $End = $T.AddHours(1)
        $Logs = Get-WinEvent -FilterHashtable @{LogName='Security'; Id=4624} -StartTime $Start -EndTime $End -ErrorAction SilentlyContinue |
                Where-Object { $_.Message -match "Logon Type:\s+(10|3|2)" -and $_.Message -notmatch "Account Name:\s+(DWM-|UMFD-|SYSTEM|LOCAL SERVICE|NETWORK SERVICE)" } |
                Select-Object -First 5
        $LoginEvents += $Logs
    }
    
    if ($LoginEvents) {
        $TimelineEvents += $LoginEvents | ForEach-Object {
            [PSCustomObject]@{
                Time = $_.TimeCreated
                Source = "Security-Login"
                EventID = 4624
                Detail = $_.Message
            }
        }
        Write-Log "  找到 $($LoginEvents.Count) 条与任务创建时间关联的登录记录。" "Yellow"
    }
}

# 输出时间线
if ($TimelineEvents.Count -gt 0) {
    Write-Section "综合时间线分析 (按时间正序)"
    $SortedTimeline = $TimelineEvents | Sort-Object Time
    
    $TimelineOutput = $SortedTimeline | ForEach-Object {
        "[$($_.Time)] [Source: $($_.Source)] [ID: $($_.EventID)]`n$($_.Detail)`n" + ("-"*50)
    }
    
    # 写入文件，避免控制台输出过长
    Add-Content -Path $LogPath -Value "`n================ 详细时间线开始 ================`n"
    $TimelineOutput | Add-Content -Path $LogPath
    Add-Content -Path $LogPath -Value "`n================ 详细时间线结束 ================`n"
    
    Write-Log "  详细时间线已写入日志文件末尾 (共 $($SortedTimeline.Count) 个事件)。" "Cyan"
    Write-Log "  请在生成的 .log 文件中查看完整时间线。" "Cyan"
} else {
    Write-Log "  未在事件日志中找到与目标域名直接相关的记录。" "Gray"
    Write-Log "  [可能性] 日志已被覆盖、被清除，或攻击者使用了不记录日志的手法。" "Gray"
}

# ================= 结束 =================
$Footer = @"
=================================================================
采集结束。
报告路径: $LogPath

报告内容包含:
1. 现存计划任务中的域名痕迹及解码内容。
2. DNS 缓存及历史解析记录 (如有)。
3. 2024 年至今创建的临时文件及 MSI 日志痕迹。
4. 当前活跃的网络连接与进程。
5. 基于事件日志构建的综合时间线 (任务创建、进程启动、登录关联)。

⚠️ 再次声明：本报告仅供分析，不含任何处置建议。
=================================================================
"@
Write-Log $Footer "Cyan"

Write-Host "`n✅ 长周期采集完成！请查看日志文件：$LogFileName" -ForegroundColor Green
Write-Host "   (由于数据量大，建议使用 Notepad++ 或 VS Code 打开查看)" -ForegroundColor Gray