#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy all CloudInfraMap microservice units to Kubernetes on Mac Mini M4 (ARM64)

.DESCRIPTION
    Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker images for ARM64
    architecture and deploys via Helm. Skips units without a Dockerfile (library projects).
    Shows detailed build/deploy events and a failure summary at the end.
    Optimized for Mac Mini M4 with Apple Silicon (ARM64).

.PARAMETER Namespace
    Kubernetes namespace (default: cloudinframap)

.PARAMETER Registry
    Docker image registry prefix (default: cloudinframap)

.PARAMETER Tag
    Docker image tag (default: latest)

.PARAMETER SkipUnits
    Comma-separated list of units to skip entirely

.PARAMETER SkipBuild
    Skip docker build (use pre-existing images)

.PARAMETER SkipDeploy
    Skip helm deploy (build only)

.PARAMETER Local
    Use values-local.yaml for local Docker Desktop K8s deployment

.PARAMETER ValuesFile
    Explicit path to a Helm values override file

.PARAMETER Replicas
    Number of pod replicas (default: 1)

.PARAMETER DryRun
    Print commands without executing

.EXAMPLE
    ./deploy_all_mac.ps1
    ./deploy_all_mac.ps1 -SkipUnits "unit-billing-sync-orchestrator" -DryRun
    ./deploy_all_mac.ps1 -SkipBuild   # deploy only, images already built
    ./deploy_all_mac.ps1 -Local       # local Docker Desktop K8s
    ./deploy_all_mac.ps1 -Replicas 2  # deploy with 2 replicas
#>
param(
    [string]$Namespace = "cim",
    [string]$Registry = "cim",
    [string]$Tag = "latest",
    [string]$SkipUnits = "",
    [string]$DebugUnits = "",
    [string]$ValuesFile = "",
    [int]$Replicas = 1,
    [switch]$SkipBuild = $false,
    [switch]$SkipDeploy = $false,
    [switch]$Local = $false,
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Continue"
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$unitsRoot = Join-Path $scriptRoot "microservices/units"
$skipList = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$debugList = $DebugUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

# Target platform for Mac Mini M4 (Apple Silicon)
$Platform = "linux/arm64"

# ============================================================================
# HELPERS
# ============================================================================

function Write-Status {
    param([string]$Msg, [string]$Color = "White")
    Write-Host $Msg -ForegroundColor $Color
}

function Invoke-Cmd {
    param([string]$Cmd, [string[]]$CmdArgs)
    if ($DryRun) {
        Write-Status "  [DRY-RUN] $Cmd $($CmdArgs -join ' ')" "DarkGray"
        return 0
    }
    # Capture output to avoid polluting the return value pipeline
    $output = & $Cmd @CmdArgs 2>&1
    $code = $LASTEXITCODE
    if ($output) { $output | ForEach-Object { Write-Host $_ } }
    return $code
}

# ============================================================================
# UNIT TYPE DETECTION
# ============================================================================

function Get-UnitType {
    param([string]$UnitPath)

    $srcPath = Join-Path $UnitPath "src"

    # Python: has main.py in src/ or any depth
    if (Test-Path (Join-Path $srcPath "main.py")) { return "Python" }
    if (Get-ChildItem -Path $UnitPath -Filter "main.py" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1) { return "Python" }

    # Node.js: has package.json in src/ (frontend units)
    if (Test-Path (Join-Path $srcPath "package.json")) { return "NodeJS" }
    $pkgJson = Get-ChildItem -Path $UnitPath -Filter "package.json" -Recurse -Depth 3 -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -notmatch "[/\\]node_modules$" } | Select-Object -First 1
    if ($pkgJson) { return "NodeJS" }

    # .NET: has non-test csproj
    $csproj = Get-ChildItem -Path $UnitPath -Filter "*.csproj" -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "\.Tests?\.csproj$" } | Select-Object -First 1
    if ($csproj) { return "DotNet" }

    return "Unknown"
}

# ============================================================================
# BUILD (ARM64 for Mac Mini M4)
# ============================================================================

