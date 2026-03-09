#Requires -Version 5.1
<#
.SYNOPSIS
    AI-Powered Autonomous Microservice Repair Orchestrator

.DESCRIPTION
    Orchestrates unit test failure detection and autonomous repair via kilocode CLI.
    Supports --yolo mode (auto-accept all actions) and streams live kilocode output
    to the console so you can see exactly what the AI is doing.

.PARAMETER ModelName
    AI model to use (default: "kilo/anthropic/claude-haiku-4.5")

.PARAMETER UnitName
    Specific unit to repair. If omitted, reads from failed_units.json (when -ReadFromJson)
    or scans all units for test failures.

.PARAMETER MaxConcurrent
    Maximum number of concurrent AI repair tasks (default: 2)

.PARAMETER DryRun
    Show what would be done without executing kilocode

.PARAMETER YoloMode
    Pass --yolo to kilocode to auto-accept all actions (default: $true).
    Prevents kilocode from blocking on yes/no prompts.

.PARAMETER AutoAcceptFlag
    The CLI flag used for auto-accept (default: "--yolo").
    Change if your kilocode version uses a different flag.

.PARAMETER TimeoutMinutes
    Timeout per unit repair in minutes (default: 10)

.PARAMETER ReportPath
    Directory containing build_results.json / test_results.json / failed_units.json.
    Defaults to ./reports

.PARAMETER ReadFromJson
    Read failed units from reports/failed_units.json instead of running tests.
    Use this when called from batch_test_workflow.ps1 (report_units.ps1 already ran).

.EXAMPLE
    # Repair all failed units (auto-scan tests)
    .\ai_repair_orchestrator.ps1

.EXAMPLE
    # Repair from JSON produced by report_units.ps1
    .\ai_repair_orchestrator.ps1 -ReadFromJson

.EXAMPLE
    # Repair specific unit, custom model, dry-run
    .\ai_repair_orchestrator.ps1 -UnitName "unit-architecture-analyzer" -ModelName "anthropic/claude-opus-4" -DryRun

.EXAMPLE
    # Disable yolo mode (kilocode will ask for confirmations)
    .\ai_repair_orchestrator.ps1 -YoloMode:$false
#>
param(
    [string]$ModelName       = "kilo/anthropic/claude-haiku-4.5",
    [string]$UnitName        = $null,
    [int]$MaxConcurrent      = 2,
    [switch]$DryRun          = $false,
    [switch]$NoYolo,
    [string]$AutoAcceptFlag  = "--yolo",
    [int]$TimeoutMinutes     = 10,
    [string]$ReportPath      = "./reports",
    [switch]$ReadFromJson    = $false
)

# ============================================================================
# CONFIGURATION & INITIALIZATION
# ============================================================================

$ErrorActionPreference = "Continue"
$WarningPreference     = "Continue"

# Resolved once at script level so all functions and report templates can use it
$yoloActive = -not $NoYolo

$scriptRoot    = Split-Path -Parent $MyInvocation.MyCommand.Path
$unitsRoot     = Join-Path $scriptRoot "units"
$logsDir       = Join-Path $scriptRoot "logs"
$reportsDir    = Join-Path $scriptRoot $ReportPath
$templateFile  = Join-Path $scriptRoot "ai_repair_prompt_template.txt"

@($logsDir, $reportsDir) | ForEach-Object {
    if (-not (Test-Path $_)) { New-Item -ItemType Directory -Path $_ -Force | Out-Null }
}

$runTimestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile      = Join-Path $logsDir  "ai_repair_${runTimestamp}.log"
$reportFile   = Join-Path $reportsDir "ai_repair_report_${runTimestamp}.md"

$script:repairResults          = @()
$script:failedUnits            = @()
$script:repairedUnits          = @()
$script:manualInterventionUnits = @()

