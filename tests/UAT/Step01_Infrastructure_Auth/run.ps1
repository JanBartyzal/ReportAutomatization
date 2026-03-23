# Step01 Infrastructure Auth — Runner
$logFile = "$PSScriptRoot/../logs/step01_Infrastructure_Auth.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_01_infrastructure_auth.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
