# ReportAutomatization - Web UI UAT Test Runner
# Run with: python run_all_tests.py

param(
    [switch]
    $SkipInit,

    [switch]
    $Headless
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  ReportAutomatization - Web UI UAT Test Suite" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

# Set environment
$env:WEB_BASE_URL = "http://localhost:5173"
if ($Headless) {
    $env:SELENIUM_HEADLESS = "true"
}

# Check if frontend is running
Write-Host "[CHECK] Checking if frontend is accessible on http://localhost:5173..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "[OK]   Frontend is running on port 5173" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Frontend returned status code: $($response.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARN] Frontend not accessible on port 5173 - tests may fail" -ForegroundColor Yellow
    Write-Host "       Start frontend with: cd apps/frontend && npm run dev" -ForegroundColor Gray
}

Write-Host ""

# Create logs directory
$LogsDir = Join-Path $ScriptDir "logs"
if (-not (Test-Path $LogsDir)) {
    New-Item -ItemType Directory -Path $LogsDir | Out-Null
}

# Run Python tests
Write-Host "[RUN] Starting Web UI UAT tests..." -ForegroundColor Cyan
Write-Host ""

$PythonExe = "python"
if ($PSVersionTable.Platform -eq "Win32NT") {
    # Try python3 first on Windows
    $PythonExe = "python"
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFile = Join-Path $LogsDir "web_uat_$Timestamp.log"

try {
    $process = Start-Process -FilePath $PythonExe `
        -ArgumentList "run_all_tests.py" `
        -WorkingDirectory $ScriptDir `
        -PassThru `
        -NoNewWindow `
        -RedirectStandardOutput $LogFile `
        -RedirectStandardError (Join-Path $LogsDir "web_uat_err_$Timestamp.log")

    $null = $process.WaitForExit()

    # Read and display output
    if (Test-Path $LogFile) {
        Get-Content $LogFile | Select-Object -Last 50
    }

    Write-Host ""
    Write-Host "[RESULT] Test run completed with exit code: $($process.ExitCode)" -ForegroundColor $(if ($process.ExitCode -eq 0) { "Green" } else { "Red" })

} catch {
    Write-Host "[ERROR] Failed to run tests: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Logs saved to: $LogsDir" -ForegroundColor Gray
Write-Host ""

# List log files
Write-Host "Recent log files:" -ForegroundColor Cyan
Get-ChildItem $LogsDir -Filter "*.log" | Sort-Object LastWriteTime -Descending | Select-Object Name, LastWriteTime | Format-Table -AutoSize

Write-Host ""
Write-Host "Recent report files:" -ForegroundColor Cyan
Get-ChildItem $LogsDir -Filter "*report*.md" | Sort-Object LastWriteTime -Descending | Select-Object Name, LastWriteTime | Format-Table -AutoSize

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  Web UI UAT Tests Complete" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
