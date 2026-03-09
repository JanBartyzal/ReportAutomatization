#Requires -Version 5.1
<#
.SYNOPSIS
    CloudInfraMap - Master Batch Workflow Orchestrator

.DESCRIPTION
    Orchestrates the full build -> test -> report -> AI-repair pipeline
    for all C# microservice units. Automatically detects Python and Node.js
    units and routes them to appropriate test runners (pytest / npm test)
    instead of dotnet, preventing false errors.

    Phase 1: .NET  build  (build_units.ps1)
    Phase 2: .NET  tests  (test_units.ps1)
    Phase 3: Python tests (pytest) — if any Python units found
    Phase 4: Node  tests  (npm test) — if any Node.js units found
    Phase 5: Report       (report_units.ps1)
    Phase 6: AI Repair    (ai_repair_orchestrator.ps1) — optional

.PARAMETER SkipUnits
    Comma-separated list of units to skip (all phases)

.PARAMETER ReportPath
    Path where reports will be saved (default: ./reports)

.PARAMETER RunAiRepair
    Invoke ai_repair_orchestrator.ps1 for units that failed

.PARAMETER AiModel
    AI model for repairs (passed to ai_repair_orchestrator.ps1)

.PARAMETER DryRun
    Pass -DryRun to ai_repair_orchestrator.ps1

.PARAMETER SkipPythonTests
    Skip pytest for Python units (pytest required in PATH when not skipped)

.PARAMETER SkipNodeTests
    Skip npm test for Node.js units (npm required in PATH when not skipped)

.EXAMPLE
    .\batch_test_workflow.ps1
    .\batch_test_workflow.ps1 -RunAiRepair
    .\batch_test_workflow.ps1 -SkipUnits "unit-billing-sync-orchestrator,unit-other"
    .\batch_test_workflow.ps1 -SkipPythonTests -SkipNodeTests
#>
param(
    [string]$SkipUnits      = "unit-billing-sync-orchestrator",
    [string]$ReportPath     = "./reports",
    [switch]$RunAiRepair    = $false,
    [string]$AiModel        = "kilo/anthropic/claude-haiku-4.5",
    [switch]$DryRun         = $false,
    [switch]$SkipPythonTests,
    [switch]$SkipNodeTests
)

$ErrorActionPreference = "Continue"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot  = Join-Path $scriptRoot "units"
$reportDir  = Join-Path $scriptRoot $ReportPath
$logFile    = Join-Path $reportDir "batch_workflow.log"

if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }

# ============================================================================
# LOGGING
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet("INFO", "WARN", "ERROR", "SUCCESS", "PHASE")]
        [string]$Level = "INFO"
    )
    $ts    = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $entry = "[$ts] [$Level] $Message"
    Add-Content -Path $logFile -Value $entry -ErrorAction SilentlyContinue

    switch ($Level) {
        "ERROR"   { Write-Host $entry -ForegroundColor Red }
        "WARN"    { Write-Host $entry -ForegroundColor Yellow }
        "SUCCESS" { Write-Host $entry -ForegroundColor Green }
        "PHASE"   { Write-Host $entry -ForegroundColor Magenta }
        default   { Write-Host $entry }
    }
}

# ============================================================================
# UNIT TYPE DETECTION  (fast pre-scan, no dotnet calls)
# ============================================================================

