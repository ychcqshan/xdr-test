$agentProcesses = Get-WmiObject Win32_Process | Where-Object { $_.Name -match "python" }
if ($agentProcesses) {
    foreach ($process in $agentProcesses) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Output "Stopped PID $($process.ProcessId)"
    }
}
else {
    Write-Output "No running Agent processes found."
}
