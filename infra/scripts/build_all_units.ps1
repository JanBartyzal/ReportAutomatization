#Requires -Version 5.1
<#
.SYNOPSIS
    Build all C# microservice source projects

.DESCRIPTION
    Discovers and builds all C# microservice source projects using the same
    reliable MSBuild error format as compile_all.ps1.
    Saves structured results to reports/build_results.json for downstream scripts.
    Supports all products (cim, pulse, archdecide, guard) and shared services.

.PARAMETER Product
    Product scope: cim, pulse, archdecide, guard, shared (default: cim)

.PARAMETER SkipUnits
    Comma-separated list of units to skip

.PARAMETER ReportPath
    Path where reports will be saved (default: ./reports)

.EXAMPLE
    .\build_all_units.ps1
    .\build_all_units.ps1 -Product cim
    .\build_all_units.ps1 -Product suite
    .\build_all_units.ps1 -SkipUnits "unit-billing-sync-orchestrator,unit-other"
#>
param(
    [ValidateSet("cim", "pulse", "archdecide", "guard", "suite")]
    [string]$Product = "cim",

    [string]$SkipUnits = "bu",
    [string]$ReportPath = "./reports"
)

$ErrorActionPreference = "Continue"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "../..")).Path

$unitsRoot = Join-Path $repoRoot "apps/$Product/microservices/units"

$reportDir = Join-Path $scriptRoot $ReportPath
$errorFile = Join-Path $reportDir "build_errors.txt"
$resultsFile = Join-Path $reportDir "build_results.json"

if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }
if (Test-Path $errorFile) { Clear-Content $errorFile } else { New-Item -Path $errorFile -ItemType File | Out-Null }

$skipList = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Clouply Suite - Build Units"          -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Product: $Product"
Write-Host "Units:   $unitsRoot"
Write-Host "Skip:   $(if ($skipList) { $skipList -join ', ' } else { '(none)' })"
Write-Host ""

