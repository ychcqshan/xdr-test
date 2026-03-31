<#
.SYNOPSIS
    Windows 主机被攻击痕迹自动化排查脚本
.DESCRIPTION
    收集系统日志、网络连接、进程、启动项、计划任务、最近文件等信息并输出到日志文件。
    需要管理员权限运行。
.NOTES
    作者：AI Assistant
    日期：2026-03-16
#>

# 确保以管理员身份运行
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Warning "请以管理员身份运行此脚本！"
    Write-Host "正在尝试请求管理员权限..."
    Start-Process powershell.exe "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# 配置输出文件
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFileName = "Security_Audit_$Timestamp.log"
$LogPath = Join-Path $PWD $LogFileName

# 初始化日志文件
$Header = @"
============================================================
Windows 主机安全排查报告
生成时间: $(Get-Date)
主机名: $env:COMPUTERNAME
当前用户: $env:USERNAME
============================================================

"@
Add-Content -Path $LogPath -Value $Header

# 定义辅助函数：写入分隔线和标题
function Write-Section {
    param([string]$Title)
    $Separator = "=" * 60
    $SectionHeader = "`n$Separator`n[+] $Title`n$Separator"
    Add-Content -Path $LogPath -Value $SectionHeader
    Write-Host $SectionHeader -ForegroundColor Cyan
}

# 定义辅助函数：执行命令并记录输出
function Run-Command {
    param(
        [string]$Cmd,
        [string]$Description
    )
    Write-Host "正在检查: $Description ..." -ForegroundColor Gray
    
    # 添加注释
    Add-Content -Path $LogPath -Value "`n# $Description"
    Add-Content -Path $LogPath -Value "# 命令: $Cmd"
    
    try {
        # 使用 Out-String 捕获输出，避免格式混乱
        $Output = Invoke-Expression $Cmd 2>&1 | Out-String
        if ($Output.Trim() -eq "") {
            Add-Content -Path $LogPath -Value "(无相关记录)"
        } else {
            Add-Content -Path $LogPath -Value $Output
        }
    } catch {
        Add-Content -Path $LogPath -Value "[错误] 执行失败: $($_.Exception.Message)"
    }
}

# 1. 系统基本信息
Write-Section "1. 系统基本信息与网络配置"
Run-Command "systeminfo" "系统详细信息"
Run-Command "ipconfig /all" "网络接口详细配置"
Run-Command "net user" "本地用户列表"
Run-Command "net localgroup administrators" "管理员组成员"

# 2. 网络连接与端口 (重点排查 ESTABLISHED 和 LISTENING)
Write-Section "2. 网络连接与端口 (Netstat)"
Run-Command "netstat -ano | findstr /C:'ESTABLISHED' /C:'LISTENING'" "活跃连接与监听端口 (含PID)"

# 3. 进程信息
Write-Section "3. 进程列表 (含路径与父进程)"
# 使用 wmic 获取更详细的进程树信息
Run-Command "wmic process get Name,ProcessId,ParentProcessId,ExecutablePath,CommandLine /format:list" "详细进程列表"

# 4. 安全事件日志 (关键 ID)
Write-Section "4. 关键安全事件日志 (Event Logs)"
Write-Host "正在提取安全日志 (可能需要几秒钟)..." -ForegroundColor Gray

$EventQueries = @(
    @{ID="4624"; Desc="登录成功 (重点关注 Type 10 远程桌面)"},
    @{ID="4625"; Desc="登录失败 (暴力破解痕迹)"},
    @{ID="4720"; Desc="创建新用户"},
    @{ID="4732"; Desc="用户加入特权组"},
    @{ID="1102"; Desc="审计日志被清除"},
    @{ID="7045"; Desc="新服务安装"},
    @{ID="4698"; Desc="计划任务创建"}
)

