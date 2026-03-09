#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy a single CloudInfraMap microservice unit to Kubernetes on Mac Mini M4 (ARM64)

.DESCRIPTION
    Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker image for ARM64
    architecture and deploys via Helm. Skips units without a Dockerfile (library projects).
    Optimized for Mac Mini M4 with Apple Silicon (ARM64).

.PARAMETER UnitName
    Name of the unit to deploy (e.g. unit-web-gateway). Required.

.PARAMETER Namespace
    Kubernetes namespace (default: cloudinframap)

.PARAMETER Registry
    Docker image registry prefix (default: cloudinframap)

.PARAMETER Tag
    Docker image tag (default: latest)

.PARAMETER SkipBuild
    Skip docker build (use pre-existing images)

.PARAMETER SkipDeploy
    Skip helm deploy (build only)

.PARAMETER Replicas
    Number of pod replicas (default: 1)

.PARAMETER DryRun
    Print commands without executing

.EXAMPLE
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway -DryRun
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway -SkipBuild
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway -Tag "v1.2.3" -Registry "myregistry.azurecr.io/cloudinframap"
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway -Local
    ./deploy_unit_mac.ps1 -UnitName unit-web-gateway -Replicas 2
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = "Name of the unit directory (e.g. unit-web-gateway)")]
    [string]$UnitName,

    [string]$Namespace = "cim",
    [string]$Registry = "cim",
    [string]$Tag = "latest",
    [string]$ValuesFile = "",
    [int]$Replicas = 1,
    [switch]$Local = $false,
    [switch]$SkipBuild = $false,
    [switch]$SkipDeploy = $false,
    [switch]$EnableDebug = $false,
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Continue"
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$unitsRoot = Join-Path $scriptRoot "microservices/units"

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
    # Use Write-Host to display output without polluting the return value
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
        return $true   # not an error, just a lib project
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
    $dockerArgs.Add($dockerfile)  # absolute path to unit's Dockerfile
    $dockerArgs.Add("--no-cache")

    foreach ($arg in $extraArgs) { $dockerArgs.Add($arg) }
    $dockerArgs.Add($buildContext)  # project root as build context

    $exitCode = Invoke-Cmd "docker" ([string[]]$dockerArgs)

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker build exit $exitCode)" "Red"
        return $false
    }

    Write-Status "  OK  $UnitName  (image: $imageTag, platform: $Platform)" "Green"
    return $true
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

    $unitChartPath = Join-Path $UnitPath "helm"

    if (-not (Test-Path $unitChartPath)) {
        Write-Status "  SKIP DEPLOY  $UnitName  (no helm chart found at $unitChartPath)" "DarkGray"
        return $true
    }

    # Update helm dependencies (downloads base-service chart)
    Write-Status "  Updating Helm dependencies..." "Gray"
    $depExitCode = Invoke-Cmd "helm" @("dependency", "update", $unitChartPath)

    if ($depExitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (helm dependency update exit $depExitCode)" "Red"
        return $false
    }

    # Resolve values file: -Local auto-detects values-local.yaml, -ValuesFile uses explicit path
    $resolvedValuesFile = $ValuesFile
    if ($Local -and -not $resolvedValuesFile) {
        $localValues = Join-Path $unitChartPath "values-local.yaml"
        if (Test-Path $localValues) {
            $resolvedValuesFile = $localValues
            Write-Status "  Using local values: $localValues" "DarkCyan"
        }
        else {
            Write-Status "  WARN  No values-local.yaml found, using defaults" "Yellow"
        }
    }

    $helmArgs = @(
        "upgrade", "--install", $UnitName, $unitChartPath,
        "--namespace", $Namespace,
        "--create-namespace",
        "--set", "base-service.unitName=$UnitName",
        "--set", "base-service.image.repository=$Registry/$UnitName",
        "--set", "base-service.image.tag=$Tag",
        "--set", "base-service.replicaCount=$ReplicaCount",
        "--set", "base-service.resources.requests.memory=64Mi",
        "--set", "base-service.resources.limits.memory=256Mi",
        "--set", "base-service.resources.requests.cpu=10m",
        "--set", "base-service.resources.limits.cpu=500m",
        "--wait",
        "--timeout", "5m"
    )

    # Debug mode
    if ($EnableDebug) {
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
        return $false
    }

    Write-Status "  OK  $UnitName  (deployed to namespace: $Namespace, replicas: $ReplicaCount)" "Green"
    return $true
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  CloudInfraMap - Deploy Single Unit"  "Cyan"
Write-Status "  Target: Mac Mini M4 (ARM64)"         "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "Unit:       $UnitName"
Write-Status "Namespace:  $Namespace"
Write-Status "Registry:   $Registry"
Write-Status "Tag:        $Tag"
Write-Status "Platform:   $Platform"
Write-Status "Replicas:   $Replicas"
Write-Status "SkipBuild:  $SkipBuild"
Write-Status "SkipDeploy: $SkipDeploy"
Write-Status "Debug:      $EnableDebug"
Write-Status "DryRun:     $DryRun"
Write-Status ""

# Resolve unit path
$unitPath = Join-Path $unitsRoot $UnitName

if (-not (Test-Path $unitPath)) {
    Write-Status "ERROR: Unit directory not found: $unitPath" "Red"
    Write-Status ""
    Write-Status "Available units:" "Yellow"
    Get-ChildItem -Path $unitsRoot -Directory -Filter "unit-*" -ErrorAction SilentlyContinue |
    ForEach-Object { Write-Status "  - $($_.Name)" "Gray" }
    exit 1
}

# Detect type
$unitType = Get-UnitType -UnitPath $unitPath
Write-Status "  Detected type: $unitType" "DarkGray"
Write-Status ""

# Build
$buildOk = $true
if (-not $SkipBuild) {
    $buildOk = Invoke-DockerBuild -UnitName $UnitName -UnitPath $unitPath -UnitType $unitType
}
else {
    Write-Status "  SKIP BUILD (--SkipBuild)" "DarkGray"
}

# Deploy
$deployOk = $true
if (-not $SkipDeploy -and $buildOk) {
    $deployOk = Invoke-HelmDeploy -UnitName $UnitName -UnitPath $unitPath -ReplicaCount $Replicas
}
elseif ($SkipDeploy) {
    Write-Status "  SKIP DEPLOY (--SkipDeploy)" "DarkGray"
}
elseif (-not $buildOk) {
    Write-Status "  SKIP DEPLOY (build failed)" "Red"
}

# Final status
Write-Status ""
Write-Status "======================================" "Cyan"
if ($buildOk -and $deployOk) {
    Write-Status "  RESULT: SUCCESS" "Green"
    Write-Status "  Unit '$UnitName' [$unitType] deployed to '$Namespace' (ARM64)" "Green"
    $exitCodeFinal = 0
}
else {
    Write-Status "  RESULT: FAILED" "Red"
    if (-not $buildOk) { Write-Status "  Build failed" "Red" }
    if (-not $deployOk) { Write-Status "  Deploy failed" "Red" }
    $exitCodeFinal = 1
}
Write-Status "======================================" "Cyan"

exit $exitCodeFinal
