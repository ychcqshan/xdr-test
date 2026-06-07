Write-Host "1. Creating fresh isolated virtual environment (venv)..." -ForegroundColor Cyan
python -m venv venv-build

Write-Host "2. Activating venv and installing core dependencies..." -ForegroundColor Cyan
.\venv-build\Scripts\python.exe -m pip install -U pip
.\venv-build\Scripts\python.exe -m pip install -r requirements.txt
.\venv-build\Scripts\python.exe -m pip install pyinstaller

Write-Host "3. Cleaning old build artifacts..." -ForegroundColor Cyan
if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }

Write-Host "4. Starting clean PyInstaller build..." -ForegroundColor Cyan
.\venv-build\Scripts\pyinstaller.exe xdr-agent-win.spec --clean

Write-Host "5. Copying output to release directory..." -ForegroundColor Cyan
$releaseDir = "..\xdr-release-v1.1"
if (-Not (Test-Path "$releaseDir\agent")) { New-Item -ItemType Directory -Force "$releaseDir\agent" }
if (-Not (Test-Path "$releaseDir\frontend\downloads")) { New-Item -ItemType Directory -Force "$releaseDir\frontend\downloads" }

Copy-Item "dist\xdr-agent-win.exe" -Destination "$releaseDir\agent\xdr-agent-win.exe" -Force
Copy-Item "config.yaml" -Destination "$releaseDir\agent\config.yaml" -Force

Copy-Item "dist\xdr-agent-win.exe" -Destination "$releaseDir\frontend\downloads\xdr-agent-win.exe" -Force

Write-Host "Clean build complete! Size and speed should be greatly optimized." -ForegroundColor Green