# --- Build base-unit first (shared Base/*.cs code) ------------------------
$baseUnitCsproj = Join-Path $scriptRoot "base-unit\BaseUnit.csproj"
if (Test-Path $baseUnitCsproj) {
    Write-Host "  BUILD base-unit [BaseUnit.csproj]" -ForegroundColor White
    $baseOutput = & dotnet build $baseUnitCsproj -nologo -clp:NoSummary --configuration Release -p:TreatWarningsAsErrors=false 2>&1
    $baseExitCode = $LASTEXITCODE

    if ($baseExitCode -eq 0) {
        Write-Host "        OK" -ForegroundColor Green
    }
    else {
        Write-Host "        FAILED (exit $baseExitCode)" -ForegroundColor Red
        foreach ($line in $baseOutput) {
            if ($line -match '(?i)\berror\b') {
                Write-Host "        ERR $line" -ForegroundColor Red
                Add-Content -Path $errorFile -Value "base-unit | $line"
            }
        }
        Write-Host ""
        Write-Host "ERROR: base-unit build failed. Fix Base code before building units." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue
if (-not $units) {
    Write-Host "ERROR: No unit directories found in $unitsRoot" -ForegroundColor Red
    exit 1
}

$unitResults = [System.Collections.Generic.List[object]]::new()
$successCount = 0
$failCount = 0

foreach ($unit in $units) {
    if ($unit.Name -in $skipList) {
        Write-Host "  SKIP  $($unit.Name)" -ForegroundColor DarkGray
        continue
    }

    $srcDir = Join-Path $unit.FullName "src"

    # --- Detect non-.NET units and skip them gracefully --------------------
    # Python: has main.py in src/ or anywhere in the unit tree
    $isPython = (Test-Path (Join-Path $srcDir "main.py")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1))

    # Node.js: has package.json in src/ or a shallow subdir (not node_modules)
    $isNodeJs = (Test-Path (Join-Path $srcDir "package.json")) -or
    ($null -ne (Get-ChildItem -Path $unit.FullName -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1))

    if ($isPython) {
        Write-Host "  SKIP  $($unit.Name) [Python - use pytest]" -ForegroundColor DarkCyan
        continue
    }
    if ($isNodeJs) {
        Write-Host "  SKIP  $($unit.Name) [Node.js - use npm]" -ForegroundColor DarkCyan
        continue
    }

    # --- Find source .csproj -----------------------------------------------
    # Priority 1: src/ directory (standard layout)
    $csproj = $null
    if (Test-Path $srcDir) {
        $csproj = Get-ChildItem -Path $srcDir -Filter "*.csproj" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    }

    # Priority 2: recursive search, excluding test projects
    if (-not $csproj) {
        $csproj = Get-ChildItem -Path $unit.FullName -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" -and $_.DirectoryName -notmatch "[/\\]tests?$" } |
        Select-Object -First 1
    }

    if (-not $csproj) {
        Write-Host "  WARN  $($unit.Name) - no source .csproj found (unknown type)" -ForegroundColor Yellow
        $unitResults.Add([PSCustomObject]@{
                Name       = $unit.Name
                CsprojPath = $null
                Success    = $false
                Errors     = @("No source .csproj found")
                Warnings   = @()
            })
        $failCount++
        continue
    }

    Write-Host "  BUILD $($unit.Name) [$($csproj.Name)]" -ForegroundColor White

    # --- Run dotnet build --------------------------------------------------
    $buildOutput = & dotnet build $csproj.FullName -nologo -clp:NoSummary --configuration Release -p:TreatWarningsAsErrors=false 2>&1
    $exitCode = $LASTEXITCODE

    $errors = [System.Collections.Generic.List[string]]::new()
    $warnings = [System.Collections.Generic.List[string]]::new()

    foreach ($line in $buildOutput) {
        # Standard MSBuild error format: file(line,col): error CODE: message
        if ($line -match '^\s*(?<file>.*)\(\d+,\d+\):\s+error\s+(?<msg>.*)$') {
            $msg = "$($Matches.file.Trim()) | $($Matches.msg.Trim())"
            $errors.Add($msg)
            Add-Content -Path $errorFile -Value "$($unit.Name) | $msg"
            Write-Host "        ERR $($Matches.msg.Trim())" -ForegroundColor Red
        }
        elseif ($line -match '^\s*(?<file>.*)\(\d+,\d+\):\s+warning\s+(?<msg>.*)$') {
            $warnings.Add($Matches.msg.Trim())
        }
    }

    # Capture general failures when no parsed errors exist
    if ($exitCode -ne 0 -and $errors.Count -eq 0) {
        $fallback = $buildOutput | Where-Object { $_ -match "(?i)\berror\b" } | Select-Object -First 5
        foreach ($e in $fallback) { $errors.Add($e.ToString().Trim()) }
        if ($errors.Count -eq 0) { $errors.Add("Build failed with exit code $exitCode") }
    }

    if ($exitCode -eq 0) {
        Write-Host "        OK" -ForegroundColor Green
        $successCount++
    }
    else {
        Write-Host "        FAILED (exit $exitCode)" -ForegroundColor Red
        $failCount++
    }

    $unitResults.Add([PSCustomObject]@{
            Name       = $unit.Name
            CsprojPath = $csproj.FullName
            Success    = ($exitCode -eq 0)
            Errors     = $errors.ToArray()
            Warnings   = $warnings.ToArray()
        })
}

# --- Save JSON results -----------------------------------------------------
[PSCustomObject]@{
    Timestamp  = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    TotalUnits = $unitResults.Count
    Succeeded  = $successCount
    Failed     = $failCount
    Units      = $unitResults.ToArray()
} | ConvertTo-Json -Depth 5 | Out-File -FilePath $resultsFile -Encoding UTF8 -Force

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Build Summary"
Write-Host "  Units:   $($unitResults.Count)"
Write-Host "  Success: $successCount" -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host "  Failed:  $failCount" -ForegroundColor Red
}
else {
    Write-Host "  Failed:  $failCount" -ForegroundColor Green
}
Write-Host "  Errors:  $errorFile"
Write-Host "  Results: $resultsFile"
