#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy a single CloudInfraMap microservice unit to Azure Container Apps

.DESCRIPTION
    Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker image,
    pushes to Azure Container Registry (ACR) and deploys to Azure Container Apps.
    Skips units without a Dockerfile (library projects).

.PARAMETER UnitName
    Name of the unit to deploy (e.g. unit-web-gateway). Required.

.PARAMETER ResourceGroup
    Azure resource group name. Required.

.PARAMETER AcrName
    Azure Container Registry name (without .azurecr.io). Required.

.PARAMETER Environment
    Azure Container Apps Environment name. Required.

.PARAMETER Tag
    Docker image tag (default: latest)

.PARAMETER SkipBuild
    Skip docker build and push (use pre-existing images in ACR)

.PARAMETER SkipDeploy
    Skip Azure Container Apps deploy (build only)

.PARAMETER Replicas
    Number of container replicas (default: 1)

.PARAMETER MinReplicas
    Minimum number of replicas for scaling (default: 0)

.PARAMETER MaxReplicas
    Maximum number of replicas for scaling (default: 10)

.PARAMETER Ingress
    Ingress type: internal, external, or none (default: internal)

.PARAMETER TargetPort
    Target port for the container (default: 8080)

.PARAMETER Cpu
    CPU allocation (default: 0.5)

.PARAMETER Memory
    Memory allocation (default: 1Gi)

.PARAMETER DryRun
    Print commands without executing

.EXAMPLE
    ./deploy_unit_aca.ps1 -UnitName unit-web-gateway -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap"
    ./deploy_unit_aca.ps1 -UnitName unit-web-gateway -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -DryRun
    ./deploy_unit_aca.ps1 -UnitName unit-web-gateway -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -SkipBuild
    ./deploy_unit_aca.ps1 -UnitName unit-web-gateway -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -Replicas 2
    ./deploy_unit_aca.ps1 -UnitName unit-web-gateway -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -Ingress external
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = "Name of the unit directory (e.g. unit-web-gateway)")]
    [string]$UnitName,

    [Parameter(Mandatory = $true, HelpMessage = "Azure resource group name")]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true, HelpMessage = "Azure Container Registry name (without .azurecr.io)")]
    [string]$AcrName,

    [Parameter(Mandatory = $true, HelpMessage = "Azure Container Apps Environment name")]
    [string]$Environment,

    [string]$Tag = "latest",
    [int]$Replicas = 1,
    [int]$MinReplicas = 0,
    [int]$MaxReplicas = 10,
    [ValidateSet("internal", "external", "none")]
    [string]$Ingress = "internal",
    [int]$TargetPort = 8080,
    [string]$Cpu = "0.5",
    [string]$Memory = "1Gi",
    [switch]$SkipBuild = $false,
    [switch]$SkipDeploy = $false,
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Continue"
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$unitsRoot = Join-Path $scriptRoot "microservices/units"

# ACR login server
$AcrLoginServer = "$AcrName.azurecr.io"

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
# BUILD & PUSH TO ACR
# ============================================================================

function Invoke-AcrBuild {
    param(
        [string]$UnitName,
        [string]$UnitPath,
        [string]$UnitType
    )

    # Find Dockerfile
    $dockerfile = Join-Path $UnitPath "Dockerfile"
    if (-not (Test-Path $dockerfile)) {
        Write-Status "  SKIP BUILD  $UnitName -- no Dockerfile found (library/support unit)" "DarkGray"
        return $true
    }

    $buildContext = $scriptRoot
    if (-not $buildContext) { $buildContext = "." }

    # Define Image Tag (ACR format)
    $imageTag = "$AcrLoginServer/${UnitName}:${Tag}"

    Write-Status "  BUILD & PUSH [$UnitType]  $UnitName  ->  $imageTag" "Cyan"

    # Type-specific build args
    $extraArgs = switch ($UnitType) {
        "Python" { @("--build-arg", "UNIT_TYPE=python") }
        "NodeJS" { @("--build-arg", "UNIT_TYPE=nodejs") }
        "DotNet" { @("--build-arg", "UNIT_TYPE=dotnet") }
        default { @() }
    }

    # Build locally first
    $dockerArgs = New-Object System.Collections.Generic.List[string]
    $dockerArgs.Add("buildx")
    $dockerArgs.Add("build")
    $dockerArgs.Add("-t")
    $dockerArgs.Add($imageTag)
    $dockerArgs.Add("-f")
    $dockerArgs.Add($dockerfile)
    $dockerArgs.Add("--no-cache")

    foreach ($arg in $extraArgs) { $dockerArgs.Add($arg) }
    $dockerArgs.Add($buildContext)

    $exitCode = Invoke-Cmd "docker" ([string[]]$dockerArgs)

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker build exit $exitCode)" "Red"
        return $false
    }

    # Push to ACR
    Write-Status "  PUSH  $UnitName  ->  $imageTag" "Cyan"
    $pushExitCode = Invoke-Cmd "docker" @("push", $imageTag)

    if ($pushExitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker push exit $pushExitCode)" "Red"
        return $false
    }

    Write-Status "  OK  $UnitName  (image: $imageTag)" "Green"
    return $true
}

