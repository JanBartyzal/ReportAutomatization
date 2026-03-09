# --- Connect to Azure AKS ---
# Credentials are loaded from connect_credentials.json (gitignored)

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
$ClusterName = $credentials.azure.aks_cluster_name
$SubscriptionId = $credentials.azure.subscription_id

# Validate required fields
if ([string]::IsNullOrWhiteSpace($ResourceGroupName) -or [string]::IsNullOrWhiteSpace($ClusterName)) {
    Write-Host "ERROR: Missing required Azure credentials (resource_group, aks_cluster_name)" -ForegroundColor Red
    exit 1
}

Write-Host "--- Connecting to Azure AKS: $ClusterName ---" -ForegroundColor Cyan

# 1. Check Azure login
$azAccount = az account show --query "user" -o tsv 2>$null
if ([string]::IsNullOrWhiteSpace($azAccount)) {
    Write-Host "Not logged in to Azure. Opening browser..." -ForegroundColor Yellow
    az login
}

# 2. Set subscription (if specified)
if (-not [string]::IsNullOrWhiteSpace($SubscriptionId)) {
    Write-Host "Setting subscription context..."
    az account set --subscription $SubscriptionId
}

# 3. Get AKS credentials (merge into .kube/config)
Write-Host "Downloading cluster credentials..." -ForegroundColor Green
az aks get-credentials --resource-group $ResourceGroupName --name $ClusterName --overwrite-existing

# 4. Switch kubectl context
$contextName = $ClusterName
kubectl config use-context $contextName

# 5. Verify connection
Write-Host "`nTesting connection to Azure AKS:" -ForegroundColor Cyan
kubectl cluster-info

Write-Host "`nDone! You are now connected to Azure AKS." -ForegroundColor Green
