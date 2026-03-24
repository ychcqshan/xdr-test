Stop-Process -Name java -Force -ErrorAction SilentlyContinue
Set-Location e:\project\xdr-test\xdr-server
Start-Process mvn.cmd -ArgumentList "clean", "package", "-pl", "asset-service", "-am", "-DskipTests", "-B" -Wait -NoNewWindow
powershell -File e:\project\xdr-test\scripts\start-backend.ps1
