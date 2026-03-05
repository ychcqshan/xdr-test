# XDR Backend Microservices Build Script
$ErrorActionPreference = "Stop"

$rootPath = "e:\project\xdr-test\xdr-server"

Write-Host "--- XDR Backend Build Program ---" -ForegroundColor Cyan
Write-Host "[*] Starting clean build for all microservices..." -ForegroundColor Yellow

# 1. Check Java Environment
try {
    java -version
}
catch {
    Write-Error "Java not found in PATH. Please ensure JDK 17+ is installed."
    exit
}

# 2. Check Maven Environment
try {
    mvn -version
}
catch {
    Write-Error "Maven not found in PATH. Please ensure Maven is installed and configured."
    exit
}

# 3. Build Project
cd $rootPath

Write-Host "[*] Executing 'mvn clean install -DskipTests' in $rootPath" -ForegroundColor Yellow
mvn clean install -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "[OK] All backend services successfully built and packaged!" -ForegroundColor Green
    Write-Host "JAR files are generated in the target/ directory of each respective module."
}
else {
    Write-Host "==========================================" -ForegroundColor Red
    Write-Host "[!] Build failed. Please check the Maven output logs above." -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to exit this script..."
Read-Host
