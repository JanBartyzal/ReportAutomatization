# Step12 API AI MCP — Runner
$logFile = "$PSScriptRoot/../logs/step12_API_AI_MCP.log"
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }
Push-Location $PSScriptRoot
python test_12_api_ai_mcp.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location
exit $exitCode