function Get-UnitsByType {
    param([string]$UnitsRoot, [string[]]$ExcludeList)

    $result = @{
        DotNet  = [System.Collections.Generic.List[string]]::new()
        Python  = [System.Collections.Generic.List[string]]::new()
        NodeJS  = [System.Collections.Generic.List[string]]::new()
        Unknown = [System.Collections.Generic.List[string]]::new()
    }

    $dirs = Get-ChildItem -Path $UnitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue
    foreach ($dir in $dirs) {
        if ($dir.Name -in $ExcludeList) { continue }

        $srcPath = Join-Path $dir.FullName "src"

        # Python: main.py
        if ((Test-Path (Join-Path $srcPath "main.py")) -or
            (Get-ChildItem -Path $dir.FullName -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1)) {
            $result.Python.Add($dir.Name)
            continue
        }

        # Node.js: package.json in src/ (not node_modules)
        if ((Test-Path (Join-Path $srcPath "package.json")) -or
            (Get-ChildItem -Path $dir.FullName -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
             Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1)) {
            $result.NodeJS.Add($dir.Name)
            continue
        }

        # .NET: non-test csproj
        $hasCsproj = Get-ChildItem -Path $dir.FullName -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                     Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } | Select-Object -First 1
        if ($hasCsproj) {
            $result.DotNet.Add($dir.Name)
            continue
        }

        $result.Unknown.Add($dir.Name)
    }

    return $result
}

# ============================================================================
# PHASE RUNNER
# ============================================================================

function Invoke-Phase {
    param(
        [string]$Label,
        [string]$Script,
        [string[]]$ScriptArgs
    )

    $scriptPath = Join-Path $scriptRoot $Script
    if (-not (Test-Path $scriptPath)) {
        Write-Log "MISSING SCRIPT: $scriptPath" "ERROR"
        return $false
    }

    Write-Log "" "INFO"
    Write-Log "----------------------------------------" "PHASE"
    Write-Log "PHASE: $Label" "PHASE"
    Write-Log "----------------------------------------" "PHASE"

    $phaseStart = Get-Date
    & $scriptPath @ScriptArgs
    $exitCode = $LASTEXITCODE
    $elapsed  = [math]::Round(((Get-Date) - $phaseStart).TotalSeconds, 1)

    if ($exitCode -eq 0 -or $null -eq $exitCode) {
        Write-Log "Phase '$Label' completed in ${elapsed}s" "SUCCESS"
        return $true
    }
    else {
        Write-Log "Phase '$Label' exited with code $exitCode after ${elapsed}s" "WARN"
        return $false
    }
}

# ============================================================================
# PYTHON TESTS  (pytest)
# ============================================================================

function Invoke-PythonTests {
    param([string[]]$Units)

    Write-Log "" "INFO"
    Write-Log "----------------------------------------" "PHASE"
    Write-Log "PHASE: Python Tests (pytest)" "PHASE"
    Write-Log "----------------------------------------" "PHASE"

    $pytestAvail = Get-Command "pytest" -ErrorAction SilentlyContinue
    if (-not $pytestAvail) {
        Write-Log "pytest not found in PATH - skipping Python tests" "WARN"
        Write-Log "Install with: pip install pytest" "INFO"
        return
    }

    foreach ($unitName in $Units) {
        $unitPath  = Join-Path $unitsRoot $unitName
        $testsPath = Join-Path $unitPath "tests"
        $srcPath   = Join-Path $unitPath "src"

        if (-not (Test-Path $testsPath)) {
            Write-Log "  SKIP  $unitName - no tests/ directory" "WARN"
            continue
        }

        Write-Log "  PYTEST  $unitName" "INFO"

        # Install requirements if present
        $reqFile = Join-Path $srcPath "requirements.txt"
        if (Test-Path $reqFile) {
            Write-Log "  Installing requirements for $unitName..." "INFO"
            & pip install -r $reqFile -q 2>&1 | Out-Null
        }

        $testOutput = & pytest $testsPath -v --tb=short 2>&1
        $exitCode   = $LASTEXITCODE

        $testOutput | ForEach-Object { Write-Log "    $_" "INFO" }

        if ($exitCode -eq 0) {
            Write-Log "  OK  $unitName" "SUCCESS"
        }
        else {
            Write-Log "  FAILED  $unitName (pytest exit $exitCode)" "ERROR"
        }
    }
}

# ============================================================================
# NODE.JS TESTS  (npm test)
# ============================================================================

