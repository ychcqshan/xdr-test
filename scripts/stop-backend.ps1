# XDR Backend Microservices Shutdown Script
$ErrorActionPreference = "Continue"

Write-Host "--- Stopping XDR Backend Services ---" -ForegroundColor Cyan

$services = @(
    @{ name = "api-gateway"; port = 8080 },
    @{ name = "auth-service"; port = 8081 },
    @{ name = "asset-service"; port = 8082 },
    @{ name = "baseline-service"; port = 8083 },
    @{ name = "threat-service"; port = 8084 },
    @{ name = "policy-service"; port = 8085 },
    @{ name = "upgrade-service"; port = 8086 },
    @{ name = "compliance-service"; port = 8087 }
)

# 1. 尝试通过端口精确定位并关闭
foreach ($svc in $services) {
    Write-Host "[*] Checking $($svc.name) on port $($svc.port)..."
    $connections = Get-NetTCPConnection -LocalPort $svc.port -State Listen -ErrorAction SilentlyContinue
    
    if ($connections) {
        foreach ($conn in $connections) {
            $pidToKill = $conn.OwningProcess
            if ($pidToKill -gt 4) {
                Write-Host "    Found process (PID: $pidToKill) listening on $($svc.port). Stopping..." -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

# 2. 补漏：通过进程命令行深度清理残留的 xdr-server 进程 (处理那些占用了端口但 netstat 没写 Owner 的诡异情况)
Write-Host "[*] Performing deep cleanup for residual XDR Java processes..." -ForegroundColor Cyan
$residualProcesses = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*xdr-server*" -and $_.Name -eq "java.exe" }

if ($residualProcesses) {
    foreach ($proc in $residualProcesses) {
        Write-Host "    Force killing residual process (PID: $($proc.ProcessId)): $($proc.CommandLine.Substring(0, [Math]::Min(100, $proc.CommandLine.Length)))..." -ForegroundColor Red
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "All XDR Backend services have been stopped and cleaned." -ForegroundColor Green
