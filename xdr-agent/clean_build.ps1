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

Write-Host "5. Copying external configuration..." -ForegroundColor Cyan
Copy-Item "config.yaml" -Destination "dist\config.yaml" -Force

Write-Host "Clean build complete! Size and speed should be greatly optimized." -ForegroundColor Green
