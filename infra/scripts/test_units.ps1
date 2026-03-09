#Requires -Version 5.1
<#
.SYNOPSIS
    Run xUnit tests for all C# microservice units

.DESCRIPTION
    Discovers and runs test projects for all C# microservice units.
    Optionally skips units that failed to build (reads build_results.json).
    Saves structured results to reports/test_results.json for downstream scripts.

.PARAMETER SkipUnits
    Comma-separated list of units to skip

.PARAMETER IncludeFailedBuilds
    Skip units that failed to build (reads build_results.json, default: $true)

.PARAMETER NoBuild
    Pass --no-build to dotnet test (use when build_units.ps1 was run first)

.PARAMETER ReportPath
    Path where reports will be saved (default: ./reports)

.EXAMPLE
    .\test_units.ps1
    .\test_units.ps1 -NoBuild -IncludeFailedBuilds
#>
param(
    [string]$SkipUnits = "ba",
    [switch]$IncludeFailedBuilds,
    [switch]$NoBuild = $false,
    [string]$ReportPath = "./reports"
)

$ErrorActionPreference = "Continue"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot = Join-Path $scriptRoot "units"
$reportDir = Join-Path $scriptRoot $ReportPath
$resultsFile = Join-Path $reportDir "test_results.json"
$buildResultFile = Join-Path $reportDir "build_results.json"

if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }

$skipList = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

# --- Load build results to skip failed builds ------------------------------
$buildStatus = @{}
if (-not $IncludeFailedBuilds -and (Test-Path $buildResultFile)) {
    try {
        $buildData = Get-Content $buildResultFile -Raw | ConvertFrom-Json
        foreach ($u in $buildData.Units) {
            $buildStatus[$u.Name] = $u.Success
        }
        Write-Host "Loaded build results for $($buildStatus.Count) units" -ForegroundColor DarkGray
    }
    catch {
        Write-Host "WARN: Could not read build_results.json: $_" -ForegroundColor Yellow
    }
}

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  CloudInfraMap - Test Units"           -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Units:     $unitsRoot"
Write-Host "Skip:      $(if ($skipList) { $skipList -join ', ' } else { '(none)' })"
Write-Host "NoBuild:   $NoBuild"
Write-Host "SkipFails: $(-not $IncludeFailedBuilds)"
Write-Host ""

$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue
if (-not $units) {
    Write-Host "ERROR: No unit directories found in $unitsRoot" -ForegroundColor Red
    exit 1
}

$unitResults = [System.Collections.Generic.List[object]]::new()
$passCount = 0
$failCount = 0
$skipCount = 0

