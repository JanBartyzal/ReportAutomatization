#Requires -Version 5.1
<#
.SYNOPSIS
    Build and test a single Clouply microservice unit

.DESCRIPTION
    Discovers the source .csproj and test .csproj for a given unit,
    runs dotnet build and dotnet test, and reports results.
    Supports .NET, Python, and Node.js unit types.
    Supports all products (cim, pulse, archdecide, guard) and shared services.

.PARAMETER UnitName
    Name of the unit directory (e.g. unit-web-gateway). Required.

.PARAMETER Product
    Product scope: cim, pulse, archdecide, guard, shared (default: cim)

.PARAMETER SkipBuild
    Skip the build step (run tests only)

.PARAMETER SkipTest
    Skip the test step (build only)

.PARAMETER NoBuild
    Pass --no-build to dotnet test (assumes build was already done)

.PARAMETER Configuration
    Build configuration (default: Release)

.PARAMETER ReportPath
    Path where reports will be saved (default: ./reports)

.EXAMPLE
    .\build_unit.ps1 -UnitName unit-web-gateway
    .\build_unit.ps1 -UnitName unit-web-gateway -Product cim
    .\build_unit.ps1 -UnitName unit-identity-service -Product shared
    .\build_unit.ps1 -UnitName unit-web-gateway -SkipTest
    .\build_unit.ps1 -UnitName unit-web-gateway -SkipBuild -NoBuild
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = "Name of the unit directory (e.g. unit-web-gateway)")]
    [string]$UnitName,

    [ValidateSet("cim", "pulse", "archdecide", "guard", "suite")]
    [string]$Product = "cim",

    [switch]$SkipBuild = $false,
    [switch]$SkipTest = $false,
    [switch]$NoBuild = $false,
    [string]$Configuration = "Release",
    [string]$ReportPath = "./reports"
)

$ErrorActionPreference = "Continue"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "../..")).Path
$unitsRoot = Join-Path $repoRoot "apps/$Product/microservices/units"


$reportDir = Join-Path $scriptRoot $ReportPath
$errorFile = Join-Path $reportDir "build_errors_unit.txt"

if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }
if (Test-Path $errorFile) { Clear-Content $errorFile } else { New-Item -Path $errorFile -ItemType File | Out-Null }

# ============================================================================
# HELPERS
# ============================================================================

function Write-Status {
    param([string]$Msg, [string]$Color = "White")
    Write-Host $Msg -ForegroundColor $Color
}

# ============================================================================
# UNIT TYPE DETECTION
# ============================================================================

function Get-UnitType {
    param([string]$UnitPath)

    $srcPath = Join-Path $UnitPath "src"

    # Python
    if (Test-Path (Join-Path $srcPath "main.py")) { return "Python" }
    if (Get-ChildItem -Path $UnitPath -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1) { return "Python" }

    # Node.js
    if (Test-Path (Join-Path $srcPath "package.json")) { return "NodeJS" }
    $pkgJson = Get-ChildItem -Path $UnitPath -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1
    if ($pkgJson) { return "NodeJS" }

    # .NET
    $csproj = Get-ChildItem -Path $UnitPath -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } | Select-Object -First 1
    if ($csproj) { return "DotNet" }

    return "Unknown"
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  Clouply Suite - Build & Test Unit"   "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "Product:       $Product"
Write-Status "Unit:          $UnitName"
Write-Status "UnitsRoot:     $unitsRoot"
Write-Status "Configuration: $Configuration"
Write-Status "SkipBuild:     $SkipBuild"
Write-Status "SkipTest:      $SkipTest"
Write-Status ""

# --- Resolve unit path -------------------------------------------------------
$unitPath = Join-Path $unitsRoot $UnitName

if (-not (Test-Path $unitPath)) {
    Write-Status "ERROR: Unit directory not found: $unitPath" "Red"
    Write-Status ""
    Write-Status "Available units:" "Yellow"
    Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue |
    ForEach-Object { Write-Status "  - $($_.Name)" "Gray" }
    exit 1
}

# --- Detect unit type ---------------------------------------------------------
$unitType = Get-UnitType -UnitPath $unitPath
Write-Status "Detected type: $unitType" "DarkGray"
Write-Status ""

$buildSuccess = $true
$testSuccess = $true

# ============================================================================
# BUILD
# ============================================================================

