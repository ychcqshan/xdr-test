# XDR Backend Microservices Build Script
$ErrorActionPreference = "Stop"

$rootPath = "e:\project\xdr-test\xdr-server"
$scriptPath = "e:\project\xdr-test\scripts"
$stopScript = "$scriptPath\stop-backend.ps1"

Write-Host "--- XDR Backend Build Program ---" -ForegroundColor Cyan

# 1. 环境检查
if (!(Get-Command java -ErrorAction SilentlyContinue)) { Write-Error "Java not found."; exit 1 }
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) { Write-Error "Maven not found."; exit 1 }

# 2. 构建前清理
Write-Host "[*] Checking for running backend processes..." -ForegroundColor Yellow
$running = Get-NetTCPConnection -LocalPort 8080,8081,8082,8083,8084,8085,8086,8087 -State Listen -ErrorAction SilentlyContinue
if ($running) {
    Write-Host "[!] Detected running services. Stopping..." -ForegroundColor Yellow
    powershell -ExecutionPolicy Bypass -File "$stopScript"
}

# 3. 执行编译 (使用 cmd /c 以获得最强 Maven 兼容性)
Set-Location $rootPath
Write-Host "[*] Executing 'mvn clean install -DskipTests'..." -ForegroundColor Yellow
cmd /c "mvn clean install -DskipTests"

if ($LASTEXITCODE -eq 0) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "[OK] All backend services built!" -ForegroundColor Green
} else {
    Write-Error "Build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Press any key to exit..."
Read-Host
