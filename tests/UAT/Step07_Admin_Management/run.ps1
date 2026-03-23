# Step07 Admin Management — Runner
$logFile = "$PSScriptRoot/../logs/step07_Admin_Management.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_07_admin_management.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
