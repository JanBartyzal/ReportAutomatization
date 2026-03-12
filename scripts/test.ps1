# =============================================================================
# test.ps1 - Run tests for all consolidated services
# =============================================================================
# Usage:
#   .\scripts\test.ps1                        # Run all unit tests
#   .\scripts\test.ps1 -Java                  # Java tests only
#   .\scripts\test.ps1 -Python                # Python tests only
#   .\scripts\test.ps1 -Frontend              # Frontend tests only
#   .\scripts\test.ps1 -Service engine-core   # Specific module
#   .\scripts\test.ps1 -Integration           # Integration tests (Docker required)
#   .\scripts\test.ps1 -E2E                   # E2E tests (stack must be running)
#   .\scripts\test.ps1 -Coverage              # Include coverage reports
# =============================================================================

[CmdletBinding()]
param(
    [string]$Service = "",
    [switch]$Java,
    [switch]$Python,
    [switch]$Frontend,
    [switch]$Integration,
    [switch]$E2E,
    [switch]$Coverage
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path) }

# ---------------------------------------------------------------------------
# Module Definitions
# ---------------------------------------------------------------------------
$JavaModules = @(
    @{ Name = "engine-core";          Path = "apps/engine/engine-core" }
    @{ Name = "engine-ingestor";      Path = "apps/engine/engine-ingestor" }
    @{ Name = "engine-orchestrator";  Path = "apps/engine/engine-orchestrator" }
    @{ Name = "engine-data";          Path = "apps/engine/engine-data" }
    @{ Name = "engine-reporting";     Path = "apps/engine/engine-reporting" }
    @{ Name = "engine-integrations";  Path = "apps/engine/engine-integrations" }
)

$PythonModules = @(
    @{ Name = "processor-atomizers";  Path = "apps/processor/processor-atomizers" }
    @{ Name = "processor-generators"; Path = "apps/processor/processor-generators" }
)

$FrontendDir = "apps/frontend"

# ---------------------------------------------------------------------------
# Results tracking
# ---------------------------------------------------------------------------
$Results = @()

function Add-Result {
    param([string]$Name, [bool]$Passed)
    $script:Results += @{ Name = $Name; Passed = $Passed }
    if ($Passed) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $Name" -ForegroundColor Red
    }
}

# ---------------------------------------------------------------------------
# Java Tests
# ---------------------------------------------------------------------------
function Invoke-JavaTests {
    param([array]$Modules)
    Write-Host "[TEST] Running Java unit tests..." -ForegroundColor Cyan

    foreach ($mod in $Modules) {
        $modPath = Join-Path $ProjectRoot $mod.Path
        if (-not (Test-Path $modPath)) {
            Write-Host "  [WARN] Skipping $($mod.Name): directory not found" -ForegroundColor Yellow
            continue
        }

        Write-Host "  [TEST] Testing $($mod.Name)..." -ForegroundColor Cyan

        $gradleCmd = if ($Coverage) { "test jacocoTestReport" } else { "test" }

        Push-Location $modPath
        try {
            if (Test-Path "gradlew.bat") {
                & .\gradlew.bat $gradleCmd.Split(" ") --no-daemon 2>&1 | Out-Host
            } elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
                & gradle $gradleCmd.Split(" ") --no-daemon 2>&1 | Out-Host
            } else {
                Write-Host "  [WARN] No Gradle wrapper or gradle found" -ForegroundColor Yellow
                Add-Result $mod.Name $false
                continue
            }
            Add-Result $mod.Name ($LASTEXITCODE -eq 0)
        } finally {
            Pop-Location
        }
    }
}

# ---------------------------------------------------------------------------
# Python Tests
# ---------------------------------------------------------------------------
function Invoke-PythonTests {
    param([array]$Modules)
    Write-Host "[TEST] Running Python unit tests..." -ForegroundColor Cyan

    foreach ($mod in $Modules) {
        $modPath = Join-Path $ProjectRoot $mod.Path
        if (-not (Test-Path $modPath)) {
            Write-Host "  [WARN] Skipping $($mod.Name): directory not found" -ForegroundColor Yellow
            continue
        }

        Write-Host "  [TEST] Testing $($mod.Name)..." -ForegroundColor Cyan

        $pytestArgs = @("tests/", "-v", "--tb=short")
        if ($Coverage) {
            $pytestArgs += @("--cov=.", "--cov-report=html", "--cov-report=term")
        }

        Push-Location $modPath
        try {
            & python -m pytest @pytestArgs 2>&1 | Out-Host
            Add-Result $mod.Name ($LASTEXITCODE -eq 0)
        } catch {
            Add-Result $mod.Name $false
        } finally {
            Pop-Location
        }
    }
}

