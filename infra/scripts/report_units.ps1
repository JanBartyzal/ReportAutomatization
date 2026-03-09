#Requires -Version 5.1
<#
.SYNOPSIS
    Generate markdown reports from build and test results

.DESCRIPTION
    Reads build_results.json and test_results.json from the reports directory
    and generates a master_report.md plus per-unit report files.
    Can also generate a failed_units.json for use by ai_repair_orchestrator.ps1.

.PARAMETER ReportPath
    Path where reports are saved (default: ./reports)

.PARAMETER SkipAiInput
    Generate failed_units.json for ai_repair_orchestrator.ps1 (default: $true)

.EXAMPLE
    .\report_units.ps1
    .\report_units.ps1 -ReportPath "./reports" -SkipAiInput
#>
param(
    [string]$ReportPath      = "./reports",
    [switch]$SkipAiInput
)

$ErrorActionPreference = "Continue"

$scriptRoot      = Split-Path -Parent $MyInvocation.MyCommand.Path
$reportDir       = Join-Path $scriptRoot $ReportPath
$buildResultFile = Join-Path $reportDir "build_results.json"
$testResultFile  = Join-Path $reportDir "test_results.json"
$masterReport    = Join-Path $reportDir "master_report.md"
$failedUnitsFile = Join-Path $reportDir "failed_units.json"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  CloudInfraMap - Generate Reports"    -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# --- Load build results ----------------------------------------------------
$buildData = $null
if (Test-Path $buildResultFile) {
    try {
        $buildData = Get-Content $buildResultFile -Raw | ConvertFrom-Json
        Write-Host "Loaded build results: $($buildData.TotalUnits) units"
    }
    catch {
        Write-Host "WARN: Cannot parse build_results.json: $_" -ForegroundColor Yellow
    }
}
else {
    Write-Host "WARN: build_results.json not found - run build_units.ps1 first" -ForegroundColor Yellow
}

# --- Load test results -----------------------------------------------------
$testData = $null
if (Test-Path $testResultFile) {
    try {
        $testData = Get-Content $testResultFile -Raw | ConvertFrom-Json
        Write-Host "Loaded test results:  $($testData.TotalUnits) units"
    }
    catch {
        Write-Host "WARN: Cannot parse test_results.json: $_" -ForegroundColor Yellow
    }
}
else {
    Write-Host "WARN: test_results.json not found - run test_units.ps1 first" -ForegroundColor Yellow
}

if (-not $buildData -and -not $testData) {
    Write-Host "ERROR: No result files found. Run build_units.ps1 and test_units.ps1 first." -ForegroundColor Red
    exit 1
}

# --- Build lookup tables ---------------------------------------------------
$buildLookup = @{}
if ($buildData) {
    foreach ($u in $buildData.Units) { $buildLookup[$u.Name] = $u }
}

$testLookup = @{}
if ($testData) {
    foreach ($u in $testData.Units) { $testLookup[$u.Name] = $u }
}

# --- Collect all unit names ------------------------------------------------
$allUnitNames = [System.Collections.Generic.HashSet[string]]::new()
if ($buildData) { foreach ($u in $buildData.Units) { $allUnitNames.Add($u.Name) | Out-Null } }
if ($testData)  { foreach ($u in $testData.Units)  { $allUnitNames.Add($u.Name) | Out-Null } }

# --- Compute summary stats -------------------------------------------------
$totalUnits    = $allUnitNames.Count
$buildSuccess  = if ($buildData) { $buildData.Succeeded } else { 0 }
$buildFailed   = if ($buildData) { $buildData.Failed }    else { 0 }
$testPassed    = if ($testData)  { $testData.Passed }     else { 0 }
$testFailed    = if ($testData)  { $testData.Failed }     else { 0 }
$testSkipped   = if ($testData)  { $testData.Skipped }    else { 0 }

