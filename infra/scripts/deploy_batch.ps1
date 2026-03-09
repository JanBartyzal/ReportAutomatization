
param(
    [ValidateSet("cim", "pulse", "archdecide", "guard", "suite")]
    [string]$Product = "cim",

    [switch]$DryRun = $false,
    [switch]$SkipBuild = $false
)

$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "../..")).Path
$unitsRoot = Join-Path $repoRoot "apps/$Product/microservices/units"

$suiteUnits = @(
    "unit-billing-subscription",
    "unit-config-secrets-service",
    "unit-identity-service",
    "unit-notification-service",
    "unit-observability-service",
    "unit-rbac-policy-engine"
)

$cimUnits = @(
    "unit-ai-assistant",
    "unit-api-gateway",
    "unit-asset-lifecycle",
    "unit-audit-governance",
    "unit-autonomous-finops",
    "unit-backup-restore",
    "unit-cloud-billing-connectors",
    "unit-compliance-engine",
    "unit-cost-calculator",
    "unit-data-privacy",
    "unit-enterprise-integrations",
    "unit-export-service",
    "unit-forecasting-anomaly",
    "unit-iac-parser-service",
    "unit-init",
    "unit-network-storage-engine",
    "unit-onboarding-sandbox",
    "unit-onprem-infrastructure",
    "unit-optimization-engine",
    "unit-pricing-ingestion",
    "unit-project-portfolio",
    "unit-report-generator",
    "unit-scenario-benchmark",
    "unit-search-jobs",
    "unit-sustainability",
    "unit-visual-designer"
)


$toRun = @()

switch ($Product) {
    "suite" {
        $toRun = $suiteUnits
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow
    }
    "cim" {
        $toRun = $cimUnits
        Write-Host "Selected batch units: $toRun" -ForegroundColor Yellow
    }
    default {
        $toRun = @()
        Write-Host "No batch units selected" -ForegroundColor Yellow
    }
}

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   Clouply Suite - Batch Deployment"            -ForegroundColor Cyan
Write-Host "   Product: $Product"           -ForegroundColor Cyan
Write-Host "   UnitsRoot: $unitsRoot"                       -ForegroundColor DarkGray
if ($DryRun) { Write-Host "   MODE: DRY-RUN" -ForegroundColor Magenta }
Write-Host "==============================================" -ForegroundColor Cyan

foreach ($u in $toRun) {
    $unitPath = Join-Path $unitsRoot $u

    if (Test-Path $unitPath) {
        Write-Host "`n>>> Deploying: $u" -ForegroundColor White

        try {
            $deployArgs = @{
                UnitName    = $u
                Product     = $Product
                EnableDebug = $true
                DryRun      = $DryRun
                SkipBuild   = $SkipBuild
            }
            & "$scriptRoot\deploy_unit.ps1" @deployArgs

            if ($LASTEXITCODE -ne 0) {
                Write-Error "Deployment of $u failed with exit code $LASTEXITCODE"
            }
        }
        catch {
            Write-Host "Critical error during deployment of $u : $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    else {
        Write-Host "Warning: Path for unit $u not found at $unitPath" -ForegroundColor Yellow
    }
}

Write-Host "`nBatch $Group Deployment Completed! (Product: $Product)" -ForegroundColor Green