function Invoke-NodeTests {
    param([string[]]$Units)

    Write-Log "" "INFO"
    Write-Log "----------------------------------------" "PHASE"
    Write-Log "PHASE: Node.js Tests (npm test)" "PHASE"
    Write-Log "----------------------------------------" "PHASE"

    $npmAvail = Get-Command "npm" -ErrorAction SilentlyContinue
    if (-not $npmAvail) {
        Write-Log "npm not found in PATH - skipping Node.js tests" "WARN"
        return
    }

    foreach ($unitName in $Units) {
        $unitPath = Join-Path $unitsRoot $unitName

        # Find package.json
        $pkgJson = Get-ChildItem -Path $unitPath -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
                   Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1

        if (-not $pkgJson) {
            Write-Log "  SKIP  $unitName - no package.json found" "WARN"
            continue
        }

        $pkgDir = $pkgJson.DirectoryName
        Write-Log "  NPM TEST  $unitName  [$($pkgJson.Name)]" "INFO"

        # Install deps if node_modules missing
        if (-not (Test-Path (Join-Path $pkgDir "node_modules"))) {
            Write-Log "  npm install for $unitName..." "INFO"
            & npm install --prefix $pkgDir --silent 2>&1 | Out-Null
        }

        # Check if test script exists in package.json
        $pkg = Get-Content $pkgJson.FullName -Raw | ConvertFrom-Json -ErrorAction SilentlyContinue
        if (-not ($pkg.scripts.test)) {
            Write-Log "  SKIP  $unitName - no 'test' script in package.json" "WARN"
            continue
        }

        $testOutput = & npm test --prefix $pkgDir 2>&1
        $exitCode   = $LASTEXITCODE

        $testOutput | ForEach-Object { Write-Log "    $_" "INFO" }

        if ($exitCode -eq 0) {
            Write-Log "  OK  $unitName" "SUCCESS"
        }
        else {
            Write-Log "  FAILED  $unitName (npm test exit $exitCode)" "ERROR"
        }
    }
}

# ============================================================================
# MAIN
# ============================================================================

$startTime = Get-Date

Write-Log "============================================" "INFO"
Write-Log " CloudInfraMap - Batch Workflow Orchestrator" "INFO"
Write-Log "============================================" "INFO"
Write-Log "Started:        $startTime"
Write-Log "ReportPath:     $reportDir"
Write-Log "SkipUnits:      $SkipUnits"
Write-Log "RunAiRepair:    $RunAiRepair"
Write-Log "SkipPythonTests: $SkipPythonTests"
Write-Log "SkipNodeTests:   $SkipNodeTests"
Write-Log "Log:            $logFile"

# ── Pre-scan: classify all units by type ─────────────────────────────────────
$explicitSkip = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

Write-Log "" "INFO"
Write-Log "Pre-scanning unit types..." "INFO"
$unitsByType = Get-UnitsByType -UnitsRoot $unitsRoot -ExcludeList $explicitSkip

Write-Log "  .NET:    $($unitsByType.DotNet.Count) units" "INFO"
Write-Log "  Python:  $($unitsByType.Python.Count) units  [$($unitsByType.Python -join ', ')]" "INFO"
Write-Log "  Node.js: $($unitsByType.NodeJS.Count) units  [$($unitsByType.NodeJS -join ', ')]" "INFO"
Write-Log "  Unknown: $($unitsByType.Unknown.Count) units  [$($unitsByType.Unknown -join ', ')]" "INFO"

# Build the .NET-only skip list (exclude Python + Node + Unknown from .NET phases)
$nonDotNetUnits = @($unitsByType.Python.ToArray()) + @($unitsByType.NodeJS.ToArray()) + @($unitsByType.Unknown.ToArray())
$dotnetSkip     = ($explicitSkip + $nonDotNetUnits) | Select-Object -Unique
$dotnetSkipStr  = $dotnetSkip -join ","