# ============================================================================
# DEPLOY TO AZURE CONTAINER APPS
# ============================================================================

function Invoke-AcaDeploy {
    param(
        [string]$UnitName,
        [string]$UnitPath,
        [int]$ReplicaCount = 1
    )

    Write-Status "  ACA DEPLOY  $UnitName" "White"

    $imageTag = "$AcrLoginServer/${UnitName}:${Tag}"

    # Check if container app exists
    $existsOutput = & az containerapp show --name $UnitName --resource-group $ResourceGroup 2>&1
    $exists = $LASTEXITCODE -eq 0

    if ($exists) {
        # Update existing container app
        Write-Status "  Updating existing Container App..." "Gray"

        $acaArgs = @(
            "containerapp", "update",
            "--name", $UnitName,
            "--resource-group", $ResourceGroup,
            "--image", $imageTag,
            "--min-replicas", $MinReplicas,
            "--max-replicas", $MaxReplicas,
            "--cpu", $Cpu,
            "--memory", $Memory
        )
    }
    else {
        # Create new container app
        Write-Status "  Creating new Container App..." "Gray"

        $acaArgs = @(
            "containerapp", "create",
            "--name", $UnitName,
            "--resource-group", $ResourceGroup,
            "--environment", $Environment,
            "--image", $imageTag,
            "--registry-server", $AcrLoginServer,
            "--min-replicas", $MinReplicas,
            "--max-replicas", $MaxReplicas,
            "--cpu", $Cpu,
            "--memory", $Memory
        )

        # Add ingress settings
        if ($Ingress -ne "none") {
            $acaArgs += @("--ingress", $Ingress, "--target-port", $TargetPort)
        }
    }

    $exitCode = Invoke-Cmd "az" $acaArgs

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (az containerapp exit $exitCode)" "Red"
        return $false
    }

    Write-Status "  OK  $UnitName  (deployed to ACA, replicas: $MinReplicas-$MaxReplicas)" "Green"
    return $true
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  CloudInfraMap - Deploy Single Unit"  "Cyan"
Write-Status "  Target: Azure Container Apps"        "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "Unit:          $UnitName"
Write-Status "ResourceGroup: $ResourceGroup"
Write-Status "ACR:           $AcrLoginServer"
Write-Status "Environment:   $Environment"
Write-Status "Tag:           $Tag"
Write-Status "Replicas:      $Replicas (min: $MinReplicas, max: $MaxReplicas)"
Write-Status "Ingress:       $Ingress"
Write-Status "TargetPort:    $TargetPort"
Write-Status "Resources:     CPU: $Cpu, Memory: $Memory"
Write-Status "SkipBuild:     $SkipBuild"
Write-Status "SkipDeploy:    $SkipDeploy"
Write-Status "DryRun:        $DryRun"
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

# Login to ACR
if (-not $DryRun -and -not $SkipBuild) {
    Write-Status "Logging in to Azure Container Registry..." "Gray"
    $loginExitCode = Invoke-Cmd "az" @("acr", "login", "--name", $AcrName)
    if ($loginExitCode -ne 0) {
        Write-Status "ERROR: Failed to login to ACR. Run 'az login' first." "Red"
        exit 1
    }
}

# Build & Push
$buildOk = $true
if (-not $SkipBuild) {
    $buildOk = Invoke-AcrBuild -UnitName $UnitName -UnitPath $unitPath -UnitType $unitType
}
else {
    Write-Status "  SKIP BUILD (--SkipBuild)" "DarkGray"
}

# Deploy
$deployOk = $true
if (-not $SkipDeploy -and $buildOk) {
    $deployOk = Invoke-AcaDeploy -UnitName $UnitName -UnitPath $unitPath -ReplicaCount $Replicas
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
    Write-Status "  Unit '$UnitName' [$unitType] deployed to Azure Container Apps" "Green"
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