foreach ($unit in $units) {

    # --- Skip list ----------------------------------------------------------
    if ($unit.Name -in $skipList) {
        Write-Host "  SKIP  $($unit.Name) (excluded)" -ForegroundColor DarkGray
        $skipCount++
        continue
    }

    # --- Detect non-.NET units and skip them gracefully ---------------------
    $unitSrcDir = Join-Path $unit.FullName "src"
    $unitIsPython = (Test-Path (Join-Path $unitSrcDir "main.py")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1))
    $unitIsNodeJs = (Test-Path (Join-Path $unitSrcDir "package.json")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1))

    if ($unitIsPython) {
        Write-Host "  SKIP  $($unit.Name) [Python - use pytest]" -ForegroundColor DarkCyan
        $skipCount++
        continue
    }
    if ($unitIsNodeJs) {
        Write-Host "  SKIP  $($unit.Name) [Node.js - use npm test]" -ForegroundColor DarkCyan
        $skipCount++
        continue
    }

    # --- Skip failed builds -------------------------------------------------
    if (-not $IncludeFailedBuilds -and $buildStatus.ContainsKey($unit.Name) -and -not $buildStatus[$unit.Name]) {
        Write-Host "  SKIP  $($unit.Name) (build failed)" -ForegroundColor DarkGray
        $skipCount++
        $unitResults.Add([PSCustomObject]@{
                Name        = $unit.Name
                CsprojPath  = $null
                Skipped     = $true
                SkipReason  = "Build failed"
                Success     = $false
                TestCount   = 0
                PassedCount = 0
                FailedCount = 0
                Errors      = @("Skipped - build failed")
                Output      = ""
            })
        continue
    }

    # --- Find test .csproj --------------------------------------------------
    # Priority 1: tests/ directory (standard layout), any depth
    $testCsproj = $null
    $testsDir = Join-Path $unit.FullName "tests"
    if (Test-Path $testsDir) {
        $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "\.Tests?\.csproj$" -or $_.DirectoryName -match "[/\\]tests?$" } |
        Select-Object -First 1
        # Fallback: any csproj in tests/ directory
        if (-not $testCsproj) {
            $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1
        }
    }

    # Priority 2: recursive search for test projects anywhere in unit
    if (-not $testCsproj) {
        $testCsproj = Get-ChildItem -Path $unit.FullName -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "\.Tests?\.csproj$" } |
        Select-Object -First 1
    }

    if (-not $testCsproj) {
        Write-Host "  WARN  $($unit.Name) - no test project found" -ForegroundColor Yellow
        $skipCount++
        $unitResults.Add([PSCustomObject]@{
                Name        = $unit.Name
                CsprojPath  = $null
                Skipped     = $true
                SkipReason  = "No test project found"
                Success     = $false
                TestCount   = 0
                PassedCount = 0
                FailedCount = 0
                Errors      = @("No *.Tests.csproj found")
                Output      = ""
            })
        continue
    }

    Write-Host "  TEST  $($unit.Name) [$($testCsproj.Name)]" -ForegroundColor White

    # --- Run dotnet test ----------------------------------------------------
    $dotnetArgs = @(
        "test", $testCsproj.FullName,
        "--configuration", "Release",
        "--logger", "console;verbosity=minimal"
    )
    if ($NoBuild) { $dotnetArgs += "--no-build" }

    $testOutput = & dotnet @dotnetArgs 2>&1
    $exitCode = $LASTEXITCODE

    $passed = 0
    $failed = 0
    $errors = [System.Collections.Generic.List[string]]::new()

    foreach ($line in $testOutput) {
        if ($line -match '(\d+)\s+passed') { $passed = [int]$Matches[1] }
        if ($line -match '(\d+)\s+failed') { $failed = [int]$Matches[1] }
        if ($line -match 'FAILED\s+(\S+)') { $errors.Add("FAILED: $($Matches[1])") }
        # MSBuild errors during compilation inside test run
        if ($line -match '^\s*(?<file>.*)\(\d+,\d+\):\s+error\s+(?<msg>.*)$') {
            $errors.Add("BUILD ERROR: $($Matches.msg.Trim())")
        }
    }

    if ($exitCode -eq 0) {
        Write-Host "        OK ($passed passed)" -ForegroundColor Green
        $passCount++
    }
    else {
        Write-Host "        FAILED ($passed passed, $failed failed)" -ForegroundColor Red
        $failCount++
    }

    $unitResults.Add([PSCustomObject]@{
            Name        = $unit.Name
            CsprojPath  = $testCsproj.FullName
            Skipped     = $false
            SkipReason  = $null
            Success     = ($exitCode -eq 0)
            TestCount   = $passed + $failed
            PassedCount = $passed
            FailedCount = $failed
            Errors      = $errors.ToArray()
            Output      = ($testOutput -join "`n")
        })
}

# --- Save JSON results -----------------------------------------------------
[PSCustomObject]@{
    Timestamp  = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    TotalUnits = $unitResults.Count
    Passed     = $passCount
    Failed     = $failCount
    Skipped    = $skipCount
    Units      = $unitResults.ToArray()
} | ConvertTo-Json -Depth 5 | Out-File -FilePath $resultsFile -Encoding UTF8 -Force

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Test Summary"
Write-Host "  Units:   $($unitResults.Count)"
Write-Host "  Passed:  $passCount"  -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host "  Failed:  $failCount"  -ForegroundColor Red
}
else {
    Write-Host "  Failed:  $failCount"  -ForegroundColor Green
}
Write-Host "  Skipped: $skipCount"  -ForegroundColor Yellow
Write-Host "  Results: $resultsFile"