$totalTests    = 0
$totalPassed   = 0
$totalFailed   = 0
if ($testData) {
    foreach ($u in $testData.Units) {
        if (-not $u.Skipped) {
            $totalTests  += $u.TestCount
            $totalPassed += $u.PassedCount
            $totalFailed += $u.FailedCount
        }
    }
}

$successRate = if ($totalUnits -gt 0) { [math]::Round(($buildSuccess / $totalUnits) * 100, 1) } else { 0 }
$testRate    = if ($totalTests  -gt 0) { [math]::Round(($totalPassed  / $totalTests)  * 100, 1) } else { 0 }

# --- Helper: format arrays -------------------------------------------------
function Format-List {
    param([object[]]$Items, [string]$None = "(none)")
    if (-not $Items -or $Items.Count -eq 0) { return $None }
    return ($Items | ForEach-Object { "- $_" }) -join "`n"
}

# --- Generate per-unit sections --------------------------------------------
$unitsSection = ""
$failedForAI  = [System.Collections.Generic.List[object]]::new()

foreach ($name in ($allUnitNames | Sort-Object)) {
    $b = $buildLookup[$name]
    $t = $testLookup[$name]

    $buildStatus  = if ($b) { if ($b.Success) { "[OK]" } else { "[FAIL]" } }    else { "[N/A]" }
    $testStatus   = if ($t) { if ($t.Skipped) { "[SKIP] $($t.SkipReason)" } elseif ($t.Success) { "[OK]" } else { "[FAIL]" } } else { "[N/A]" }
    $buildErrors  = if ($b -and $b.Errors) { $b.Errors } else { @() }
    $testErrors   = if ($t -and $t.Errors) { $t.Errors } else { @() }
    $allIssues    = @($buildErrors) + @($testErrors)

    $needsAI = ($b -and -not $b.Success) -or ($t -and -not $t.Skipped -and -not $t.Success)
    if ($needsAI) {
        $failedForAI.Add([PSCustomObject]@{
            Name     = $name
            Build    = $b.Success
            Failures = $allIssues
        })
    }

    $unitsSection += @"

### $name

| Check  | Status |
|--------|--------|
| Build  | $buildStatus |
| Tests  | $testStatus |

"@
    if ($b) {
        $unitsSection += "**Build:** $($b.CsprojPath)`n`n"
        if ($b.Errors -and $b.Errors.Count -gt 0) {
            $unitsSection += "**Build Errors:**`n$(Format-List $b.Errors)`n`n"
        }
    }
    if ($t -and -not $t.Skipped) {
        $unitsSection += "**Tests:** $($t.PassedCount) passed / $($t.FailedCount) failed / $($t.TestCount) total`n`n"
        if ($t.Errors -and $t.Errors.Count -gt 0) {
            $unitsSection += "**Test Failures:**`n$(Format-List $t.Errors)`n`n"
        }
    }
    $unitsSection += "---`n"
}

# --- Lists for summary sections --------------------------------------------
$buildOKList   = ($buildData.Units | Where-Object { $_.Success })     | ForEach-Object { "- $($_.Name)" }
$buildFailList = ($buildData.Units | Where-Object { -not $_.Success }) | ForEach-Object { "- $($_.Name)" }
$testOKList    = ($testData.Units  | Where-Object { -not $_.Skipped -and $_.Success })     | ForEach-Object { "- $($_.Name)" }
$testFailList  = ($testData.Units  | Where-Object { -not $_.Skipped -and -not $_.Success }) | ForEach-Object { "- $($_.Name)" }
$testSkipList  = ($testData.Units  | Where-Object { $_.Skipped })     | ForEach-Object { "- $($_.Name) ($($_.SkipReason))" }

# --- Write master report ---------------------------------------------------
$reportContent = @"
# CloudInfraMap - Master Report

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Build results:** $($buildData.Timestamp)
**Test results:**  $($testData.Timestamp)

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Units | $totalUnits |
| Build Success | $buildSuccess |
| Build Failed  | $buildFailed |
| Tests Passed (units) | $testPassed |
| Tests Failed (units) | $testFailed |
| Tests Skipped (units) | $testSkipped |
| **Build Success Rate** | **$successRate%** |
| **Test Pass Rate** | **$testRate%** |

