# Step00 Init — Runner
# Spustí Python init skript (infra/init/setup.py) přes REST API a pak verifikační test.

param(
    [string]$InitJsonPath = (Resolve-Path "$PSScriptRoot/../config/uat_init.json"),
    [string]$ApiUrl = "http://localhost:8080",
    [int]$WaitTimeout = 60,
    [switch]$SkipInit
)

Write-Host "Before start run init from config folder" -ForegroundColor Cyan

$logFile = "$PSScriptRoot/../logs/step00_Init.log"

# Ensure logs directory exists
$logsDir = Split-Path $logFile -Parent
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }

Write-Host ""
Write-Host "Verifying initialization via API..." -ForegroundColor Cyan
Push-Location $PSScriptRoot
python test_00_init.py 2>&1 | Tee-Object -FilePath $logFile -Append
$exitCode = $LASTEXITCODE
Pop-Location

exit $exitCode
