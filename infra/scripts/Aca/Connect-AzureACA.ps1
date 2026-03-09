# --- Azure Container Apps Management ---
# Credentials are loaded from connect_credentials.json (gitignored)

param(
    [string]$AppName = ""  # Optional: override app name from command line
)

$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
$credentialsFile = Join-Path $scriptRoot "connect_credentials.json"

# Load credentials
if (-not (Test-Path $credentialsFile)) {
    Write-Host "ERROR: Credentials file not found: $credentialsFile" -ForegroundColor Red
    Write-Host "Copy connect_credentials.template.json to connect_credentials.json and fill in your values." -ForegroundColor Yellow
    exit 1
}

$credentials = Get-Content $credentialsFile | ConvertFrom-Json
$ResourceGroupName = $credentials.azure.resource_group
$EnvName = $credentials.azure.aca_environment_name
if ([string]::IsNullOrWhiteSpace($AppName)) {
    $AppName = $credentials.azure.aca_app_name
}

# Validate required fields
if ([string]::IsNullOrWhiteSpace($ResourceGroupName)) {
    Write-Host "ERROR: Missing required Azure credentials (resource_group)" -ForegroundColor Red
    exit 1
}

Write-Host "--- Azure Container Apps Management ---" -ForegroundColor Cyan
Write-Host "Resource Group: $ResourceGroupName"
Write-Host "Environment:    $EnvName"
Write-Host "App:            $AppName"
Write-Host ""

# 1. Check Azure login
$azAccount = az account show --query "user" -o tsv 2>$null
if ([string]::IsNullOrWhiteSpace($azAccount)) {
    Write-Host "Not logged in to Azure. Opening browser..." -ForegroundColor Yellow
    az login
}

# 2. Install/update Container Apps extension
Write-Host "Checking Azure CLI extensions..." -ForegroundColor Gray
az extension add --name containerapp --upgrade --yes 2>$null

# 3. List all container apps in environment (if no specific app)
if ([string]::IsNullOrWhiteSpace($AppName)) {
    Write-Host "`nListing all Container Apps in :" -ForegroundColor Cyan
    az containerapp list --resource-group $ResourceGroupName -o table
    exit 0
}

# 4. Get app info (FQDN)
$fqdn = az containerapp show --name $AppName --resource-group $ResourceGroupName --query "properties.configuration.ingress.fqdn" -o tsv 2>$null

if ($fqdn) {
    Write-Host "`nYour app is running at:" -ForegroundColor Green
    Write-Host "https://$fqdn" -ForegroundColor White
}
else {
    Write-Host "`nApp '$AppName' not found or has no ingress configured." -ForegroundColor Yellow
}

# 5. Show logs option
Write-Host "`nDo you want to view live logs from $AppName? (y/n)" -ForegroundColor Yellow
$choice = Read-Host
if ($choice -eq 'y') {
    az containerapp logs show --name $AppName --resource-group $ResourceGroupName --follow
}