foreach ($Query in $EventQueries) {
    Add-Content -Path $LogPath -Value "`n--- 事件 ID $($Query.ID): $($Query.Desc) ---"
    try {
        # 获取最近 50 条相关日志
        $Logs = Get-WinEvent -FilterHashtable @{LogName='Security';Id=$Query.ID} -MaxEvents 500 -ErrorAction SilentlyContinue | 
            Select-Object TimeCreated, Id, LevelDisplayName, Message | 
            Format-Table -AutoSize | Out-String
        
        if ($Logs) {
            Add-Content -Path $LogPath -Value $Logs
        } else {
            Add-Content -Path $LogPath -Value "(未找到相关日志)"
        }
    } catch {
        Add-Content -Path $LogPath -Value "(读取日志失败或无权限: $($_.Exception.Message))"
    }
}

# 5. 持久化机制：启动项与注册表
Write-Section "5. 持久化机制：启动项与注册表"
Run-Command "reg query 'HKLM\Software\Microsoft\Windows\CurrentVersion\Run'" "机器级启动项 (HKLM)"
Run-Command "reg query 'HKCU\Software\Microsoft\Windows\CurrentVersion\Run'" "用户级启动项 (HKCU)"
Run-Command "reg query 'HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon'" "Winlogon 自动登录设置"
Run-Command "dir '$env:APPDATA\Microsoft\Windows\Start Menu\Programs\Startup' /s" "启动文件夹内容 (当前用户)"
Run-Command "dir 'C:\ProgramData\Microsoft\Windows\Start Menu\Programs\StartUp' /s" "启动文件夹内容 (所有用户)"

# 6. 持久化机制：计划任务
Write-Section "6. 计划任务 (Scheduled Tasks)"
Run-Command "schtasks /query /fo LIST /v" "所有计划任务详细信息"

# 7. 持久化机制：服务
Write-Section "7. 系统服务 (Services)"
# 筛选出非微软签名的服务或者最近修改的服务比较困难，这里列出所有自动启动的服务
Run-Command "gwmi Win32_Service | Where-Object {$_.StartMode -eq 'Auto'} | Select-Object Name, State, PathName, StartName | Format-Table -AutoSize" "自动启动的服务"

# 8. 文件系统痕迹：预读取与最近文档
Write-Section "8. 文件系统痕迹 (Prefetch & Recent)"
if (Test-Path "C:\Windows\Prefetch") {
    Run-Command "dir C:\Windows\Prefetch /o-d /t:w" "Prefetch 文件夹 (按最后写入时间排序，最近运行的程序)"
} else {
    Add-Content -Path $LogPath -Value "Prefetch 文件夹不存在或未启用"
}

if (Test-Path "$env:USERPROFILE\Recent") {
    Run-Command "dir '$env:USERPROFILE\Recent' /o-d" "最近访问的文档快捷方式"
}

# 9. 检查隐藏文件 (系统盘根目录及Temp)
Write-Section "9. 可疑隐藏文件扫描"
Write-Host "正在扫描系统盘根目录和 Temp 目录的隐藏/系统文件..." -ForegroundColor Gray
$HiddenFiles = Get-ChildItem -Path "C:\", "$env:TEMP", "$env:LOCALAPPDATA\Temp" -Recurse -Force -ErrorAction SilentlyContinue | 
    Where-Object { ($_.Attributes -band [System.IO.FileAttributes]::Hidden) -or ($_.Attributes -band [System.IO.FileAttributes]::System) } |
    Select-Object FullName, Length, LastWriteTime, Attributes | Format-Table -AutoSize | Out-String

Add-Content -Path $LogPath -Value $HiddenFiles
if ($HiddenFiles.Trim() -eq "") {
    Add-Content -Path $LogPath -Value "(未发现明显的隐藏/系统属性文件)"
}

# 结束
$Footer = "`n============================================================`n排查结束。请人工分析上述日志中的异常项。`n建议重点关注：未知IP的连接、陌生的进程路径、非预期的启动项、非工作时间的登录日志。`n============================================================"
Add-Content -Path $LogPath -Value $Footer

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "排查完成！" -ForegroundColor Green
Write-Host "日志文件已生成: $LogPath" -ForegroundColor Yellow
Write-Host "请将此文件发送给安全专家进行分析。" -ForegroundColor Green
Write-Host "========================================`n"