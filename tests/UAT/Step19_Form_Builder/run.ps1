# Step19 Form Builder — Runner
$logFile = "$PSScriptRoot/../logs/step19_Form_Builder.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_19_form_builder.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
