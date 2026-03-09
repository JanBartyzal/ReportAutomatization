#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Runs unit tests with coverage for all microservice units and generates reports.

.DESCRIPTION
    This script discovers all .NET microservice units, runs their tests with coverage
    collection, and generates Test-Result.md files for each unit along with a summary report.

.PARAMETER SkipUnits
    Comma-separated list of unit names to skip.

.PARAMETER IncludeFailedBuilds
    Include units that failed to build in the report.

.PARAMETER NoBuild
    Skip the build step (assumes projects are already built).

.PARAMETER ReportPath
    Output directory for reports. Default is "./reports".

.PARAMETER CoverageThreshold
    Minimum coverage percentage required. Default is 80.0.

.PARAMETER Verbose
    Enable detailed output.

.PARAMETER UnitFilter
    Filter specific units (supports wildcards).

.PARAMETER Parallel
    Run tests in parallel.

.PARAMETER MaxParallelJobs
    Maximum number of parallel test jobs. Default is 4.

.EXAMPLE
    ./test_all_unit_coverage.ps1 -CoverageThreshold 80 -Verbose

.EXAMPLE
    ./test_all_unit_coverage.ps1 -UnitFilter "unit-auth*" -Verbose
#>

param(
    [string]$SkipUnits = "",
    [switch]$IncludeFailedBuilds,
    [switch]$NoBuild,
    [string]$ReportPath = "./reports",
    [double]$CoverageThreshold = 80.0,
    [switch]$Verbose,
    [string]$UnitFilter = "",
    [switch]$Parallel,
    [int]$MaxParallelJobs = 4
)

$ErrorActionPreference = "Continue"

