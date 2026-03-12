# =============================================================================
# dev-start.ps1 - Start the Tilt local development environment
# =============================================================================
# Usage:
#   .\scripts\dev-start.ps1                  # Start Tilt
#   .\scripts\dev-start.ps1 -Observability   # Include observability stack
# =============================================================================

[CmdletBinding()]
param(
    [switch]$Observability
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path) }

# ---------------------------------------------------------------------------
# Pre-flight Checks
# ---------------------------------------------------------------------------
Write-Host "[INFO]  Running pre-flight checks..." -ForegroundColor Green

# Tilt
if (-not (Get-Command tilt -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Tilt is not installed." -ForegroundColor Red
    Write-Host "  Install: scoop install tilt"
    Write-Host "  Or: https://docs.tilt.dev/install.html"
    exit 1
}
Write-Host "[INFO]  Tilt found: $(tilt version)" -ForegroundColor Green

# Docker
try {
    docker info 2>&1 | Out-Null
} catch {
    Write-Host "[ERROR] Docker is not running. Start Docker Desktop first." -ForegroundColor Red
    exit 1
}
Write-Host "[INFO]  Docker is running." -ForegroundColor Green

# Docker Compose
try {
    docker compose version 2>&1 | Out-Null
} catch {
    Write-Host "[ERROR] Docker Compose v2 is not available." -ForegroundColor Red
    exit 1
}
Write-Host "[INFO]  Docker Compose found: $(docker compose version --short)" -ForegroundColor Green

# ---------------------------------------------------------------------------
# Environment File
# ---------------------------------------------------------------------------
$EnvFile = Join-Path $ProjectRoot "infra/docker/.env"
$EnvExample = Join-Path $ProjectRoot "infra/docker/.env.example"

if (-not (Test-Path $EnvFile)) {
    if (Test-Path $EnvExample) {
        Write-Host "[WARN]  .env not found. Copying from .env.example..." -ForegroundColor Yellow
        Copy-Item $EnvExample $EnvFile
        Write-Host "[INFO]  Created $EnvFile" -ForegroundColor Green
        Write-Host "[WARN]  Review values before proceeding." -ForegroundColor Yellow
    } else {
        Write-Host "[WARN]  No .env or .env.example found." -ForegroundColor Yellow
    }
} else {
    Write-Host "[INFO]  Environment file found." -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# Start Tilt
# ---------------------------------------------------------------------------
Set-Location $ProjectRoot

$tiltArgs = @("up")
if ($Observability) {
    $tiltArgs += @("--", "--enable-observability")
}

Write-Host ""
Write-Host "[INFO]  Starting Tilt..." -ForegroundColor Green
Write-Host ""
Write-Host "  Tilt UI: http://localhost:10350"
Write-Host "  Press Ctrl+C to stop watching (services keep running)"
Write-Host "  Run '.\scripts\dev-stop.ps1' to tear down everything"
Write-Host ""

& tilt @tiltArgs
