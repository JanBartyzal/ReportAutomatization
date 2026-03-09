# 1. Nastavení kontextu (jistota, že jsme v Docker Desktop)
Write-Host "--- Přepínám kontext na Docker Desktop ---" -ForegroundColor Cyan
kubectl config use-context docker-desktop

# 2. Smazání tvého namespace (tím zmizí všechny tvé unity)
Write-Host "--- Mažu namespace 'cim' (včetně všech unit) ---" -ForegroundColor Yellow
kubectl delete namespace cim --ignore-not-found

Write-Host "--- Mažu namespace 'cloupy-shared' (včetně všech unit) ---" -ForegroundColor Yellow
kubectl delete namespace cloupy-shared --ignore-not-found

# 3. Vyčištění Dapr v Kubernetes
Write-Host "--- Odstraňuji Dapr z clusteru ---" -ForegroundColor Yellow
dapr uninstall -k

# 4. Agresivní čistka Dockeru (smaže nepoužívané vrstvy a image)
Write-Host "--- Čistím Docker (Pruning) ---" -ForegroundColor Cyan
docker system prune -a -f --volumes

# 5. Restartování Dapr Control Plane
Write-Host "--- Instaluji čerstvý Dapr Control Plane ---" -ForegroundColor Green
# Používáme Helm, protože je na Windows stabilnější než dapr init -k
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm install dapr dapr/dapr --namespace dapr-system --create-namespace --wait

kubectl patch configuration dapr-system -n dapr-system --type merge -p '{\"spec\": {\"mtls\": {\"enabled\": false}}}'

# 6. Kontrola stavu
Write-Host "--- Hotovo! Aktuální stav clusteru: ---" -ForegroundColor Cyan
kubectl get pods -A
dapr status -k