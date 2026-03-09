param(
    [Parameter(Mandatory = $true)]
    [string]$UnitName,

    [string]$Namespace = "cim"
)

Write-Host "--- Vyhledávání podů pro jednotku: $UnitName ve jmenném prostoru: $Namespace ---" -ForegroundColor Cyan

# 1. Získání seznamu jmen podů na základě Helm labelu
# Používáme jsonpath pro čistý seznam jmen oddělených mezerou
$podNames = kubectl get pods -n $Namespace -l "app.kubernetes.io/instance=$UnitName" -o jsonpath='{.items[*].metadata.name}'

if (-not $podNames) {
    Write-Host "Žádné běžící pody pro '$UnitName' nebyly nalezeny." -ForegroundColor Yellow
    exit 0
}

# Převod řetězce na pole (pokud je podů víc)
$podList = $podNames.Split(" ")

Write-Host "Nalezeno $($podList.Count) podů k odstranění." -ForegroundColor White

# 2. Odstranění nalezených podů
foreach ($pod in $podList) {
    Write-Host "Odstraňuji pod: $pod ..." -ForegroundColor Magenta
    kubectl delete pod $pod -n $Namespace
}

Write-Host "--- Hotovo ---" -ForegroundColor Green