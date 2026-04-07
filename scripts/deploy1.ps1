# =============================================================================
# deploy.ps1 - Deploy services to Docker Compose environment
# =============================================================================
# Usage:
#   .\scripts\deploy.ps1                        # Build + start all
#   .\scripts\deploy.ps1 -UpOnly                # Start without rebuilding
#   .\scripts\deploy.ps1 -BuildOnly             # Build only
#   .\scripts\deploy.ps1 -Observability         # Include observability stack
#   .\scripts\deploy.ps1 -Clean                 # Clean deploy (remove volumes)
#   .\scripts\deploy.ps1 -Detach               # Run in background
#   .\scripts\deploy.ps1 -Service engine-core   # Deploy specific service
# =============================================================================

[CmdletBinding()]
param(
    [string[]]$Service = @(),
    [switch]$UpOnly,
    [switch]$BuildOnly,
    [switch]$Observability,
    [switch]$Clean,
    [switch]$Detach
)

$ErrorActionPreference = "Stop"
$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$ProjectRoot = Split-Path -Parent $ScriptDir
Write-Host "[INFO]  Project root: $ProjectRoot" -ForegroundColor DarkGray

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
$ComposeFile = Join-Path $ProjectRoot "infra/docker/docker-compose.yml"
$ComposeOverride = Join-Path $ProjectRoot "infra/docker/docker-compose.override.yml"
$ComposeObservability = Join-Path $ProjectRoot "infra/docker/docker-compose.observability.yml"
$EnvFile = Join-Path $ProjectRoot "infra/docker/.env"
$EnvExample = Join-Path $ProjectRoot "infra/docker/.env.example"

# ---------------------------------------------------------------------------
# Pre-flight
# ---------------------------------------------------------------------------
$null = docker info 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker is not running." -ForegroundColor Red
    exit 1
}

$null = docker compose version 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Docker Compose v2 is not available." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $ComposeFile)) {
    Write-Host "[ERROR] docker-compose.yml not found at $ComposeFile" -ForegroundColor Red
    exit 1
}

# Environment file
if (-not (Test-Path $EnvFile)) {
    if (Test-Path $EnvExample) {
        Write-Host "[WARN]  .env not found. Copying from .env.example..." -ForegroundColor Yellow
        Copy-Item $EnvExample $EnvFile
        Write-Host "[INFO]  Created $EnvFile" -ForegroundColor Green
        Write-Host "[WARN]  Review values in $EnvFile before proceeding." -ForegroundColor Yellow
    } else {
        Write-Host "[WARN]  No .env file found. Services may fail." -ForegroundColor Yellow
    }
}

# ---------------------------------------------------------------------------
# Build compose command
# ---------------------------------------------------------------------------
$ComposeArgs = @("-f", $ComposeFile)

if (Test-Path $ComposeOverride) {
    $ComposeArgs += @("-f", $ComposeOverride)
}

if ($Observability -and (Test-Path $ComposeObservability)) {
    $ComposeArgs += @("-f", $ComposeObservability)
    Write-Host "[INFO]  Observability stack included." -ForegroundColor Green
}

if (Test-Path $EnvFile) {
    $ComposeArgs += @("--env-file", $EnvFile)
}

# ---------------------------------------------------------------------------
# Clean deploy
# ---------------------------------------------------------------------------
if ($Clean) {
    Write-Host "[WARN]  Clean deploy: removing containers and volumes..." -ForegroundColor Yellow
    & docker compose @ComposeArgs down -v --remove-orphans
    Write-Host "[INFO]  Cleanup complete." -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# Build phase
# ---------------------------------------------------------------------------
if (-not $UpOnly) {
    Write-Host "[DEPLOY] Building Docker images..." -ForegroundColor Cyan
    if ($Service.Count -gt 0) {
        & docker compose @ComposeArgs build @Service
    } else {
        & docker compose @ComposeArgs build
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Build failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "[INFO]  Build complete." -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# Deploy phase
# ---------------------------------------------------------------------------
if (-not $BuildOnly) {
    Write-Host "[DEPLOY] Starting services..." -ForegroundColor Cyan

    $UpArgs = @("up")
    if ($Detach) { $UpArgs += "-d" }

    if ($Service.Count -gt 0) {
        & docker compose @ComposeArgs @UpArgs @Service
    } else {
        & docker compose @ComposeArgs @UpArgs
    }

    if ($Detach) {
        Write-Host ""
        Write-Host "[INFO]  Services started in detached mode." -ForegroundColor Green
        Write-Host ""
        Write-Host "  Service endpoints:"
        Write-Host "    Frontend:            http://localhost:3000"
        Write-Host "    API Gateway:         http://localhost:8080"
        Write-Host "    engine-core:         http://localhost:8081"
        Write-Host "    engine-ingestor:     http://localhost:8082"
        Write-Host "    engine-orchestrator: http://localhost:8095"
        Write-Host "    engine-data:         http://localhost:8100"
        Write-Host "    engine-reporting:    http://localhost:8105"
        Write-Host "    engine-integrations: http://localhost:8110"
        Write-Host ""
        if ($Observability) {
            Write-Host "  Observability:"
            Write-Host "    Grafana:      http://localhost:3001"
            Write-Host "    Prometheus:   http://localhost:9090"
            Write-Host ""
        }
        Write-Host "  Logs:  docker compose -f infra/docker/docker-compose.yml logs -f [service]"
        Write-Host "  Stop:  .\scripts\dev-stop.ps1  OR  docker compose -f infra/docker/docker-compose.yml down"
    }
}
