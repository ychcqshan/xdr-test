# XDR Backend Microservices Shutdown Script
$ErrorActionPreference = "Continue"

Write-Host "--- Stopping XDR Backend Services ---" -ForegroundColor Cyan

$services = @(
    @{ name = "api-gateway"; port = 8080 },
    @{ name = "auth-service"; port = 8081 },
    @{ name = "asset-service"; port = 8082 },
    @{ name = "baseline-service"; port = 8083 },
    @{ name = "threat-service"; port = 8084 }
)

foreach ($svc in $services) {
    Write-Host "[*] Checking $($svc.name) on port $($svc.port)..."
    
    # 查找占用指定端口的进程
    $connections = Get-NetTCPConnection -LocalPort $svc.port -State Listen -ErrorAction SilentlyContinue
    
    if ($connections) {
        foreach ($conn in $connections) {
            $pidToKill = $conn.OwningProcess
            # PID 0 是系统空闲进程，4 是 System，通常不会是我们的 Java 进程，为了安全起见一并排除
            if ($pidToKill -gt 4) {
                Write-Host "    Found process (PID: $pidToKill). Stopping..." -ForegroundColor Yellow
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
                Write-Host "    [OK] Stopped $($svc.name)." -ForegroundColor Green
            }
        }
    }
    else {
        Write-Host "    [-] Service $($svc.name) is not running (Port $($svc.port) is free)." -ForegroundColor DarkGray
    }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "All XDR Backend services have been stopped."