# ---------------------------------------------------------------------------
# Frontend Tests
# ---------------------------------------------------------------------------
function Invoke-FrontendTests {
    Write-Host "[TEST] Running frontend tests..." -ForegroundColor Cyan
    $fePath = Join-Path $ProjectRoot $FrontendDir

    if (-not (Test-Path $fePath)) {
        Write-Host "  [WARN] Frontend directory not found." -ForegroundColor Yellow
        return
    }

    Push-Location $fePath
    try {
        # Install dependencies if needed
        if (-not (Test-Path "node_modules")) {
            Write-Host "  [TEST] Installing dependencies..." -ForegroundColor Cyan
            & npm ci --silent 2>&1 | Out-Host
        }

        # ESLint
        Write-Host "  [TEST] ESLint check..." -ForegroundColor Cyan
        & npx eslint src/ --ext .ts,.tsx --quiet 2>&1 | Out-Host
        Add-Result "frontend-lint" ($LASTEXITCODE -eq 0)

        # TypeScript type check
        Write-Host "  [TEST] TypeScript type check..." -ForegroundColor Cyan
        & npx tsc --noEmit 2>&1 | Out-Host
        Add-Result "frontend-typecheck" ($LASTEXITCODE -eq 0)

        # Vitest
        Write-Host "  [TEST] Vitest unit tests..." -ForegroundColor Cyan
        $vitestArgs = @("vitest", "run")
        if ($Coverage) { $vitestArgs += "--coverage" }
        & npx @vitestArgs 2>&1 | Out-Host
        Add-Result "frontend-unit" ($LASTEXITCODE -eq 0)
    } finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------------------
# Integration Tests
# ---------------------------------------------------------------------------
function Invoke-IntegrationTests {
    Write-Host "[TEST] Running integration tests..." -ForegroundColor Cyan
    $intPath = Join-Path $ProjectRoot "tests/integration"

    if (-not (Test-Path $intPath)) {
        Write-Host "  [WARN] Integration test directory not found." -ForegroundColor Yellow
        return
    }

    try { docker info 2>&1 | Out-Null } catch {
        Write-Host "  [ERROR] Docker required for integration tests." -ForegroundColor Red
        Add-Result "integration" $false
        return
    }

    Push-Location $intPath
    try {
        if (Test-Path "gradlew.bat") {
            & .\gradlew.bat test --no-daemon 2>&1 | Out-Host
        } else {
            & gradle test --no-daemon 2>&1 | Out-Host
        }
        Add-Result "integration" ($LASTEXITCODE -eq 0)
    } finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------------------
# E2E Tests
# ---------------------------------------------------------------------------
function Invoke-E2ETests {
    Write-Host "[TEST] Running E2E tests (Playwright)..." -ForegroundColor Cyan
    $e2ePath = Join-Path $ProjectRoot "tests/e2e"

    if (-not (Test-Path $e2ePath)) {
        Write-Host "  [WARN] E2E test directory not found." -ForegroundColor Yellow
        return
    }

    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8080/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    } catch {
        Write-Host "  [ERROR] Stack must be running for E2E tests." -ForegroundColor Red
        Write-Host "  Run '.\scripts\deploy.ps1 -Detach' first." -ForegroundColor Yellow
        Add-Result "e2e" $false
        return
    }

    Push-Location $e2ePath
    try {
        & npx playwright test 2>&1 | Out-Host
        Add-Result "e2e" ($LASTEXITCODE -eq 0)
    } finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------------------
# Execute
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[INFO]  Test runner starting..." -ForegroundColor Green
Write-Host ""

# Specific module
if ($Service) {
    $found = $false
    $JavaModules | Where-Object { $_.Name -eq $Service } | ForEach-Object {
        Invoke-JavaTests @($_)
        $found = $true
    }
    $PythonModules | Where-Object { $_.Name -eq $Service } | ForEach-Object {
        Invoke-PythonTests @($_)
        $found = $true
    }
    if ($Service -eq "frontend") {
        Invoke-FrontendTests
        $found = $true
    }
    if (-not $found) {
        Write-Host "[ERROR] Unknown module: $Service" -ForegroundColor Red
        exit 1
    }
} else {
    # Default: all unit tests if no specific flag
    $RunAll = -not ($Java -or $Python -or $Frontend -or $Integration -or $E2E)

    if ($Java -or $RunAll)       { Invoke-JavaTests $JavaModules }
    if ($Python -or $RunAll)     { Invoke-PythonTests $PythonModules }
    if ($Frontend -or $RunAll)   { Invoke-FrontendTests }
    if ($Integration)            { Invoke-IntegrationTests }
    if ($E2E)                    { Invoke-E2ETests }
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
$Total = $Results.Count
$Passed = ($Results | Where-Object { $_.Passed }).Count
$FailedModules = $Results | Where-Object { -not $_.Passed }

Write-Host ""
Write-Host "========================================="
Write-Host "  Test Summary"
Write-Host "========================================="
Write-Host "  Total:  $Total"
Write-Host "  Passed: $Passed" -ForegroundColor Green
Write-Host "  Failed: $($FailedModules.Count)" -ForegroundColor $(if ($FailedModules.Count -gt 0) { "Red" } else { "Green" })

if ($FailedModules.Count -gt 0) {
    Write-Host ""
    Write-Host "  Failed modules:" -ForegroundColor Red
    $FailedModules | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Red }
    Write-Host "========================================="
    exit 1
}
Write-Host "========================================="
Write-Host "[INFO]  All tests passed!" -ForegroundColor Green
