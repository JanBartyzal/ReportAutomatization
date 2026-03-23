# Step04 Orchestrator Workflow — Runner
$logFile = "$PSScriptRoot/../logs/step04_Orchestrator_Workflow.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_04_orchestrator_workflow.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