# ============================================================================
# LOGGING
# ============================================================================

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet("INFO", "WARN", "ERROR", "SUCCESS")]
        [string]$Level = "INFO"
    )

    $ts    = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $entry = "[$ts] [$Level] $Message"

    Add-Content -Path $logFile -Value $entry -ErrorAction SilentlyContinue
    Write-Host $entry
}

function Write-Section {
    param([string]$Title)
    $sep = "=" * 80
    Write-Log $sep
    Write-Log $Title
    Write-Log $sep
}

# ============================================================================
# TEMPLATE / PROMPT HELPERS
# ============================================================================

function Get-TemplateContent {
    if (-not (Test-Path $templateFile)) {
        Write-Log "Template file not found: $templateFile" "ERROR"
        return $null
    }
    return Get-Content -Path $templateFile -Raw
}

function New-RepairPrompt {
    param(
        [string]$RepairUnitName,
        [string[]]$Failures
    )

    $template = Get-TemplateContent
    if (-not $template) { return $null }

    $prompt = $template -replace "{{UNIT_NAME}}", $RepairUnitName

    $failureContext = "`n`n## TEST FAILURES DETECTED`nThe following failures were detected in ${RepairUnitName}:`n`n"
    foreach ($f in $Failures) { $failureContext += "- $f`n" }

    return $prompt + $failureContext
}

# ============================================================================
# KILOCODE STREAMING EXECUTION
# ============================================================================

