# Windows 入侵痕迹排查脚本 (基于典型后渗透取证思路)
# 请以管理员权限运行此脚本

$ReportPath = "$env:USERPROFILE\Desktop\Intrusion_Check_Report.txt"
"--- Windows 入侵痕迹排查报告 ---" | Out-File $ReportPath
"扫描时间: $(Get-Date)" | Out-File $ReportPath -Append

function Write-Section($Title) {
    "`n[!] $Title" | Tee-Object -FilePath $ReportPath -Append
    "--------------------------------" | Out-File $ReportPath -Append
}

# 1. 检查异常网络连接 (ESTABLISHED状态)
Write-Section "异常网络连接 (ESTABLISHED)"
Get-NetTCPConnection -State Established | Select-Object LocalAddress, LocalPort, RemoteAddress, RemotePort, OwningProcess | 
    ForEach-Object {
        $Process = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        $_ | Add-Member -NotePropertyName ProcessName -NotePropertyValue $Process.Name -PassThru
    } | Format-Table | Out-File $ReportPath -Append

# 2. 检查可疑进程 (无签名或非System32路径下的系统名进程)
Write-Section "可疑进程扫描 (检查路径与名称)"
$SuspiciousNames = @("lsass.exe", "svchost.exe", "services.exe", "wininit.exe")
Get-Process | ForEach-Object {
    $Path = $_.Path
    if ($null -ne $Path) {
        # 检查是否伪装系统进程名但在异常路径
        foreach ($name in $SuspiciousNames) {
            if ($_.Name -eq $name.Replace(".exe","") -and $Path -notlike "*\Windows\System32\*") {
                "警告: 发现伪装进程 $($_.Name) 路径: $Path" | Tee-Object -FilePath $ReportPath -Append
            }
        }
    }
}

# 3. 检查账户风险
Write-Section "账户检查 (隐藏账户/新增账户)"
net user | Out-File $ReportPath -Append
"隐藏账户检查 (需手动对比本地用户组):" | Out-File $ReportPath -Append
Get-LocalUser | Select-Object Name, Enabled, LastLogon | Format-Table | Out-File $ReportPath -Append

# 4. 检查启动项 (Registry & Startup Folder)
Write-Section "启动项检查"
$RunKeys = @(
    "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Run",
    "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\RunOnce",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run",
    "HKCU:\Software\Microsoft\Windows\CurrentVersion\RunOnce"
)
foreach ($key in $RunKeys) {
    if (Test-Path $key) {
        Get-ItemProperty $key | Out-File $ReportPath -Append
    }
}

# 5. 检查最近 24 小时内修改的可疑文件 (Temp/Download/System32)
Write-Section "最近 24 小时内修改的文件 (Temp/System32)"
$CheckPaths = @("$env:TEMP", "C:\Windows\System32", "$env:USERPROFILE\Downloads")
foreach ($p in $CheckPaths) {
    Get-ChildItem -Path $p -Recurse -ErrorAction SilentlyContinue | 
        Where-Object { $_.LastWriteTime -gt (Get-Date).AddDays(-1) } | 
        Select-Object FullName, LastWriteTime | Out-File $ReportPath -Append
}

# 6. 检查计划任务 (排查异常脚本执行)
Write-Section "计划任务检查 (仅列出非微软任务)"
Get-ScheduledTask | Where-Object { $_.Author -notlike "*Microsoft*" } | 
    Select-Object TaskName, TaskPath, State | Format-Table | Out-File $ReportPath -Append

Write-Host "`n[+] 检查完成！报告已保存至: $ReportPath" -ForegroundColor Green