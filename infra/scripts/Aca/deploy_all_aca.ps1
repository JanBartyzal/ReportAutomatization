#Requires -Version 5.1
<#
.SYNOPSIS
    Deploy all CloudInfraMap microservice units to Azure Container Apps

.DESCRIPTION
    Auto-detects unit type (.NET 10 / Python / Node.js), builds Docker images,
    pushes to Azure Container Registry (ACR) and deploys to Azure Container Apps.
    Skips units without a Dockerfile (library projects).
    Shows detailed build/deploy events and a failure summary at the end.

.PARAMETER ResourceGroup
    Azure resource group name. Required.

.PARAMETER AcrName
    Azure Container Registry name (without .azurecr.io). Required.

.PARAMETER Environment
    Azure Container Apps Environment name. Required.

.PARAMETER Tag
    Docker image tag (default: latest)

.PARAMETER SkipUnits
    Comma-separated list of units to skip entirely

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

.PARAMETER DryRun
    Print commands without executing

.EXAMPLE
    ./deploy_all_aca.ps1 -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap"
    ./deploy_all_aca.ps1 -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -DryRun
    ./deploy_all_aca.ps1 -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -SkipBuild
    ./deploy_all_aca.ps1 -ResourceGroup "rg-cloudinframap" -AcrName "acrcloudinframap" -Environment "cae-cloudinframap" -Replicas 2
#>
param(
    [Parameter(Mandatory = $true, HelpMessage = "Azure resource group name")]
    [string]$ResourceGroup,

    [Parameter(Mandatory = $true, HelpMessage = "Azure Container Registry name (without .azurecr.io)")]
    [string]$AcrName,

    [Parameter(Mandatory = $true, HelpMessage = "Azure Container Apps Environment name")]
    [string]$Environment,

    [string]$Tag = "latest",
    [string]$SkipUnits = "",
    [int]$Replicas = 1,
    [int]$MinReplicas = 0,
    [int]$MaxReplicas = 10,
    [switch]$SkipBuild = $false,
    [switch]$SkipDeploy = $false,
    [switch]$DryRun = $false
)

$ErrorActionPreference = "Continue"
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$unitsRoot = Join-Path $scriptRoot "microservices/units"
$skipList = $SkipUnits -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }

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
        return @{ Success = $true; Skipped = $true; Error = "" }
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

    foreach ($arg in $extraArgs) { $dockerArgs.Add($arg) }
    $dockerArgs.Add($buildContext)

    $exitCode = Invoke-Cmd "docker" ([string[]]$dockerArgs)

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker build exit $exitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Docker build failed (exit $exitCode)" }
    }

    # Push to ACR
    Write-Status "  PUSH  $UnitName  ->  $imageTag" "Cyan"
    $pushExitCode = Invoke-Cmd "docker" @("push", $imageTag)

    if ($pushExitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (docker push exit $pushExitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Docker push failed (exit $pushExitCode)" }
    }

    Write-Status "  OK  $UnitName  (image: $imageTag)" "Green"
    return @{ Success = $true; Skipped = $false; Error = "" }
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
            "--max-replicas", $MaxReplicas
        )

        # Set exact replicas if no scaling
        if ($MinReplicas -eq $MaxReplicas) {
            $acaArgs += @("--replica", $ReplicaCount)
        }
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
            "--ingress", "internal",
            "--target-port", "8080",
            "--cpu", "0.5",
            "--memory", "1Gi"
        )
    }

    $exitCode = Invoke-Cmd "az" $acaArgs

    if ($exitCode -ne 0) {
        Write-Status "  FAILED  $UnitName  (az containerapp exit $exitCode)" "Red"
        return @{ Success = $false; Skipped = $false; Error = "Azure Container Apps deploy failed (exit $exitCode)" }
    }

    Write-Status "  OK  $UnitName  (deployed to ACA, replicas: $MinReplicas-$MaxReplicas)" "Green"
    return @{ Success = $true; Skipped = $false; Error = "" }
}

# ============================================================================
# MAIN
# ============================================================================

Write-Status "======================================" "Cyan"
Write-Status "  CloudInfraMap - Deploy All Units"    "Cyan"
Write-Status "  Target: Azure Container Apps"        "Cyan"
Write-Status "======================================" "Cyan"
Write-Status "ResourceGroup: $ResourceGroup"
Write-Status "ACR:           $AcrLoginServer"
Write-Status "Environment:   $Environment"
Write-Status "Tag:           $Tag"
Write-Status "Replicas:      $Replicas (min: $MinReplicas, max: $MaxReplicas)"
if ($skipList) {
    Write-Status "Skip:          $($skipList -join ", ")"
}
else {
    Write-Status "Skip:          (none)"
}
Write-Status "DryRun:        $DryRun"
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

    # Build & Push
    $buildOk = $true
    if (-not $SkipBuild) {
        $buildResult = Invoke-AcrBuild -UnitName $unitName -UnitPath $unit.FullName -UnitType $unitType
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
        $deployResult = Invoke-AcaDeploy -UnitName $unitName -UnitPath $unit.FullName -ReplicaCount $Replicas
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
Write-Status "  Deployment Summary (Azure Container Apps)" "Cyan"
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