function Invoke-DockerBuild {
    param(
        [string]$UnitName,
        [string]$UnitPath,
        [string]$UnitType
    )

    # Find Dockerfile
    $dockerfile = Join-Path $UnitPath "Dockerfile"
    if (-not (Test-Path $dockerfile)) {
        Write-Status "  SKIP BUILD  $UnitName -- no Dockerfile found (library/support unit)" "DarkGray"
        return @{ Success = $true; Skipped = $true; Error = "" }
    }

    $buildContext = $scriptRoot  # root context so all units share proto/shared files
    if (-not $buildContext) { $buildContext = "." }

    # Define Image Tag
    $imageTag = "$Registry/${UnitName}:${Tag}"

    Write-Status "  BUILD [$UnitType] [ARM64]  $UnitName  ->  $imageTag" "Cyan"

    # Type-specific build args
    $extraArgs = switch ($UnitType) {
        "Python" { @("--build-arg", "UNIT_TYPE=python") }
        "NodeJS" { @("--build-arg", "UNIT_TYPE=nodejs") }
        "DotNet" { @("--build-arg", "UNIT_TYPE=dotnet") }
        default { @() }
    }

    # Build from project root context with absolute Dockerfile path
    # Use --platform linux/arm64 for Mac Mini M4 (Apple Silicon)
    $dockerArgs = New-Object System.Collections.Generic.List[string]
    $dockerArgs.Add("buildx")
    $dockerArgs.Add("build")
    $dockerArgs.Add("--platform")
    $dockerArgs.Add($Platform)
    $dockerArgs.Add("-t")
    $dockerArgs.Add($imageTag)
    $dockerArgs.Add("-f")
    $dockerArgs.Add($dockerfile)

    foreach ($arg in $extraArgs) { $dockerArgs.Add($arg) }
    $dockerArgs.Add($buildContext)

    $exitCode = Invoke-Cmd "docker" ([string[]]$dockerArgs)

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker build exit $exitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Docker build failed (exit $exitCode)" }
    }

    Write-Status "  OK  $UnitName  (image: $imageTag, platform: $Platform)" "Green"
    return @{ Success = $true; Skipped = $false; Error = "" }
}

# ============================================================================
# DEPLOY
# ============================================================================

