# =============================================================================
# dev-stop.ps1 - Stop the Tilt local development environment
# =============================================================================
# Usage:
#   .\scripts\dev-stop.ps1           # Stop services
#   .\scripts\dev-stop.ps1 -Clean    # Stop + remove volumes
# =============================================================================

[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path) }

Set-Location $ProjectRoot

Write-Host "[INFO]  Stopping Tilt and all services..." -ForegroundColor Green

if (Get-Command tilt -ErrorAction SilentlyContinue) {
    & tilt down
    Write-Host "[INFO]  Tilt environment stopped." -ForegroundColor Green
} else {
    Write-Host "[WARN]  Tilt not found. Using docker compose down..." -ForegroundColor Yellow
    & docker compose -f infra/docker/docker-compose.yml down
    Write-Host "[INFO]  Docker Compose services stopped." -ForegroundColor Green
}

if ($Clean) {
    Write-Host "[WARN]  Removing Docker volumes (data will be lost)..." -ForegroundColor Yellow
    & docker compose -f infra/docker/docker-compose.yml down -v
    Write-Host "[INFO]  Volumes removed." -ForegroundColor Green
}

Write-Host "[INFO]  Done." -ForegroundColor Green
