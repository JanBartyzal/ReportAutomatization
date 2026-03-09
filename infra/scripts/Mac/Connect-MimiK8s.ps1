# --- Connect to Kubernetes on Mac Mini M4 (MIMI) ---
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
$MacName = $credentials.mac_mini.hostname
$User = $credentials.mac_mini.username
$Password = $credentials.mac_mini.password
$LocalPort = $credentials.mac_mini.local_port
$RemotePort = $credentials.mac_mini.remote_port
$K8sContext = $credentials.mac_mini.k8s_context

# Validate required fields
if ([string]::IsNullOrWhiteSpace($MacName) -or [string]::IsNullOrWhiteSpace($User)) {
    Write-Host "ERROR: Missing required Mac Mini credentials (hostname, username)" -ForegroundColor Red
    exit 1
}

Write-Host "--- Connecting to Kubernetes on Mac Mini M4 ($MacName) ---" -ForegroundColor Cyan

# 1. Kill any existing SSH tunnels on this port
Get-Process | Where-Object { $_.ProcessName -eq "ssh" } | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process | Where-Object { $_.ProcessName -eq "plink" } | Stop-Process -Force -ErrorAction SilentlyContinue

# 2. Start SSH tunnel in background
# Use plink (PuTTY) or native ssh
if (Get-Command "plink" -ErrorAction SilentlyContinue) {
    Write-Host "Opening SSH tunnel via plink..."
    if (-not [string]::IsNullOrWhiteSpace($Password)) {
        Start-Process plink -ArgumentList "-batch -pw `"$Password`" -L ${LocalPort}:127.0.0.1:${RemotePort} ${User}@${MacName} -N" -WindowStyle Hidden
    }
    else {
        Write-Host "Warning: No password set. Using SSH key authentication..." -ForegroundColor Yellow
        Start-Process plink -ArgumentList "-batch -t -pw $Password -L ${LocalPort}:127.0.0.1:${RemotePort} ${User}@${MacName} -N" -WindowStyle Hidden
        # Přidali jsme parametr -t a -N pro udržení spojení
        
    }
}
else {
    Write-Host "Warning: 'plink' not found. Using native ssh (may require manual password entry)..." -ForegroundColor Yellow
    Start-Process ssh -ArgumentList "-L ${LocalPort}:127.0.0.1:${RemotePort} ${User}@${MacName} -N" -NoNewWindow
}

# 3. Wait for tunnel to initialize
Start-Sleep -Seconds 2

# 4. Switch kubectl context
Write-Host "Switching kubectl context to $K8sContext..." -ForegroundColor Green
kubectl config use-context $K8sContext

# 5. Verify connection
Write-Host "`nTesting connection to cluster:" -ForegroundColor Cyan
kubectl cluster-info

Write-Host "`nDone! You can now use kubectl as if running locally on Mac Mini." -ForegroundColor Green