if ($nonDotNetUnits.Count -gt 0) {
    Write-Log "Auto-skipping non-.NET units from dotnet phases: $($nonDotNetUnits -join ', ')" "INFO"
}

# ── Phase 1: .NET Build ───────────────────────────────────────────────────────
$buildOk = Invoke-Phase -Label ".NET Build" -Script "build_units.ps1" -ScriptArgs @(
    "-SkipUnits",  $dotnetSkipStr,
    "-ReportPath", $ReportPath
)

# ── Phase 2: .NET Tests ───────────────────────────────────────────────────────
$testArgs = [System.Collections.Generic.List[string]]@(
    "-SkipUnits",  $dotnetSkipStr,
    "-ReportPath", $ReportPath
    # -IncludeFailedBuilds omitted on purpose: failed builds are skipped by default
)
if ($buildOk) { $testArgs.Add("-NoBuild") }

Invoke-Phase -Label ".NET Tests" -Script "test_units.ps1" -ScriptArgs $testArgs.ToArray() | Out-Null

# ── Phase 3: Python Tests ─────────────────────────────────────────────────────
if (-not $SkipPythonTests -and $unitsByType.Python.Count -gt 0) {
    Invoke-PythonTests -Units $unitsByType.Python.ToArray()
}
elseif ($unitsByType.Python.Count -gt 0) {
    Write-Log "Python tests skipped (use -SkipPythonTests:$false or omit -SkipPythonTests)" "INFO"
}

# ── Phase 4: Node.js Tests ────────────────────────────────────────────────────
if (-not $SkipNodeTests -and $unitsByType.NodeJS.Count -gt 0) {
    Invoke-NodeTests -Units $unitsByType.NodeJS.ToArray()
}
elseif ($unitsByType.NodeJS.Count -gt 0) {
    Write-Log "Node.js tests skipped (use -SkipNodeTests:$false or omit -SkipNodeTests)" "INFO"
}

# ── Phase 5: Report ───────────────────────────────────────────────────────────
Invoke-Phase -Label "Report" -Script "report_units.ps1" -ScriptArgs @(
    "-ReportPath", $ReportPath
    # -SkipAiInput omitted on purpose: failed_units.json is generated by default
) | Out-Null

# ── Phase 6: AI Repair (optional) ─────────────────────────────────────────────
if ($RunAiRepair) {
    $aiArgs = [System.Collections.Generic.List[string]]@(
        "-ModelName",    $AiModel,
        "-ReportPath",   $ReportPath,
        "-ReadFromJson"  # use failed_units.json from report phase (no re-running tests)
    )
    if ($DryRun) { $aiArgs.Add("-DryRun") }

    Invoke-Phase -Label "AI Repair" -Script "ai_repair_orchestrator.ps1" -ScriptArgs $aiArgs.ToArray() | Out-Null
}
else {
    Write-Log "" "INFO"
    Write-Log "AI Repair skipped. Use -RunAiRepair to enable." "INFO"
    Write-Log "Failed units: $(Join-Path $reportDir 'failed_units.json')" "INFO"
}

# ── Final summary ──────────────────────────────────────────────────────────────
$elapsed = [math]::Round(((Get-Date) - $startTime).TotalMinutes, 2)

Write-Log "" "INFO"
Write-Log "============================================" "INFO"
Write-Log " Workflow Complete in ${elapsed} min"         "SUCCESS"
Write-Log "============================================" "INFO"
Write-Log "Reports:     $reportDir"
Write-Log "Master:      $(Join-Path $reportDir 'master_report.md')"
Write-Log "Build JSON:  $(Join-Path $reportDir 'build_results.json')"
Write-Log "Test JSON:   $(Join-Path $reportDir 'test_results.json')"
Write-Log "AI Input:    $(Join-Path $reportDir 'failed_units.json')"
Write-Log "Log:         $logFile"
