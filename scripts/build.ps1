# =============================================================================
# build.ps1 - Build Docker images for all consolidated services
# =============================================================================
# Usage:
#   .\scripts\build.ps1                    # Build all services
#   .\scripts\build.ps1 -Service engine-core  # Build specific service
#   .\scripts\build.ps1 -Java              # Build all Java services
#   .\scripts\build.ps1 -Python            # Build all Python services
#   .\scripts\build.ps1 -Frontend          # Build frontend only
#   .\scripts\build.ps1 -NoCache           # Build without Docker cache
# =============================================================================

[CmdletBinding()]
param(
    [string]$Service = "",
    [switch]$Java,
    [switch]$Python,
    [switch]$Frontend,
    [switch]$NoCache,
    [string]$Tag = "latest",
    [string]$Registry = "reportautomatization"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path) }

# ---------------------------------------------------------------------------
# Service Definitions
# ---------------------------------------------------------------------------
$JavaServices = @(
    @{ Name = "engine-core";          Context = "apps/engine/engine-core";          Dockerfile = "apps/engine/engine-core/Dockerfile" }
    @{ Name = "engine-ingestor";      Context = "apps/engine/engine-ingestor";      Dockerfile = "apps/engine/engine-ingestor/Dockerfile" }
    @{ Name = "engine-orchestrator";  Context = "apps/engine/engine-orchestrator";  Dockerfile = "apps/engine/engine-orchestrator/Dockerfile" }
    @{ Name = "engine-data";          Context = "apps/engine/engine-data";          Dockerfile = "apps/engine/engine-data/Dockerfile" }
    @{ Name = "engine-reporting";     Context = "apps/engine/engine-reporting";     Dockerfile = "apps/engine/engine-reporting/Dockerfile" }
    @{ Name = "engine-integrations";  Context = "apps/engine/engine-integrations";  Dockerfile = "apps/engine/engine-integrations/Dockerfile" }
)

$PythonServices = @(
    @{ Name = "processor-atomizers";  Context = "apps/processor/processor-atomizers";  Dockerfile = "apps/processor/processor-atomizers/Dockerfile" }
    @{ Name = "processor-generators"; Context = "apps/processor/processor-generators"; Dockerfile = "apps/processor/processor-generators/Dockerfile" }
)

$FrontendService = @{ Name = "frontend"; Context = "apps/frontend"; Dockerfile = "apps/frontend/Dockerfile" }

# ---------------------------------------------------------------------------
# Pre-flight
# ---------------------------------------------------------------------------
try {
    docker info 2>&1 | Out-Null
} catch {
    Write-Host "[ERROR] Docker is not running." -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------------
# Determine what to build
# ---------------------------------------------------------------------------
$BuildTargets = @()

if ($Service) {
    $AllServices = $JavaServices + $PythonServices + @($FrontendService)
    $found = $AllServices | Where-Object { $_.Name -eq $Service }
    if (-not $found) {
        Write-Host "[ERROR] Unknown service: $Service" -ForegroundColor Red
        Write-Host "Available: $($AllServices.Name -join ', ')"
        exit 1
    }
    $BuildTargets = @($found)
} elseif ($Java -or $Python -or $Frontend) {
    if ($Java)    { $BuildTargets += $JavaServices }
    if ($Python)  { $BuildTargets += $PythonServices }
    if ($Frontend) { $BuildTargets += $FrontendService }
} else {
    # Build all
    $BuildTargets = $JavaServices + $PythonServices + @($FrontendService)
}

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
$Total = $BuildTargets.Count
$Current = 0
$Failed = @()

Write-Host ""
Write-Host "[INFO]  Building $Total service(s) with tag '$Tag'..." -ForegroundColor Green
Write-Host ""

foreach ($svc in $BuildTargets) {
    $Current++
    $Image = "$Registry/$($svc.Name):$Tag"
    $DockerfilePath = Join-Path $ProjectRoot $svc.Dockerfile
    $ContextPath = Join-Path $ProjectRoot $svc.Context

    Write-Host "[BUILD] [$Current/$Total] $($svc.Name) -> $Image" -ForegroundColor Cyan

    if (-not (Test-Path $DockerfilePath)) {
        Write-Host "[WARN]  Dockerfile not found: $($svc.Dockerfile) - skipping" -ForegroundColor Yellow
        continue
    }

    $dockerArgs = @("build", "-t", $Image, "-f", $DockerfilePath, $ContextPath)
    if ($NoCache) { $dockerArgs = @("build", "--no-cache", "-t", $Image, "-f", $DockerfilePath, $ContextPath) }

    $result = & docker @dockerArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] $($svc.Name) build FAILED" -ForegroundColor Red
        $Failed += $svc.Name
    } else {
        Write-Host "[INFO]  $($svc.Name) built successfully" -ForegroundColor Green
    }
    Write-Host ""
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
Write-Host "========================================="
$Passed = $Total - $Failed.Count
Write-Host "  Total:  $Total"
Write-Host "  Passed: $Passed" -ForegroundColor Green
Write-Host "  Failed: $($Failed.Count)" -ForegroundColor $(if ($Failed.Count -gt 0) { "Red" } else { "Green" })

if ($Failed.Count -gt 0) {
    Write-Host ""
    Write-Host "  Failed services:" -ForegroundColor Red
    $Failed | ForEach-Object { Write-Host "    - $_" -ForegroundColor Red }
    Write-Host "========================================="
    exit 1
}
Write-Host "========================================="
Write-Host "[INFO]  All builds succeeded!" -ForegroundColor Green
