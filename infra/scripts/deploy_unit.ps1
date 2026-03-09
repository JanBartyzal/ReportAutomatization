#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy a single Clouply microservice unit to Kubernetes

.DESCRIPTION
    Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker image
    and deploys via Helm. Skips units without a Dockerfile (library projects).
    Supports all products (cim, pulse, archdecide, guard) and shared services.

.PARAMETER UnitName
    Name of the unit to deploy (e.g. unit-web-gateway). Required.

.PARAMETER Product
    Product scope: cim, pulse, archdecide, guard, shared (default: cim)

.PARAMETER Namespace
    Kubernetes namespace (default: auto from Product)

.PARAMETER Registry
    Docker image registry prefix (default: auto from Product)

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
    .\deploy_unit.ps1 -UnitName unit-web-gateway
    .\deploy_unit.ps1 -UnitName unit-web-gateway -Product cim
    .\deploy_unit.ps1 -UnitName unit-identity-service -Product shared
    .\deploy_unit.ps1 -UnitName unit-web-gateway -Product pulse
    .\deploy_unit.ps1 -UnitName unit-web-gateway -DryRun
    .\deploy_unit.ps1 -UnitName unit-web-gateway -SkipBuild
    .\deploy_unit.ps1 -UnitName unit-web-gateway -Tag "v1.2.3" -Registry "myregistry.azurecr.io/clouply"
    .\deploy_unit.ps1 -UnitName unit-web-gateway -Local
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = "Name of the unit directory (e.g. unit-web-gateway)")]
    [string]$UnitName,

    [ValidateSet("cim", "pulse", "archdecide", "guard", "suite")]
    [string]$Product = "cim",

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
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "../..")).Path
$unitsRoot = Join-Path $repoRoot "apps/$Product/microservices/units"