# Colors for output
function Write-Info { param($msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Success { param($msg) Write-Host "[PASS] $msg" -ForegroundColor Green }
function Write-Warning { param($msg) Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host "[FAIL] $msg" -ForegroundColor Red }

# Parse skip units
$skipList = @()
if ($SkipUnits) {
    $skipList = $SkipUnits -split ',' | ForEach-Object { $_.Trim() }
}

# Ensure report directory exists
$reportDir = Join-Path $PSScriptRoot $ReportPath
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}

# Results tracking
$results = @{
    TotalUnits          = 0
    UnitsTested         = 0
    UnitsPassed         = 0
    UnitsBelowThreshold = 0
    UnitsWithoutTests   = 0
    UnitsFailedBuild    = 0
    CoverageData        = @()
}

# Function to get unit info
function Get-UnitInfo {
    param($unitPath)
    
    $unitName = Split-Path $unitPath -Leaf
    $srcPath = Join-Path $unitPath "src"
    $testsPath = Join-Path $unitPath "tests"
    
    # Find csproj files
    $srcCsproj = Get-ChildItem -Path $srcPath -Filter "*.csproj" -ErrorAction SilentlyContinue | Select-Object -First 1
    $testsCsproj = Get-ChildItem -Path $testsPath -Filter "*.Tests.csproj" -ErrorAction SilentlyContinue | Select-Object -First 1
    
    return @{
        Name         = $unitName
        Path         = $unitPath
        SrcProject   = $srcCsproj
        TestsProject = $testsCsproj
        HasTests     = $null -ne $testsCsproj
    }
}

# Function to run tests and collect coverage
function Invoke-UnitTest {
    param($unitInfo)
    
    $testResultPath = Join-Path $unitInfo.Path "tests/TestResults"
    
    # Clean previous results
    if (Test-Path $testResultPath) {
        Remove-Item -Path $testResultPath -Recurse -Force
    }
    
    # Run dotnet test with coverage
    $testArgs = @(
        "test",
        $unitInfo.TestsProject.FullName,
        "--configuration", "Release",
        "--no-build",
        "--collect:", "XPlat Code Coverage",
        "--results-directory:", $testResultPath,
        "--logger:", "trx"
    )
    
    if ($Verbose) {
        $testArgs += "--verbosity"
        $testArgs += "detailed"
    }
    
    try {
        $output = & dotnet $testArgs 2>&1
        $exitCode = $LASTEXITCODE
        
        # Find coverage file
        $coverageFile = Get-ChildItem -Path $testResultPath -Filter "coverage.cobertura.xml" -Recurse | Select-Object -First 1
        
        return @{
            Success      = $exitCode -eq 0
            CoverageFile = $coverageFile
            Output       = $output
        }
    }
    catch {
        return @{
            Success      = $false
            CoverageFile = $null
            Output       = $_.Exception.Message
        }
    }
}

# Function to parse coverage from Cobertura XML
function Get-CoverageFromCobertura {
    param($coverageFile)
    
    if (-not $coverageFile -or -not (Test-Path $coverageFile)) {
        return $null
    }
    
    try {
        [xml]$xml = Get-Content $coverageFile
        $coverage = $xml.coverage
        
        $lineRate = [double]$coverage.'line-rate' * 100
        $branchRate = [double]$coverage.'branch-rate' * 100
        
        # Get package-level coverage
        $packages = @()
        foreach ($pkg in $coverage.packages.package) {
            $packages += @{
                Name       = $pkg.name
                LineRate   = [double]$pkg.'line-rate' * 100
                BranchRate = [double]$pkg.'branch-rate' * 100
            }
        }
        
        return @{
            LineRate   = [math]::Round($lineRate, 2)
            BranchRate = [math]::Round($branchRate, 2)
            Packages   = $packages
        }
    }
    catch {
        Write-Warning "Failed to parse coverage file: $_"
        return $null
    }
}

# Function to generate Test-Result.md
function New-TestResultMarkdown {
    param(
        $unitInfo,
        $testResult,
        $coverageData
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss UTC"
    $status = if ($testResult.Success -and $coverageData.LineRate -ge $CoverageThreshold) { "PASS" } else { "FAIL" }
    $coverageStatus = if ($coverageData.LineRate -ge $CoverageThreshold) { "✅ Meets threshold" } else { "⚠️ Below threshold" }
    
    $md = @"
# Test Results: $($unitInfo.Name)

## Summary

| Metric | Value |
|--------|-------|
| **Unit Name** | $($unitInfo.Name) |
| **Test Date** | $timestamp |
| **Overall Result** | $status |
| **Coverage** | $($coverageData.LineRate)% |
| **Coverage Status** | $coverageStatus |

## Code Coverage

### Overall Coverage

| Metric | Coverage |
|--------|----------|
| Line Coverage | $($coverageData.LineRate)% |
| Branch Coverage | $($coverageData.BranchRate)% |

### Coverage by Module

| Module | Line Coverage | Branch Coverage |
|--------|---------------|-----------------|
"@

    foreach ($pkg in $coverageData.Packages) {
        $md += "`n| $($pkg.Name) | $($pkg.LineRate)% | $($pkg.BranchRate)% |"
    }

    $md += @"

## Test Environment

- **Framework**: xUnit 2.6.4
- **Coverage Tool**: coverlet 6.0.2
- **Target Framework**: .NET 10.0
- **Configuration**: Release

## History

| Date | Coverage | Result |
|------|----------|--------|
| $timestamp | $($coverageData.LineRate)% | $status |
"@

    $resultPath = Join-Path $unitInfo.Path "tests/Test-Result.md"
    $md | Out-File -FilePath $resultPath -Encoding UTF8
    
    return $resultPath
}

# Function to generate summary report
function New-SummaryReport {
    param($coverageData)
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss UTC"
    
    # Calculate distribution
    $dist0_20 = ($coverageData | Where-Object { $_.Coverage -lt 20 }).Count
    $dist20_40 = ($coverageData | Where-Object { $_.Coverage -ge 20 -and $_.Coverage -lt 40 }).Count
    $dist40_60 = ($coverageData | Where-Object { $_.Coverage -ge 40 -and $_.Coverage -lt 60 }).Count
    $dist60_80 = ($coverageData | Where-Object { $_.Coverage -ge 60 -and $_.Coverage -lt 80 }).Count
    $dist80_100 = ($coverageData | Where-Object { $_.Coverage -ge 80 }).Count
    
    $avgCoverage = if ($coverageData.Count -gt 0) {
        [math]::Round(($coverageData | Measure-Object -Property Coverage -Average).Average, 2)
    }
    else { 0 }
    
    $md = @"
# Microservices Test Coverage Summary

**Generated**: $timestamp
**Threshold**: $CoverageThreshold%

## Overview

| Metric | Value |
|--------|-------|
| Total Units | $($results.TotalUnits) |
| Units Tested | $($results.UnitsTested) |
| Units Passing | $($results.UnitsPassed) |
| Units Below Threshold | $($results.UnitsBelowThreshold) |
| Units Without Tests | $($results.UnitsWithoutTests) |
| Average Coverage | $avgCoverage% |

## Coverage Distribution

```
0-20%   : $dist0_20 units $(if ($dist0_20 -gt 0) { "█" * [math]::Min($dist0_20, 20) })
20-40%  : $dist20_40 units $(if ($dist20_40 -gt 0) { "█" * [math]::Min($dist20_40, 20) })
40-60%  : $dist40_60 units $(if ($dist40_60 -gt 0) { "█" * [math]::Min($dist40_60, 20) })
60-80%  : $dist60_80 units $(if ($dist60_80 -gt 0) { "█" * [math]::Min($dist60_80, 20) })
80-100% : $dist80_100 units $(if ($dist80_100 -gt 0) { "█" * [math]::Min($dist80_100, 20) })
```

## Units Requiring Attention

### Below Threshold (Sorted by Coverage)

| Unit | Coverage | Gap |
|------|----------|-----|
"@

    $belowThreshold = $coverageData | Where-Object { $_.Coverage -lt $CoverageThreshold } | Sort-Object Coverage
    foreach ($item in $belowThreshold) {
        $gap = [math]::Round($CoverageThreshold - $item.Coverage, 2)
        $md += "`n| $($item.Unit) | $($item.Coverage)% | $gap% |"
    }

    $md += @"

## Units Meeting Threshold

| Unit | Coverage |
|------|----------|
"@

    $aboveThreshold = $coverageData | Where-Object { $_.Coverage -ge $CoverageThreshold } | Sort-Object Coverage -Descending
    foreach ($item in $aboveThreshold) {
        $md += "`n| $($item.Unit) | $($item.Coverage)% |"
    }

    $md += "`n`n---`n*Generated by test_all_unit_coverage.ps1*"

    $summaryPath = Join-Path $reportDir "coverage_summary.md"
    $md | Out-File -FilePath $summaryPath -Encoding UTF8
    
    # Also generate JSON
    $jsonPath = Join-Path $reportDir "coverage_summary.json"
    $results.CoverageData = $coverageData
    $results | ConvertTo-Json -Depth 10 | Out-File -FilePath $jsonPath -Encoding UTF8
    
    return $summaryPath
}

# Main execution
Write-Info "Starting test coverage analysis..."
Write-Info "Coverage threshold: $CoverageThreshold%"
Write-Info "Report path: $reportDir"

# Discover all units
$unitsPath = Join-Path $PSScriptRoot "units"
$units = Get-ChildItem -Path $unitsPath -Directory | Where-Object { $_.Name -like "unit-*" }

# Apply filter
if ($UnitFilter) {
    $units = $units | Where-Object { $_.Name -like $UnitFilter }
}

# Apply skip list
if ($skipList.Count -gt 0) {
    $units = $units | Where-Object { $_.Name -notin $skipList }
}

$results.TotalUnits = $units.Count
Write-Info "Found $($units.Count) units to process"

# Build all projects first (if not skipped)
if (-not $NoBuild) {
    Write-Info "Building all projects..."
    
    foreach ($unit in $units) {
        $unitInfo = Get-UnitInfo -unitPath $unit.FullName
        
        if ($unitInfo.HasTests) {
            if ($Verbose) {
                Write-Info "Building $($unitInfo.Name)..."
            }
            
            $buildOutput = & dotnet build $unitInfo.TestsProject.FullName --configuration Release 2>&1
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Build failed for $($unitInfo.Name)"
                $results.UnitsFailedBuild++
                if (-not $IncludeFailedBuilds) {
                    continue
                }
            }
        }
    }
}

# Run tests for each unit
foreach ($unit in $units) {
    $unitInfo = Get-UnitInfo -unitPath $unit.FullName
    
    if (-not $unitInfo.HasTests) {
        Write-Warning "No tests found for $($unitInfo.Name)"
        $results.UnitsWithoutTests++
        continue
    }
    
    Write-Info "Testing $($unitInfo.Name)..."
    
    $testResult = Invoke-UnitTest -unitInfo $unitInfo
    $coverageData = Get-CoverageFromCobertura -coverageFile $testResult.CoverageFile
    
    if ($null -eq $coverageData) {
        $coverageData = @{
            LineRate   = 0
            BranchRate = 0
            Packages   = @()
        }
    }
    
    $results.UnitsTested++
    
    if ($testResult.Success) {
        if ($coverageData.LineRate -ge $CoverageThreshold) {
            Write-Success "$($unitInfo.Name): $($coverageData.LineRate)% coverage"
            $results.UnitsPassed++
        }
        else {
            Write-Warning "$($unitInfo.Name): $($coverageData.LineRate)% coverage (below threshold)"
            $results.UnitsBelowThreshold++
        }
    }
    else {
        Write-Error "$($unitInfo.Name): Tests failed"
        $results.UnitsBelowThreshold++
    }
    
    # Generate Test-Result.md
    $resultPath = New-TestResultMarkdown -unitInfo $unitInfo -testResult $testResult -coverageData $coverageData
    if ($Verbose) {
        Write-Info "Generated: $resultPath"
    }
    
    # Track for summary
    $results.CoverageData += @{
        Unit           = $unitInfo.Name
        Coverage       = $coverageData.LineRate
        BranchCoverage = $coverageData.BranchRate
        Passed         = $testResult.Success -and $coverageData.LineRate -ge $CoverageThreshold
    }
}

# Generate summary report
$summaryPath = New-SummaryReport -coverageData $results.CoverageData

Write-Host ""
Write-Info "=== Summary ==="
Write-Info "Total Units: $($results.TotalUnits)"
Write-Info "Units Tested: $($results.UnitsTested)"
Write-Success "Units Passing: $($results.UnitsPassed)"
Write-Warning "Units Below Threshold: $($results.UnitsBelowThreshold)"
Write-Warning "Units Without Tests: $($results.UnitsWithoutTests)"
Write-Info "Summary report: $summaryPath"

# Exit with appropriate code
if ($results.UnitsBelowThreshold -gt 0) {
    exit 1
}
exit 0