function Invoke-KilcodeRepair {
    param(
        [string]$RepairUnitName,
        [string]$Prompt,
        [string]$Model
    )

    $modeTag = if ($yoloActive) { "yolo" } else { "interactive" }

    Write-Log "Starting kilocode repair: $RepairUnitName  [model=$Model] [mode=$modeTag]" "INFO"

    if ($DryRun) {
        Write-Log "[DRY RUN] kilocode run <prompt> --model $Model $(if ($yoloActive) { $AutoAcceptFlag })" "INFO"
        return [PSCustomObject]@{ Success = $true; TaskId = "DRY_RUN_$RepairUnitName"; Output = "Dry run - no action taken" }
    }

    try {
        # Build argument array
        $kilArgs = [System.Collections.Generic.List[string]]@("run", $Prompt, "--model", $Model)
        if ($yoloActive -and $AutoAcceptFlag) { $kilArgs.Add($AutoAcceptFlag) }

        Write-Host ""
        Write-Host "  +-----------------------------------------------------" -ForegroundColor DarkCyan
        Write-Host "  | kilocode > $RepairUnitName" -ForegroundColor Cyan
        Write-Host "  | Command : kilocode $($kilArgs -join ' ')" -ForegroundColor DarkGray
        Write-Host "  | Timeout : $TimeoutMinutes min  | Yolo: $yoloActive" -ForegroundColor DarkGray
        Write-Host "  +-----------------------------------------------------" -ForegroundColor DarkCyan

        # --- Set up Process with streaming output --------------------------
        $psi = [System.Diagnostics.ProcessStartInfo]::new()
        $psi.FileName  = "kilocode"
        # Build argument string (quote items that contain spaces)
        $psi.Arguments = ($kilArgs | ForEach-Object {
            if ($_ -match '[\s"<>&|]') { "`"$($_ -replace '"', '\"')`"" } else { $_ }
        }) -join " "
        $psi.UseShellExecute        = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError  = $true
        $psi.CreateNoWindow         = $false  # keep window visible if launched

        $process = [System.Diagnostics.Process]::new()
        $process.StartInfo           = $psi
        $process.EnableRaisingEvents = $true

        # Collect output for report (thread-safe)
        $script:kilcodeLines = [System.Collections.Concurrent.ConcurrentQueue[string]]::new()

        # Register async output/error handlers
        $outSub = Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
            $line = $Event.SourceEventArgs.Data
            if ($null -ne $line) {
                $line | Out-Null   # capture reference
                Write-Host "  | $line" -ForegroundColor Cyan
                $script:kilcodeLines.Enqueue($line)
            }
        }

        $errSub = Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
            $line = $Event.SourceEventArgs.Data
            if ($null -ne $line) {
                Write-Host "  | [STDERR] $line" -ForegroundColor Yellow
                $script:kilcodeLines.Enqueue("STDERR: $line")
            }
        }

        # Start process
        $process.Start()          | Out-Null
        $process.BeginOutputReadLine()
        $process.BeginErrorReadLine()

        Write-Log "kilocode PID $($process.Id) started" "INFO"

        # --- Poll loop - allows PowerShell event dispatcher to fire --------
        $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
        while (-not $process.HasExited) {
            if ((Get-Date) -gt $deadline) {
                Write-Log "Timeout ($TimeoutMinutes min) reached for $RepairUnitName - killing process" "WARN"
                $process.Kill()
                break
            }
            Start-Sleep -Milliseconds 300   # yields to event dispatcher
        }

        # Wait for async reads to flush
        $process.WaitForExit(5000) | Out-Null

        # Cleanup event subscriptions
        $outSub | ForEach-Object { Unregister-Event -SourceIdentifier $_.Name -ErrorAction SilentlyContinue; Remove-Job -Id $_.Id -Force -ErrorAction SilentlyContinue }
        $errSub | ForEach-Object { Unregister-Event -SourceIdentifier $_.Name -ErrorAction SilentlyContinue; Remove-Job -Id $_.Id -Force -ErrorAction SilentlyContinue }

        $exitCode   = $process.ExitCode
        $allOutput  = [string[]]$script:kilcodeLines.ToArray()

        Write-Host "  +-----------------------------------------------------" -ForegroundColor DarkCyan
        Write-Host ""

        if (-not $process.HasExited) {
            # still not exited = timed out
            return [PSCustomObject]@{ Success = $false; TaskId = $null; Output = "Timed out after $TimeoutMinutes minutes" }
        }

        if ($exitCode -eq 0) {
            Write-Log "kilocode succeeded for $RepairUnitName (exit 0)" "SUCCESS"
            return [PSCustomObject]@{ Success = $true; TaskId = "TASK_$RepairUnitName"; Output = ($allOutput -join "`n") }
        }
        else {
            Write-Log "kilocode failed for $RepairUnitName (exit $exitCode)" "ERROR"
            return [PSCustomObject]@{ Success = $false; TaskId = $null; Output = ($allOutput -join "`n") }
        }
    }
    catch {
        Write-Log "Exception in Invoke-KilcodeRepair: $_" "ERROR"
        return [PSCustomObject]@{ Success = $false; TaskId = $null; Output = $_.Exception.Message }
    }
}

# ============================================================================
# FAILED UNIT DISCOVERY
# ============================================================================

function Get-FailedUnitsFromJson {
    <# Reads failed_units.json produced by report_units.ps1 #>
    $jsonPath = Join-Path $reportsDir "failed_units.json"
    if (-not (Test-Path $jsonPath)) {
        Write-Log "failed_units.json not found at $jsonPath  - run report_units.ps1 first" "WARN"
        return @()
    }

    try {
        $data = Get-Content $jsonPath -Raw | ConvertFrom-Json
        $result = @()
        foreach ($item in $data) {
            $result += @{
                Name     = $item.Name
                Path     = Join-Path $unitsRoot $item.Name
                Failures = if ($item.Failures) { $item.Failures } else { @() }
            }
        }
        Write-Log "Loaded $($result.Count) failed unit(s) from $jsonPath" "INFO"
        return $result
    }
    catch {
        Write-Log "Cannot parse failed_units.json: $_" "ERROR"
        return @()
    }
}

function Get-FailedUnitsByScan {
    <# Scans units by actually running dotnet test #>
    Write-Section "SCANNING FOR FAILED UNITS"
    $failedUnits = @()

    if ($UnitName) {
        Write-Log "Testing specific unit: $UnitName"
        $unitPath  = Join-Path $unitsRoot $UnitName
        if (-not (Test-Path $unitPath)) {
            Write-Log "Unit path not found: $unitPath" "WARN"
            return @()
        }
        $testResult = Test-Unit -UnitPath $unitPath -TestUnitName $UnitName
        if (-not $testResult.Success) {
            $failedUnits += @{ Name = $UnitName; Path = $unitPath; Failures = $testResult.Failures }
        }
    }
    else {
        Write-Log "Scanning all units for test failures..."
        $units = Get-ChildItem -Path $unitsRoot -Directory -ErrorAction SilentlyContinue
        foreach ($unit in $units) {
            $testResult = Test-Unit -UnitPath $unit.FullName -TestUnitName $unit.Name
            if (-not $testResult.Success) {
                $failedUnits += @{ Name = $unit.Name; Path = $unit.FullName; Failures = $testResult.Failures }
                Write-Log "Failures found: $($unit.Name)" "WARN"
            }
        }
    }

    Write-Log "Found $($failedUnits.Count) unit(s) with failures"
    return $failedUnits
}

function Test-Unit {
    param(
        [string]$UnitPath,
        [string]$TestUnitName
    )

    # Find test csproj recursively in tests/ directory
    $testProj = Get-ChildItem -Path (Join-Path $UnitPath "tests") -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                Select-Object -First 1

    if (-not $testProj) {
        Write-Log "No test project found for $TestUnitName" "WARN"
        return @{ Success = $true; Failures = @() }
    }

    Write-Log "Running tests: $TestUnitName"
    $testOutput  = & dotnet test $testProj.FullName --logger "console;verbosity=minimal" 2>&1
    $testExitCode = $LASTEXITCODE

    if ($testExitCode -ne 0) {
        $failures = $testOutput | Where-Object { $_ -match 'FAILED\s+|Error:\s+|Exception:\s+' } |
                    ForEach-Object { $_.ToString().Trim() } | Select-Object -First 20
        Write-Log "Tests failed: $TestUnitName  ($($failures.Count) failure line(s))" "ERROR"
        return @{ Success = $false; Failures = $failures }
    }

    Write-Log "Tests passed: $TestUnitName" "SUCCESS"
    return @{ Success = $true; Failures = @() }
}

# ============================================================================
# REPAIR QUEUE
# ============================================================================

function Start-RepairQueue {
    param([array]$FailedUnits)

    Write-Section "PROCESSING REPAIR QUEUE"

    if ($FailedUnits.Count -eq 0) {
        Write-Log "No failed units to repair"
        return
    }

    Write-Log "Units to repair: $($FailedUnits.Count)  [max concurrency: $MaxConcurrent]"

    # Sequential processing (kilocode CLI is not safely parallelizable via simple jobs)
    foreach ($unit in $FailedUnits) {
        Write-Log "─── Repairing: $($unit.Name) ───" "INFO"

        $prompt = New-RepairPrompt -RepairUnitName $unit.Name -Failures $unit.Failures
        if (-not $prompt) {
            Write-Log "Failed to build repair prompt for $($unit.Name)" "ERROR"
            $script:manualInterventionUnits += $unit.Name
            continue
        }

        $result = Invoke-KilcodeRepair -RepairUnitName $unit.Name -Prompt $prompt -Model $ModelName

        $script:repairResults += [PSCustomObject]@{
            Unit      = $unit.Name
            Result    = $result
            Timestamp = Get-Date
        }

        if ($result.Success) {
            $script:repairedUnits += $unit.Name
            Write-Log "Repair queued successfully: $($unit.Name)" "SUCCESS"
        }
        else {
            $script:manualInterventionUnits += $unit.Name
            Write-Log "Repair failed, manual review needed: $($unit.Name)" "WARN"
        }
    }

    Write-Log "Repair queue complete"
}

# ============================================================================
# REPORT
# ============================================================================

function Save-Report {
    Write-Section "GENERATING REPORT"

    $content = @"
# AI Repair Orchestrator Report

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Configuration

| Setting | Value |
|---------|-------|
| Model | $ModelName |
| Yolo Mode | $yoloActive |
| Auto-accept Flag | $AutoAcceptFlag |
| Timeout (min) | $TimeoutMinutes |
| Max Concurrent | $MaxConcurrent |
| Dry Run | $DryRun |
| Source | $(if ($ReadFromJson) { "failed_units.json" } elseif ($UnitName) { $UnitName } else { "test scan" }) |

## Summary

| Metric | Count |
|--------|-------|
| Failed units found | $($script:failedUnits.Count) |
| Successfully dispatched | $($script:repairedUnits.Count) |
| Manual intervention needed | $($script:manualInterventionUnits.Count) |

## Dispatched to kilocode

$(if ($script:repairedUnits.Count -gt 0) { $script:repairedUnits | ForEach-Object { "- OK  $_" } | Out-String } else { "(none)" })

## Needs Manual Review

$(if ($script:manualInterventionUnits.Count -gt 0) { $script:manualInterventionUnits | ForEach-Object { "- WARN  $_" } | Out-String } else { "(none)" })

## Detailed Results

"@

    foreach ($r in $script:repairResults) {
        $status = if ($r.Result.Success) { "SUCCESS" } else { "FAILED" }
        $content += @"
### $($r.Unit) — $status

- **Timestamp:** $($r.Timestamp)
- **Task ID:** $($r.Result.TaskId)
- **Output preview:**

``````
$($r.Result.Output | Select-Object -First 30 | Out-String)
``````

"@
    }

    $content += "`n---`n*Log: $logFile*`n"
    Set-Content -Path $reportFile -Value $content -Encoding UTF8
    Write-Log "Report saved: $reportFile" "SUCCESS"
}

# ============================================================================
# MAIN
# ============================================================================

function Main {
    Write-Section "AI REPAIR ORCHESTRATOR"

    Write-Log "Script root:    $scriptRoot"
    Write-Log "Units root:     $unitsRoot"
    Write-Log "Reports dir:    $reportsDir"
    Write-Log "Template file:  $templateFile"
    Write-Log "Model:          $ModelName"
    Write-Log "Yolo mode:      $yoloActive  [$AutoAcceptFlag]"
    Write-Log "Timeout:        $TimeoutMinutes min"
    Write-Log "Max concurrent: $MaxConcurrent"
    Write-Log "Dry run:        $DryRun"
    Write-Log "Read from JSON: $ReadFromJson"

    # Validate template
    if (-not (Test-Path $templateFile)) {
        Write-Log "Template not found: $templateFile" "ERROR"
        exit 1
    }

    # Validate kilocode CLI
    $kilCheck = Get-Command "kilocode" -ErrorAction SilentlyContinue
    if (-not $kilCheck) {
        Write-Log "kilocode not found in PATH" "WARN"
        if (-not $DryRun) {
            Write-Log "Install kilocode or use -DryRun to simulate" "ERROR"
            exit 1
        }
    }
    else {
        Write-Log "kilocode found: $($kilCheck.Source)" "SUCCESS"
    }

    if ($yoloActive) {
        Write-Log "YOLO MODE ACTIVE - all kilocode actions auto-accepted ($AutoAcceptFlag)" "WARN"
    }

    # Discover failed units
    if ($ReadFromJson) {
        $script:failedUnits = Get-FailedUnitsFromJson
    }
    else {
        $script:failedUnits = Get-FailedUnitsByScan
    }

    if ($script:failedUnits.Count -eq 0) {
        Write-Log "No failed units found - nothing to repair" "SUCCESS"
        Save-Report
        exit 0
    }

    # Process repairs
    Start-RepairQueue -FailedUnits $script:failedUnits

    # Save report
    Save-Report

    Write-Section "DONE"
    Write-Log "Repaired:         $($script:repairedUnits.Count)"
    Write-Log "Manual review:    $($script:manualInterventionUnits.Count)"
    Write-Log "Report:           $reportFile"
    Write-Log "Log:              $logFile"
}

Main
