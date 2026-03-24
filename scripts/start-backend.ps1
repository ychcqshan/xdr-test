# XDR Backend Microservices Startup Script
$ErrorActionPreference = "Stop"

$rootPath = "e:\project\xdr-test\xdr-server"
$logDir = "e:\project\xdr-test\logs"
$scriptPath = "e:\project\xdr-test\scripts"
$stopScript = "$scriptPath\stop-backend.ps1"

$services = @(
    @{ name = "auth-service"; path = "auth-service"; port = 8081 },
    @{ name = "asset-service"; path = "asset-service"; port = 8082 },
    @{ name = "baseline-service"; path = "baseline-service"; port = 8083 },
    @{ name = "threat-service"; path = "threat-service"; port = 8084 },
    @{ name = "policy-service"; path = "policy-service"; port = 8085 },
    @{ name = "upgrade-service"; path = "upgrade-service"; port = 8086 },
    @{ name = "compliance-service"; path = "compliance-service"; port = 8087 },
    @{ name = "api-gateway"; path = "api-gateway"; port = 8080 }
)

Write-Host "--- XDR Backend Startup Program (Enhanced) ---" -ForegroundColor Cyan

# 1. 强力清理
Write-Host "[*] Cleaning up environment..." -ForegroundColor Yellow
powershell -ExecutionPolicy Bypass -File "$stopScript"

# 2. 检查日志目录
if (!(Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

# 3. 逐个启动
foreach ($svc in $services) {
    $jarFile = "$rootPath\$($svc.path)\target\$($svc.name)-1.0.0-SNAPSHOT.jar"
    $logFile = "$logDir\$($svc.name).log"
    $errFile = "$logDir\$($svc.name)-error.log"

    if (!(Test-Path $jarFile)) {
        Write-Host "[!] Missing JAR for $($svc.name). Build first." -ForegroundColor Red
        continue
    }

    Write-Host "[+] Starting $($svc.name) (Port $($svc.port))..." -ForegroundColor Cyan
    Start-Process java -ArgumentList "-jar `"$jarFile`"" -WorkingDirectory "$rootPath\$($svc.path)" -WindowStyle Hidden `
        -RedirectStandardOutput $logFile -RedirectStandardError $errFile

    # 4. 健康轮询
    $timeout = 45; $elapsed = 0; $success = $false
    Write-Host "    Waiting..." -NoNewline
    while ($elapsed -lt $timeout) {
        if (Get-NetTCPConnection -LocalPort $svc.port -State Listen -ErrorAction SilentlyContinue) {
            $success = $true; break
        }
        Write-Host "." -NoNewline; Start-Sleep -Seconds 2; $elapsed += 2
    }

    if ($success) {
        Write-Host " [OK]" -ForegroundColor Green
    } else {
        Write-Host " [FAILED]" -ForegroundColor Red
        if ($svc.name -eq "auth-service" -or $svc.name -eq "api-gateway") {
            Write-Error "Critical service $($svc.name) failed to start."
            exit 1
        }
    }
}

Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host "XDR Backend cluster is ready!" -ForegroundColor Green
Write-Host "Gateway: http://localhost:8080"
Read-Host "Press Enter to exit..."