# Default namespace and registry from product if not explicitly set
#if (-not $Namespace) { $Namespace = if ($Product -eq "shared") { "clouply-shared" } else { $Product } }
#if (-not $Registry) { $Registry = if ($Product -eq "shared") { "clouply" } else { "clouply-$Product" } }

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
# BUILD
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

    $buildContext = $repoRoot  # repo root context so all units share proto/shared files
    if (-not $buildContext) { $buildContext = "." }

    # Define Image Tag
    $imageTag = "$Registry/${UnitName}:${Tag}"

    Write-Status "  BUILD [$UnitType]  $UnitName  ->  $imageTag" "Cyan"

    # Type-specific build args
    $extraArgs = switch ($UnitType) {
        "Python" { @("--build-arg", "UNIT_TYPE=python") }
        "NodeJS" { @("--build-arg", "UNIT_TYPE=nodejs") }
        "DotNet" { @("--build-arg", "UNIT_TYPE=dotnet") }
        default { @() }
    }

    # Build from project root context with absolute Dockerfile path
    # Dockerfile COPY paths (e.g. packages/protos/, packages/dotnet-base/) are relative to build context (project root)
    $dockerArgs = New-Object System.Collections.Generic.List[string]
    $dockerArgs.Add("buildx")
    $dockerArgs.Add("build")
    $dockerArgs.Add("-t")
    $dockerArgs.Add($imageTag)
    $dockerArgs.Add("-f")
    $dockerArgs.Add($dockerfile)  # absolute path to unit's Dockerfile
    $dockerArgs.Add("--no-cache")
    #$dockerArgs.Add("--debug")
        
    foreach ($arg in $extraArgs) { $dockerArgs.Add($arg) }
    $dockerArgs.Add($buildContext)  # project root as build context
    
    $exitCode = Invoke-Cmd "docker" ([string[]]$dockerArgs)

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker build exit $exitCode)" "Red"
        return $false
    }

    Write-Status "  OK  $UnitName  (image: $imageTag)" "Green"
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

    $ServiceType = "ClusterIP"

    # Pokud je to gateway, vystavíme ji ven
    if ($UnitName -match "unit-api-gateway") {
        $ServiceType = "LoadBalancer"
    }

    # ============================================================================
    # 1. LOGIKA PORTŮ (gRPC 5000 / HTTP 8080)
    # ============================================================================
    $isGrpc = $UnitName -notmatch "unit-api-gateway"
    $appPort = if ($isGrpc) { 5000 } else { 8080 }
    $protocol = if ($isGrpc) { "grpc" } else { "http" }
    $serviceType = if ($isGrpc) { "ClusterIP" } else { "LoadBalancer" }

    # ============================================================================
    # 2. SESTAVENÍ HELM ARGUMENTŮ
    # ============================================================================
    $helmArgs = @(
        "upgrade", "--install", $UnitName, $unitChartPath,
        "--namespace", $Namespace,
        "--create-namespace",

        # OPRAVA "Port 0": Posíláme porty přímo i pod prefixem base-service
        # Toto vyplní chybějící místo v service.yaml šabloně
        "--set", "service.httpPort=8080",
        "--set", "service.grpcPort=5000",
        "--set", "service.targetPort=$appPort",
        "--set", "base-service.service.httpPort=8080",
        "--set", "base-service.service.grpcPort=5000",
        "--set", "base-service.service.targetPort=$appPort",

        # Identifikace jednotky
        "--set", "unitName=$UnitName",
        "--set", "base-service.unitName=$UnitName",
        "--set", "base-service.replicaCount=$ReplicaCount",
        "--set", "base-service.service.type=$serviceType",

        # Dapr sidecar (včetně dapr-config a protokolů)
        "--set-string", "base-service.dapr.enabled=true",
        "--set-string", "base-service.dapr.appId=$UnitName",
        "--set-string", "base-service.dapr.appPort=$appPort",
        "--set-string", "base-service.dapr.protocol=$protocol",
        "--set-string", "base-service.dapr.config=dapr-config",

        # Image a zdroje
        "--set", "base-service.image.repository=$Registry/$UnitName",
        "--set", "base-service.image.tag=$Tag",
        "--set", "base-service.image.pullPolicy=IfNotPresent",
        "--set", "base-service.resources.requests.cpu=10m",
        "--set", "base-service.resources.limits.cpu=500m",
        "--set", "base-service.resources.requests.memory=64Mi",
        "--set-string", "base-service.resources.limits.memory=256Mi"
    )

    # ============================================================================
    # 3. .NET SPECIFICKÉ (Kestrel porty)
    # ============================================================================
    if ($UnitType -eq "DotNet") {
        # Nastavení obou portů v aplikaci, aby proby (8080) i Dapr (5000) fungovaly
        $helmArgs += @("--set", "base-service.env[0].name=ASPNETCORE_URLS")
        $helmArgs += @("--set", "base-service.env[0].value=http://+:8080;http://+:5000")
    }

    # Výjimka pro config-service
    if ($UnitName -eq "unit-config-secrets-service") {
        $helmArgs += @("--set", "base-service.initContainers.waitForConfigDistributor.enabled=false")
    }

    # ============================================================================
    # SPECIFICKÉ DOPLŇKY (Kestrel, Config Service, Debug)
    # ============================================================================

    # Nastavení prostředí pro .NET (aby Kestrel otevřel oba porty)
    if ($UnitType -eq "DotNet") {
        $helmArgs += @("--set", "base-service.env[0].name=ASPNETCORE_URLS")
        $helmArgs += @("--set", "base-service.env[0].value=http://+:8080;http://+:5000")
    }

    # Vypnutí čekání pro config-secrets-service (aby nečekala sama na sebe)
    if ($UnitName -eq "unit-config-secrets-service") {
        $helmArgs += @("--set", "base-service.initContainers.waitForConfigDistributor.enabled=false")
    }

    # Debug režim (volitelně)
    if ($EnableDebug) {
        $helmArgs += @(
            "--set", "base-service.debug.enabled=true",
            "--set", "base-service.debug.logLevel=Debug",
            "--set", "base-service.debug.daprLogLevel=debug"
        )
    }

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

    Write-Status "  OK  $UnitName  (deployed to namespace: $Namespace)" "Green"
    return $true
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  Clouply Suite - Deploy Single Unit"  "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "Product:    $Product"
Write-Status "Unit:       $UnitName"
Write-Status "UnitsRoot:  $unitsRoot"
Write-Status "Namespace:  $Namespace"
Write-Status "Registry:   $Registry"
Write-Status "Tag:        $Tag"
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
    Write-Status "  Unit '$UnitName' [$unitType] deployed to '$Namespace'" "Green"
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
