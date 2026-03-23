# RA Platform Init — Runner
# Spustí setup.py pro inicializaci platformy přes REST API.
#
# Použití:
#   .\run.ps1                                    # výchozí init.json, localhost:80
#   .\run.ps1 -ApiUrl https://app.ra.local  # vlastní API URL
#   .\run.ps1 -Config .\my-config.json           # vlastní config
#   .\run.ps1 -SkipStep azure -SkipStep sso      # přeskočit kroky
#   .\run.ps1 -OnlyStep superadmin               # jen jeden krok
#   .\run.ps1 -WaitTimeout 120                   # čekat na API déle

param(
    [string]$Config = (Join-Path $PSScriptRoot "init.json"),
    [string]$ApiUrl = "http://localhost:8081",
    [int]$WaitTimeout = 30,
    [string[]]$SkipStep = @(),
    [string]$OnlyStep = "",
    [switch]$NoWait
)

$ErrorActionPreference = "Stop"
$setupScript = Join-Path $PSScriptRoot "setup.py"

# ── Kontroly ─────────────────────────────────────────────────────────────────

if (-not (Test-Path $setupScript)) {
    Write-Error "setup.py nenalezen v $PSScriptRoot"
    exit 1
}

if (-not (Test-Path $Config)) {
    Write-Error "Config soubor nenalezen: $Config"
    exit 1
}

# ── Python + requests ────────────────────────────────────────────────────────

$pythonCmd = if (Get-Command python -ErrorAction SilentlyContinue) { "python" } else { "python" }

try {
    & $pythonCmd --version 2>$null | Out-Null
}
catch {
    Write-Error "Python nenalezen. Nainstaluj Python 3.10+."
    exit 1
}

$prevEAP = $ErrorActionPreference
$ErrorActionPreference = 'SilentlyContinue'
& $pythonCmd -m pip install -q -r requirements.txt 2>&1 | Out-Null
$ErrorActionPreference = $prevEAP

# ── Sestavit argumenty ───────────────────────────────────────────────────────

$args_ = @(
    $setupScript,
    "--config", $Config,
    "--api-url", $ApiUrl,
    "--wait", $WaitTimeout
)

foreach ($step in $SkipStep) {
    $args_ += @("--skip-step", $step)
}

if ($OnlyStep) {
    $args_ += @("--only-step", $OnlyStep)
}

if ($NoWait) {
    $args_ += "--no-wait"
}

# ── Spustit ──────────────────────────────────────────────────────────────────

Write-Host "RA Platform Init" -ForegroundColor Cyan
Write-Host "  Config:  $Config" -ForegroundColor DarkGray
Write-Host "  API:     $ApiUrl" -ForegroundColor DarkGray
Write-Host ""

& $pythonCmd @args_
exit $LASTEXITCODE
