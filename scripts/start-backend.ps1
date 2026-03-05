# XDR Backend Microservices Startup Script
$ErrorActionPreference = "Stop"

$rootPath = "e:\project\xdr-test\xdr-server"
$logDir = "e:\project\xdr-test\logs"
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

Write-Host "--- XDR Backend Startup Program ---" -ForegroundColor Cyan

# 0. Create log directory
if (!(Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
    Write-Host "[OK] Created log directory: $logDir" -ForegroundColor Green
}

# 1. Check Java Environment
try {
    java -version
}
catch {
    Write-Error "Java not found in PATH. Please ensure JDK 17+ is installed."
    exit
}

# 2. Build Project if needed - check all JAR files exist
Write-Host "[*] Checking build status..." -ForegroundColor Yellow
cd $rootPath
$needBuild = $false
foreach ($svc in $services) {
    $jarFile = "$rootPath\$($svc.path)\target\$($svc.name)-1.0.0-SNAPSHOT.jar"
    if (!(Test-Path $jarFile)) {
        Write-Host "[!] Missing JAR: $($svc.name)" -ForegroundColor Red
        $needBuild = $true
    }
}
if ($needBuild) {
    Write-Host "[!] Some JAR files are missing. Running 'mvn clean install -DskipTests'..." -ForegroundColor Yellow
    mvn clean install -DskipTests
}
else {
    Write-Host "[OK] All JAR files are ready." -ForegroundColor Green
}

# 3. Start Services with log redirection
foreach ($svc in $services) {
    $jarFile = "$rootPath\$($svc.path)\target\$($svc.name)-1.0.0-SNAPSHOT.jar"
    $logFile = "$logDir\$($svc.name).log"
    $errFile = "$logDir\$($svc.name)-error.log"

    Write-Host "[+] Starting $($svc.name) (Port: $($svc.port)), log -> $logFile" -ForegroundColor Green

    # Start each service, redirect stdout+stderr to log file (hidden window)
    $argList = "-jar `"$jarFile`""
    Start-Process java -ArgumentList $argList -WorkingDirectory "$rootPath\$($svc.path)" -WindowStyle Hidden `
        -RedirectStandardOutput $logFile -RedirectStandardError $errFile

    # Delay to prevent port conflicts or excessive CPU usage
    Start-Sleep -Seconds 5
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "All startup requests sent."
Write-Host "Gateway URL: http://localhost:8080"
Write-Host "Log directory: $logDir" -ForegroundColor Yellow
Write-Host ""
Write-Host "View logs with:" -ForegroundColor Yellow
Write-Host "  Get-Content $logDir\api-gateway.log -Tail 50 -Wait"
Write-Host "  Get-Content $logDir\auth-service.log -Tail 50 -Wait"
Write-Host ""
Write-Host "Press any key to exit this script (backend will keep running)..."
Read-Host