if (-not $SkipBuild) {

    Write-Status "--- BUILD ---" "Yellow"

    switch ($unitType) {
        "Python" {
            Write-Status "  Python unit - checking syntax with py_compile..." "Cyan"
            $mainPy = Get-ChildItem -Path $unitPath -Filter "main.py" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($mainPy) {
                $output = & python -m py_compile $mainPy.FullName 2>&1
                if ($LASTEXITCODE -ne 0) {
                    Write-Status "  FAILED  Python syntax check" "Red"
                    $output | ForEach-Object { Write-Status "    $_" "Red" }
                    $buildSuccess = $false
                }
                else {
                    Write-Status "  OK  Python syntax check" "Green"
                }
            }
            else {
                Write-Status "  WARN  No main.py found" "Yellow"
            }

            # Install requirements if present
            $reqFile = Get-ChildItem -Path $unitPath -Filter "requirements.txt" -Recurse -Depth 2 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($reqFile) {
                Write-Status "  Installing requirements..." "Gray"
                & pip install -r $reqFile.FullName --quiet 2>&1 | Out-Null
            }
        }

        "NodeJS" {
            Write-Status "  Node.js unit - running npm install & build..." "Cyan"
            $pkgJson = Get-ChildItem -Path $unitPath -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
            Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1
            if ($pkgJson) {
                $pkgDir = $pkgJson.DirectoryName
                Push-Location $pkgDir
                try {
                    & npm install 2>&1 | Out-Null
                    if ($LASTEXITCODE -ne 0) {
                        Write-Status "  FAILED  npm install" "Red"
                        $buildSuccess = $false
                    }
                    else {
                        # Check if build script exists
                        $pkgContent = Get-Content $pkgJson.FullName -Raw | ConvertFrom-Json
                        if ($pkgContent.scripts -and $pkgContent.scripts.build) {
                            & npm run build 2>&1
                            if ($LASTEXITCODE -ne 0) {
                                Write-Status "  FAILED  npm run build" "Red"
                                $buildSuccess = $false
                            }
                            else {
                                Write-Status "  OK  npm build" "Green"
                            }
                        }
                        else {
                            Write-Status "  OK  npm install (no build script)" "Green"
                        }
                    }
                }
                finally {
                    Pop-Location
                }
            }
        }

        "DotNet" {
            # Find source .csproj (priority: src/ directory, then recursive excluding tests)
            $srcDir = Join-Path $unitPath "src"
            $csproj = $null
            if (Test-Path $srcDir) {
                $csproj = Get-ChildItem -Path $srcDir -Filter "*.csproj" -ErrorAction SilentlyContinue |
                Select-Object -First 1
            }
            if (-not $csproj) {
                $csproj = Get-ChildItem -Path $unitPath -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" -and $_.DirectoryName -notmatch "[/\\]tests?$" } |
                Select-Object -First 1
            }

            if (-not $csproj) {
                Write-Status "  WARN  No source .csproj found for $UnitName" "Yellow"
                $buildSuccess = $false
            }
            else {
                Write-Status "  BUILD $UnitName [$($csproj.Name)]" "White"

                $buildOutput = & dotnet build $csproj.FullName -nologo -clp:NoSummary --configuration $Configuration -p:TreatWarningsAsErrors=false 2>&1
                $exitCode = $LASTEXITCODE

                $errors = [System.Collections.Generic.List[string]]::new()
                $warnings = [System.Collections.Generic.List[string]]::new()

                foreach ($line in $buildOutput) {
                    if ($line -match '^\s*(?<file>.*)\(\d+,\d+\):\s+error\s+(?<msg>.*)$') {
                        $msg = "$($Matches.file.Trim()) | $($Matches.msg.Trim())"
                        $errors.Add($msg)
                        Add-Content -Path $errorFile -Value "$UnitName | $msg"
                        Write-Status "    ERR $($Matches.msg.Trim())" "Red"
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
                    Write-Status "  OK  Build succeeded ($($warnings.Count) warnings)" "Green"
                }
                else {
                    Write-Status "  FAILED  Build failed ($($errors.Count) errors)" "Red"
                    $buildSuccess = $false
                }
            }
        }

        default {
            Write-Status "  WARN  Unknown unit type - cannot build" "Yellow"
            $buildSuccess = $false
        }
    }
}
else {
    Write-Status "--- BUILD SKIPPED ---" "DarkGray"
}

Write-Status ""

# ============================================================================
# TEST
# ============================================================================

if (-not $SkipTest) {

    Write-Status "--- TEST ---" "Yellow"

    if (-not $buildSuccess -and -not $SkipBuild) {
        Write-Status "  SKIP TEST  Build failed, skipping tests" "Red"
        $testSuccess = $false
    }
    else {
        switch ($unitType) {
            "Python" {
                Write-Status "  Running pytest..." "Cyan"
                $testDir = Join-Path $unitPath "tests"
                if (Test-Path $testDir) {
                    $output = & python -m pytest $testDir -v 2>&1
                    if ($LASTEXITCODE -ne 0) {
                        Write-Status "  FAILED  pytest" "Red"
                        $output | ForEach-Object { Write-Status "    $_" "Gray" }
                        $testSuccess = $false
                    }
                    else {
                        Write-Status "  OK  pytest passed" "Green"
                    }
                }
                else {
                    Write-Status "  WARN  No tests/ directory found" "Yellow"
                }
            }

            "NodeJS" {
                Write-Status "  Running npm test..." "Cyan"
                $pkgJson = Get-ChildItem -Path $unitPath -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
                Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1
                if ($pkgJson) {
                    $pkgContent = Get-Content $pkgJson.FullName -Raw | ConvertFrom-Json
                    if ($pkgContent.scripts -and $pkgContent.scripts.test) {
                        Push-Location $pkgJson.DirectoryName
                        try {
                            $output = & npm test 2>&1
                            if ($LASTEXITCODE -ne 0) {
                                Write-Status "  FAILED  npm test" "Red"
                                $testSuccess = $false
                            }
                            else {
                                Write-Status "  OK  npm test passed" "Green"
                            }
                        }
                        finally {
                            Pop-Location
                        }
                    }
                    else {
                        Write-Status "  WARN  No test script in package.json" "Yellow"
                    }
                }
            }

            "DotNet" {
                # Find test .csproj
                $testCsproj = $null
                $testsDir = Join-Path $unitPath "tests"
                if (Test-Path $testsDir) {
                    $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                    Where-Object { $_.Name -match "\.Tests?\.csproj$" -or $_.DirectoryName -match "[/\\]tests?$" } |
                    Select-Object -First 1
                    if (-not $testCsproj) {
                        $testCsproj = Get-ChildItem -Path $testsDir -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                        Select-Object -First 1
                    }
                }
                if (-not $testCsproj) {
                    $testCsproj = Get-ChildItem -Path $unitPath -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
                    Where-Object { $_.Name -match "\.Tests?\.csproj$" } |
                    Select-Object -First 1
                }

                if (-not $testCsproj) {
                    Write-Status "  WARN  No test project found for $UnitName" "Yellow"
                }
                else {
                    Write-Status "  TEST  $UnitName [$($testCsproj.Name)]" "White"

                    $dotnetArgs = @(
                        "test", $testCsproj.FullName,
                        "--configuration", $Configuration,
                        "--logger", "console;verbosity=minimal"
                    )
                    if ($NoBuild) { $dotnetArgs += "--no-build" }

                    $testOutput = & dotnet @dotnetArgs 2>&1
                    $exitCode = $LASTEXITCODE

                    $passed = 0
                    $failed = 0
                    $testErrors = [System.Collections.Generic.List[string]]::new()

                    foreach ($line in $testOutput) {
                        if ($line -match '(\d+)\s+passed') { $passed = [int]$Matches[1] }
                        if ($line -match '(\d+)\s+failed') { $failed = [int]$Matches[1] }
                        if ($line -match 'FAILED\s+(\S+)') { $testErrors.Add("FAILED: $($Matches[1])") }
                        if ($line -match '^\s*(?<file>.*)\(\d+,\d+\):\s+error\s+(?<msg>.*)$') {
                            $testErrors.Add("BUILD ERROR: $($Matches.msg.Trim())")
                        }
                    }

                    if ($exitCode -eq 0) {
                        Write-Status "  OK  $passed passed" "Green"
                    }
                    else {
                        Write-Status "  FAILED  $passed passed, $failed failed" "Red"
                        foreach ($err in $testErrors) {
                            Write-Status "    $err" "Red"
                        }
                        $testSuccess = $false
                    }
                }
            }

            default {
                Write-Status "  WARN  Unknown unit type - cannot test" "Yellow"
            }
        }
    }
}
else {
    Write-Status "--- TEST SKIPPED ---" "DarkGray"
}

# ============================================================================
# SUMMARY
# ============================================================================

Write-Status ""
Write-Status "======================================" "Cyan"
Write-Status "  Result for: $UnitName [$unitType]"

$overallSuccess = $buildSuccess -and $testSuccess

if ($overallSuccess) {
    Write-Status "  BUILD: $(if ($SkipBuild) { 'SKIPPED' } else { 'OK' })" "Green"
    Write-Status "  TEST:  $(if ($SkipTest)  { 'SKIPPED' } else { 'OK' })" "Green"
    Write-Status "  OVERALL: SUCCESS" "Green"
}
else {
    if (-not $buildSuccess) { Write-Status "  BUILD: FAILED" "Red" }
    else { Write-Status "  BUILD: $(if ($SkipBuild) { 'SKIPPED' } else { 'OK' })" "Green" }
    if (-not $testSuccess) { Write-Status "  TEST:  FAILED" "Red" }
    else { Write-Status "  TEST:  $(if ($SkipTest)  { 'SKIPPED' } else { 'OK' })" "Green" }
    Write-Status "  OVERALL: FAILED" "Red"
}

Write-Status "======================================" "Cyan"

if ($overallSuccess) { exit 0 } else { exit 1 }
