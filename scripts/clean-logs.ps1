# XDR-Test 项目日志与调试文件清理脚本
# 作用: 清理 logs 目录下的日志以及根目录下的临时调试文件

Write-Host "--- Starting Cleanup ---" -ForegroundColor Cyan

$rootPath = "e:\project\xdr-test"
$logsPath = Join-Path $rootPath "logs"

# 1. 清理 logs 目录
if (Test-Path $logsPath) {
    Write-Host "[*] Cleaning logs directory..." -ForegroundColor Yellow
    Remove-Item -Path "$logsPath\*" -Force -Recurse -ErrorAction SilentlyContinue
}

# 2. 清理根目录下的杂质文件
Write-Host "[*] Cleaning debug files in root..." -ForegroundColor Yellow
$patterns = @(
    "*.log", 
    "*stacktrace.txt", 
    "api_check.json", 
    "api_check.txt",
    "asset-error*.txt",
    "asset_err.txt",
    "asset_log.txt",
    "auth_compile*.log",
    "build-output.txt",
    "build.log",
    "build_*.log",
    "debug_*.txt",
    "debug_*.log",
    "err_auth.log",
    "startup_debug.log",
    "verification_*.json",
    "verification_*.txt",
    "verify-*.txt",
    "xdr_threat_host_asset.txt"
)

foreach ($pattern in $patterns) {
    $files = Get-ChildItem -Path $rootPath -Filter $pattern -File -ErrorAction SilentlyContinue
    foreach ($file in $files) {
        # 排除重要脚本和基础事实文档
        if ($file.Name -ne "gemini.md" -and $file.Name -ne "package.json" -and $file.Name -ne "package-lock.json") {
            Remove-Item -Path $file.FullName -Force -ErrorAction SilentlyContinue
            Write-Host "[-] Deleted: $($file.Name)" -ForegroundColor DarkGray
        }
    }
}

Write-Host "================================" -ForegroundColor Green
Write-Host "Cleanup completed successfully." -ForegroundColor Green
Write-Host "Note: Some files may remain if they are currently locked by running processes." -ForegroundColor Gray
