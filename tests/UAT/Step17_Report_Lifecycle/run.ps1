# Step17 Report Lifecycle — Runner
$logFile = "$PSScriptRoot/../logs/step17_Report_Lifecycle.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_17_report_lifecycle.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