function Invoke-HelmDeploy {
    param(
        [string]$UnitName,
        [string]$UnitPath,
        [int]$ReplicaCount = 1
    )

    Write-Status "  HELM  $UnitName" "White"

    # Path to unit's helm chart
    $unitChartPath = Join-Path $UnitPath "helm"

    if (-not (Test-Path $unitChartPath)) {
        Write-Status "  SKIP DEPLOY  $UnitName  (no helm chart found)" "DarkGray"
        return @{ Success = $true; Skipped = $true; Error = "" }
    }

    # Update helm dependencies (downloads base-service chart)
    Write-Status "  Updating Helm dependencies..." "Gray"
    $depExitCode = Invoke-Cmd "helm" @("dependency", "update", $unitChartPath)

    if ($depExitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (helm dependency update exit $depExitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Helm dependency update failed (exit $depExitCode)" }
    }

    # Resolve values file: -Local auto-detects values-local.yaml
    $resolvedValuesFile = $ValuesFile
    if ($Local -and -not $resolvedValuesFile) {
        $localValues = Join-Path $unitChartPath "values-local.yaml"
        if (Test-Path $localValues) {
            $resolvedValuesFile = $localValues
            Write-Status "  Using local values: $localValues" "DarkCyan"
        }
    }

    # Deploy using unit's chart (no --wait, fire-and-forget)
    $helmArgs = @(
        "upgrade", "--install", $UnitName, $unitChartPath,
        "--namespace", $Namespace,
        "--create-namespace",
        "--set", "base-service.unitName=$UnitName",
        "--set", "base-service.image.repository=$Registry/$UnitName",
        "--set", "base-service.image.tag=$Tag",
        "--set", "base-service.replicaCount=$ReplicaCount",
        "--set", "base-service.region=default",
        "--set", "base-service.domain=common",
        "--set", "resources.requests.cpu=10m",
        "--set", "resources.requests.memory=24Mi",
        "--set", "resources.limits.cpu=100m",
        "--set", "resources.limits.memory=256Mi"
    )

    # Debug mode for specific units
    if ($unitName -in $debugList) {
        $helmArgs += @(
            "--set", "base-service.debug.enabled=true",
            "--set", "base-service.debug.logLevel=Debug",
            "--set", "base-service.debug.daprLogLevel=debug",
            "--set", "base-service.debug.aspnetEnvironment=Development"
        )
        Write-Status "  DEBUG MODE ENABLED for $UnitName" "Yellow"
    }

    # Add values file if specified
    if ($resolvedValuesFile -and (Test-Path $resolvedValuesFile)) {
        $helmArgs += @("-f", $resolvedValuesFile)
    }

    $exitCode = Invoke-Cmd "helm" $helmArgs

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (helm exit $exitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Helm deploy failed (exit $exitCode)" }
    }

    Write-Status "  OK  $UnitName  (deployed, replicas: $ReplicaCount)" "Green"
    return @{ Success = $true; Skipped = $false; Error = "" }
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  CloudInfraMap - Deploy All Units"    "Cyan"
Write-Status "  Target: Mac Mini M4 (ARM64)"         "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "Namespace:  $Namespace"
Write-Status "Registry:   $Registry"
Write-Status "Tag:        $Tag"
Write-Status "Platform:   $Platform"
Write-Status "Replicas:   $Replicas"
Write-Status "Local:      $Local"
if ($skipList) {
    Write-Status "Skip:       $($skipList -join ", ")"
}
else {
    Write-Status "Skip:       (none)"
}
if ($debugList) {
    Write-Status "Debug:      $($debugList -join ", ")"
}
else {
    Write-Status "Debug:      (none)"
}
Write-Status "DryRun:     $DryRun"
Write-Status ""

$units = Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue
if (-not $units) {
    Write-Status "ERROR: No unit directories found in $unitsRoot" "Red"
    exit 1
}

$summary = @{ DotNet = 0; Python = 0; NodeJS = 0; Unknown = 0; Skipped = 0; BuildFailed = 0; DeployFailed = 0; BuildOk = 0; DeployOk = 0 }

# Track failures for final report
$buildFailures = [System.Collections.Generic.List[object]]::new()
$deployFailures = [System.Collections.Generic.List[object]]::new()

foreach ($unit in $units) {
    $unitName = $unit.Name

    Write-Status "----------------------------------------------------" "Yellow"
    Write-Status "Unit: $unitName" "Yellow"

    # Skip list
    if ($unitName -in $skipList) {
        Write-Status "  SKIP  $unitName (excluded)" "DarkGray"
        $summary.Skipped++
        continue
    }

    # Detect type
    $unitType = Get-UnitType -UnitPath $unit.FullName
    Write-Status "  Type: $unitType" "DarkGray"
    $summary[$unitType]++

    # Build
    $buildOk = $true
    if (-not $SkipBuild) {
        $buildResult = Invoke-DockerBuild -UnitName $unitName -UnitPath $unit.FullName -UnitType $unitType
        $buildOk = $buildResult.Success
        if (-not $buildOk) {
            $summary.BuildFailed++
            $buildFailures.Add([PSCustomObject]@{
                    Unit  = $unitName
                    Type  = $unitType
                    Error = $buildResult.Error
                })
        }
        elseif (-not $buildResult.Skipped) {
            $summary.BuildOk++
        }
    }

    # Deploy
    if (-not $SkipDeploy -and $buildOk) {
        $deployResult = Invoke-HelmDeploy -UnitName $unitName -UnitPath $unit.FullName -ReplicaCount $Replicas
        if (-not $deployResult.Success) {
            $summary.DeployFailed++
            $deployFailures.Add([PSCustomObject]@{
                    Unit  = $unitName
                    Type  = $unitType
                    Error = $deployResult.Error
                })
        }
        elseif (-not $deployResult.Skipped) {
            $summary.DeployOk++
        }
    }
    elseif (-not $buildOk) {
        $summary.DeployFailed++
        $deployFailures.Add([PSCustomObject]@{
                Unit  = $unitName
                Type  = $unitType
                Error = "Skipped - build failed"
            })
    }
}

# ============================================================================
# SUMMARY
# ============================================================================

Write-Status ""
Write-Status "======================================" "Cyan"
Write-Status "  Deployment Summary (ARM64)"           "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "  .NET units:    $($summary.DotNet)"
Write-Status "  Python units:  $($summary.Python)"
Write-Status "  Node.js units: $($summary.NodeJS)"
Write-Status "  Unknown:       $($summary.Unknown)"
Write-Status "  Skipped:       $($summary.Skipped)"
Write-Status ""
Write-Status "  Builds OK:     $($summary.BuildOk)" "Green"
Write-Status "  Deploys OK:    $($summary.DeployOk)" "Green"

$totalFailed = $summary.BuildFailed + $summary.DeployFailed
if ($totalFailed -gt 0) {
    Write-Status ""
    Write-Status "  Build failures:  $($summary.BuildFailed)" "Red"
    Write-Status "  Deploy failures: $($summary.DeployFailed)" "Red"
}
else {
    Write-Status "  Failures:      0" "Green"
}

# ============================================================================
# FAILURE DETAILS
# ============================================================================

if ($buildFailures.Count -gt 0) {
    Write-Status ""
    Write-Status "======================================" "Red"
    Write-Status "  BUILD FAILURES ($($buildFailures.Count))" "Red"
    Write-Status "======================================" "Red"
    foreach ($f in $buildFailures) {
        Write-Status "  [$($f.Type)]  $($f.Unit)" "Red"
        Write-Status "         $($f.Error)" "DarkRed"
    }
}

if ($deployFailures.Count -gt 0) {
    Write-Status ""
    Write-Status "======================================" "Red"
    Write-Status "  DEPLOY FAILURES ($($deployFailures.Count))" "Red"
    Write-Status "======================================" "Red"
    foreach ($f in $deployFailures) {
        Write-Status "  [$($f.Type)]  $($f.Unit)" "Red"
        Write-Status "         $($f.Error)" "DarkRed"
    }
}

Write-Status ""
Write-Status "======================================" "Cyan"

if ($totalFailed -gt 0) {
    exit 1
}
else {
    exit 0
}
