param(
    [switch]$Clean = $true
)

Write-Host "Building XDR Agent (Windows)..." -ForegroundColor Cyan

if ($Clean) {
    Write-Host "Cleaning old build and dist directories..."
    if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
    if (Test-Path "dist") { Remove-Item -Recurse -Force "dist" }
}

Write-Host "Running PyInstaller..."
python -m PyInstaller xdr-agent-win.spec --clean

if ($LASTEXITCODE -ne 0) {
    Write-Error "PyInstaller build failed!"
    exit $LASTEXITCODE
}

Write-Host "Copying config.yaml to dist directory..."
Copy-Item "config.yaml" -Destination "dist\config.yaml" -Force

Write-Host "Build complete! Artifacts are in the dist/ folder." -ForegroundColor Green
Write-Host "Contains:"
Write-Host " - dist/xdr-agent-win.exe"
Write-Host " - dist/config.yaml"
