param(
    [string]$Namespace = "cim"
)

Write-Host "--- Hledám pody s chybujícími kontejnery v namespace: $Namespace ---" -ForegroundColor Cyan

# Získáme všechny pody a filtrujeme je v PowerShellu místo v kubectl
$allPods = kubectl get pods -n $Namespace -o json | ConvertFrom-Json

# Najdeme pody, kde alespoň jeden kontejner není ve stavu 'running' nebo má restarty
$failedPods = $allPods.items | Where-Object {
    $_.status.containerStatuses | Where-Object { 
        ($_.state.running -eq $null) -or ($_.restartCount -gt 0) 
    }
}

if (-not $failedPods) {
    Write-Host "Nenalezeny žádné pody v chybovém stavu." -ForegroundColor Green
    exit 0
}

foreach ($pod in $failedPods) {
    $podName = $pod.metadata.name
    Write-Host "`n[POD: $podName]" -ForegroundColor Yellow

    foreach ($cs in $pod.status.containerStatuses) {
        $cName = $cs.name
        $restarts = $cs.restartCount
        
        # Pokud kontejner neběží nebo už restartoval, vypíšeme logy
        if ($null -eq $cs.state.running -or $restarts -gt 0) {
            Write-Host "  -> Kontejner: $cName (Restarty: $restarts)" -ForegroundColor Magenta
            
            # --previous vypíše logy z POSLEDNÍ havárie (klíčové pro CrashLoopBackOff)
            Write-Host "  --- Logy z předchozí havárie: ---" -ForegroundColor Gray
            kubectl logs $podName -c $cName -n $Namespace --previous --tail=30 2>$null
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  --- Aktuální logy (předchozí nejsou): ---" -ForegroundColor Gray
                kubectl logs $podName -c $cName -n $Namespace --tail=30
            }
        }
    }
    Write-Host "---------------------------------------------------" -ForegroundColor DarkGray
}