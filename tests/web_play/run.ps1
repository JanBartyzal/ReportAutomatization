#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Run Playwright UX tests for ReportAutomatization frontend.

.DESCRIPTION
    Requires Node.js 18+ and the frontend running on http://localhost:5173.
    On first run: installs npm dependencies and Playwright browsers.

.PARAMETER Suite
    Which test suite to run. Options: all (default), auth, upload, dashboards,
    notifications, versioning, schema, audit, lifecycle, pptx, forms, filling,
    periods, local, comparison, search, integrations, promotion, sinks, generation, excel, ux

.PARAMETER Headed
    Run tests in headed (visible browser) mode.

.PARAMETER Debug
    Open Playwright inspector for step-by-step debugging.

.PARAMETER Report
    Open the HTML report after the run.

.PARAMETER BaseUrl
    Frontend base URL (default: http://localhost:5173).

.PARAMETER SkipFrontendCheck
    Skip the preflight HTTP check for the frontend base URL.

.EXAMPLE
    .\run.ps1
    .\run.ps1 -Suite lifecycle -Headed
    .\run.ps1 -Suite ux -Report
    .\run.ps1 -BaseUrl "http://myhost:5173"
#>
param(
    [ValidateSet('all','auth','upload','dashboards','notifications','versioning',
                 'schema','audit','lifecycle','pptx','forms','filling','periods',
                 'local','comparison','search','integrations','promotion','sinks','generation','excel','ux')]
    [string]$Suite    = 'all',
    [switch]$Headed,
    [switch]$Debug,
    [switch]$Report,
    [string]$BaseUrl  = 'http://localhost:5173',
    [switch]$SkipFrontendCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot

# ── Check Node.js ────────────────────────────────────────────────────────────
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js is not installed or not in PATH. Please install Node.js 18+."
    exit 1
}

# ── Install dependencies if needed ───────────────────────────────────────────
if (-not (Test-Path (Join-Path $ScriptDir 'node_modules'))) {
    Write-Host "[setup] Installing npm dependencies..." -ForegroundColor Cyan
    Push-Location $ScriptDir
    try {
        npm install
        if ($LASTEXITCODE -ne 0) {
            Write-Error "npm install failed with exit code $LASTEXITCODE. Check network access or install dependencies manually."
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
}

# ── Install Playwright browsers if needed ────────────────────────────────────
$PlaywrightBin = Join-Path (Join-Path (Join-Path $ScriptDir 'node_modules') '.bin') 'playwright'
$isWindowsHost = $PSVersionTable.PSEdition -eq 'Desktop' -or (
    $PSVersionTable.ContainsKey('Platform') -and $PSVersionTable.Platform -eq 'Win32NT'
)
if ($isWindowsHost) {
    $PlaywrightCmd = "$PlaywrightBin.cmd"
    if (Test-Path $PlaywrightCmd) {
        $PlaywrightBin = $PlaywrightCmd
    }
}
if (-not (Test-Path (Join-Path $env:LOCALAPPDATA 'ms-playwright'))) {
    Write-Host "[setup] Installing Playwright browsers..." -ForegroundColor Cyan
    & $PlaywrightBin install chromium
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Playwright browser install failed with exit code $LASTEXITCODE."
        exit $LASTEXITCODE
    }
}

# ── Check frontend availability ──────────────────────────────────────────────
if (-not $SkipFrontendCheck) {
    try {
        $response = Invoke-WebRequest -Uri $BaseUrl -Method Head -TimeoutSec 5 -UseBasicParsing
        if ($response.StatusCode -ge 400) {
            Write-Error "Frontend at $BaseUrl returned HTTP $($response.StatusCode). Start the frontend or pass -BaseUrl."
            exit 1
        }
    } catch {
        Write-Error "Frontend is not reachable at $BaseUrl. Start it with: cd apps/frontend; npm run dev. Use -SkipFrontendCheck to bypass this check."
        exit 1
    }
}

# ── Map suite name to test path ───────────────────────────────────────────────
$suiteMap = @{
    'auth'          = 'tests/FS09_Auth_Navigation'
    'upload'        = 'tests/FS09_File_Upload'
    'dashboards'    = 'tests/FS11_Dashboards'
    'search'        = 'tests/FS12_Search_AI_MCP'
    'notifications' = 'tests/FS13_Notifications'
    'versioning'    = 'tests/FS14_Versioning'
    'schema'        = 'tests/FS15_Schema_Mapping'
    'audit'         = 'tests/FS16_Audit'
    'lifecycle'     = 'tests/FS17_Report_Lifecycle'
    'pptx'          = 'tests/FS18_PPTX_Generation'
    'forms'         = 'tests/FS19_Form_Builder'
    'filling'       = 'tests/FS19_Form_Filling'
    'periods'       = 'tests/FS20_Period_Management'
    'local'         = 'tests/FS21_Local_Scope'
    'comparison'    = 'tests/FS22_Period_Comparison'
    'integrations'  = 'tests/FS23_Integrations'
    'promotion'     = 'tests/FS24_Data_Promotion'
    'sinks'         = 'tests/FS25_Sink_Browser'
    'generation'    = 'tests/FS26_Report_Generation'
    'excel'         = 'tests/FS27_Excel_Sync'
    'ux'            = 'tests/FS99_UX_Quality'
}

$testPath = if ($Suite -eq 'all') { '' } else { $suiteMap[$Suite] }

# ── Build command arguments ───────────────────────────────────────────────────
$args = @()
if ($testPath)   { $args += $testPath }
if ($Headed)     { $args += '--headed' }
if ($Debug)      { $args += '--debug' }

Write-Host ""
Write-Host "=======================================================" -ForegroundColor Blue
Write-Host "  RA Playwright UX Tests" -ForegroundColor Blue
Write-Host "  Suite    : $Suite" -ForegroundColor Blue
Write-Host "  Base URL : $BaseUrl" -ForegroundColor Blue
Write-Host "  Headed   : $Headed" -ForegroundColor Blue
Write-Host "=======================================================" -ForegroundColor Blue
Write-Host ""

Push-Location $ScriptDir
$env:BASE_URL = $BaseUrl

try {
    & $PlaywrightBin test @args
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($Report) {
    Write-Host "[report] Opening HTML report..." -ForegroundColor Cyan
    & $PlaywrightBin show-report logs/html-report
}

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "[OK] All tests passed." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Some tests failed. Check logs/html-report for details." -ForegroundColor Red
}

exit $exitCode