### Test Coverage

| Metric | Count |
|--------|-------|
| Total test cases | $totalTests |
| Passed | $totalPassed |
| Failed | $totalFailed |

---

## Build Results

### OK - Build Passed ($buildSuccess)
$(if ($buildOKList) { $buildOKList -join "`n" } else { "(none)" })

### FAIL - Build Failed ($buildFailed)
$(if ($buildFailList) { $buildFailList -join "`n" } else { "(none)" })

---

## Test Results

### OK - Tests Passed ($testPassed)
$(if ($testOKList) { $testOKList -join "`n" } else { "(none)" })

### FAIL - Tests Failed ($testFailed)
$(if ($testFailList) { $testFailList -join "`n" } else { "(none)" })

### SKIP - Tests Skipped ($testSkipped)
$(if ($testSkipList) { $testSkipList -join "`n" } else { "(none)" })

---

## Units Requiring AI Repair ($($failedForAI.Count))

$(if ($failedForAI.Count -gt 0) { $failedForAI | ForEach-Object { "- $($_.Name)" } | Out-String } else { "(none - all units healthy)" })

---

## Detailed Unit Reports

$unitsSection

---

## Files

| File | Purpose |
|------|---------|
| build_results.json | Raw build results |
| test_results.json  | Raw test results  |
| failed_units.json  | Input for ai_repair_orchestrator.ps1 |
| master_report.md   | This report |

*Report generated by CloudInfraMap report_units.ps1*
"@

$reportContent | Out-File -FilePath $masterReport -Encoding UTF8 -Force
Write-Host "Master report: $masterReport" -ForegroundColor Green

# --- Per-unit report files -------------------------------------------------
foreach ($name in ($allUnitNames | Sort-Object)) {
    $b = $buildLookup[$name]
    $t = $testLookup[$name]

    $unitFile = Join-Path $reportDir "unit-${name}_report.md"

    $buildSection = if ($b) {
        @"
## Build

- **Status:** $(if ($b.Success) { "OK" } else { "FAILED" })
- **Project:** $($b.CsprojPath)
- **Errors:** $($b.Errors.Count)
- **Warnings:** $($b.Warnings.Count)

$(Format-List $b.Errors "No build errors")
"@
    } else { "## Build`n`nNo build data available." }

    $testSection = if ($t) {
        if ($t.Skipped) {
            "## Tests`n`n**Skipped:** $($t.SkipReason)"
        }
        else {
            @"
## Tests

- **Status:** $(if ($t.Success) { "OK" } else { "FAILED" })
- **Project:** $($t.CsprojPath)
- **Passed:** $($t.PassedCount) / $($t.TestCount)
- **Failed:** $($t.FailedCount)

$(Format-List $t.Errors "No test failures")
"@
        }
    } else { "## Tests`n`nNo test data available." }

    @"
# Unit Report: $name

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

$buildSection

$testSection
"@ | Out-File -FilePath $unitFile -Encoding UTF8 -Force
}
Write-Host "Per-unit reports: $reportDir" -ForegroundColor Green

# --- Generate failed_units.json for AI repair ------------------------------
if (-not $SkipAiInput -and $failedForAI.Count -gt 0) {
    $failedForAI.ToArray() | ConvertTo-Json -Depth 4 | Out-File -FilePath $failedUnitsFile -Encoding UTF8 -Force
    Write-Host "AI repair input:  $failedUnitsFile ($($failedForAI.Count) units)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Report Summary"
Write-Host "  Build OK:    $buildSuccess / $totalUnits"
Write-Host "  Tests OK:    $testPassed unit(s) fully passing"
Write-Host "  Need repair: $($failedForAI.Count) unit(s)" -ForegroundColor $(if ($failedForAI.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "  Report:      $masterReport"
