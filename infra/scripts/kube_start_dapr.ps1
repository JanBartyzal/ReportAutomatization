# =============================================================================
# Dapr + Kubernetes Setup Script
# Připraví Dapr runtime a komponenty pro CloudInfraMap
# =============================================================================

$ErrorActionPreference = "Stop"

Write-Host "=== 1. Vytváření namespaces ===" -ForegroundColor Cyan
kubectl create namespace dapr-system --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace cim --dry-run=client -o yaml | kubectl apply -f -

Write-Host "=== 2. Instalace Dapr runtime (Helm) ===" -ForegroundColor Cyan
# Přidání Dapr Helm repo
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update

# Instalace/upgrade Dapr runtime s potřebnými komponentami
helm upgrade --install dapr dapr/dapr `
  --namespace dapr-system `
  --set global.mtls.enabled=false `
  --set dapr_sentry.enabled=true `
  --set dapr_sidecar_injector.enabled=true `
  --set dapr_operator.enabled=true `
  --set dapr_placement.enabled=true `
  --wait `
  --timeout 5m

Write-Host "=== 3. Čekání na Dapr pody ===" -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=dapr -n dapr-system --timeout=120s

Write-Host "=== 4. Nasazení Redis ===" -ForegroundColor Cyan
# Redis je potřeba pro Dapr statestore a pubsub komponenty
@"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: cim
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        resources:
          requests:
            memory: "64Mi"
            cpu: "50m"
          limits:
            memory: "128Mi"
            cpu: "100m"
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: cim
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
"@ | kubectl apply -f -

Write-Host "Čekání na Redis pod..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=redis -n cim --timeout=60s

Write-Host "=== 5. Vytváření Azure identity secretu ===" -ForegroundColor Cyan
# Secret pro Azure Key Vault přístup (tenant-id, client-id, client-secret)
kubectl create secret generic azure-identity --namespace cim `
  --from-literal=tenant-id=$env:AZURE_TENANT_ID `
  --from-literal=client-id=$env:AZURE_CLIENT_ID `
  --from-literal=client-secret=$env:AZURE_CLIENT_SECRET `
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic azure-identity --namespace dapr-system `
  --from-literal=tenant-id=$env:AZURE_TENANT_ID `
  --from-literal=client-id=$env:AZURE_CLIENT_ID `
  --from-literal=client-secret=$env:AZURE_CLIENT_SECRET `
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic azure-identity --namespace clouply-shared `
  --from-literal=tenant-id=$env:AZURE_TENANT_ID `
  --from-literal=client-id=$env:AZURE_CLIENT_ID `
  --from-literal=client-secret=$env:AZURE_CLIENT_SECRET `
  --dry-run=client -o yaml | kubectl apply -f -


kubectl create secret generic clouply-service-token --from-literal=service-token='+s6vI9om0qGvikXg1481cE5XYhgcRwL+D7aPjqwLqFY=' --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic clouply-service-token --namespace cim --from-literal=service-token='+s6vI9om0qGvikXg1481cE5XYhgcRwL+D7aPjqwLqFY=' --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic clouply-service-token --namespace dapr-system --from-literal=service-token='+s6vI9om0qGvikXg1481cE5XYhgcRwL+D7aPjqwLqFY=' --dry-run=client -o yaml | kubectl apply -f -

Write-Host "=== 6. Aplikování Dapr Configuration ===" -ForegroundColor Cyan
# Dapr Configuration (referenced by dapr.io/config annotation)
kubectl apply -f ../dapr/components/dapr-config.yaml -n dapr-system
kubectl apply -f ../dapr/components/dapr-config.yaml -n cim

# Dodatečná Configuration s tracing a metrics
@"
apiVersion: dapr.io/v1alpha1
kind: Configuration
metadata:
  name: dapr-system
  namespace: cim
spec:
  mtls:
    enabled: false
    controlPlaneTrustDomain: cluster.local
    sentryAddress: dapr-sentry.dapr-system.svc.cluster.local:443
  tracing:
    samplingRate: "1"
  metric:
    enabled: true
"@ | kubectl apply -f -


Write-Host "=== 7. Aplikování Dapr komponent ===" -ForegroundColor Cyan
# Aplikovat všechny Dapr komponenty do cim namespace
# POZN: storage-binding.yaml.disabled je záměrně vypnutý pro local dev
#       (Azure Blob Storage vyžaduje skutečné credentials)
kubectl apply -f ../dapr/components/ -n cim

Write-Host "=== 8. Restart application podů (aby dostaly nový sidecar) ===" -ForegroundColor Cyan
# Smazat unit-* pody (ne Redis)
$unitPods = kubectl get pods -n cim -o name | Where-Object { $_ -match "unit-" }
if ($unitPods) {
  $unitPods | ForEach-Object { kubectl delete $_ -n cim }
}
Start-Sleep -Seconds 5

Write-Host "=== 9. Verifikace ===" -ForegroundColor Cyan
Write-Host "Dapr system pody:" -ForegroundColor Yellow
kubectl get pods -n dapr-system

Write-Host "`nDapr komponenty v cim namespace:" -ForegroundColor Yellow
kubectl get components.dapr.io -n cim

Write-Host "`nDapr konfigurace:" -ForegroundColor Yellow
kubectl get configurations.dapr.io -A

# Nastavení default namespace
kubectl config set-context --current --namespace=cim

Write-Host "`n=== 10. Port-forward pro Traefik ===" -ForegroundColor Cyan
# Čekání na unit-web-gw-core service (pokud existuje)
$coreService = kubectl get svc unit-web-gw-core -n cim --ignore-not-found -o name 2>$null
if ($coreService) {
  Write-Host "Spouštím port-forward na pozadí (localhost:8000 -> unit-web-gw-core:80)..." -ForegroundColor Yellow
  Start-Job -ScriptBlock { kubectl port-forward svc/unit-web-gw-core -n cim 8000:80 } | Out-Null
  Write-Host "Port-forward běží na pozadí. Traefik může routovat /api a /auth na K8s." -ForegroundColor Green
}
else {
  Write-Host "Service unit-web-gw-core neexistuje. Port-forward přeskočen." -ForegroundColor Yellow
  Write-Host "Po deployi spusťte: kubectl port-forward svc/unit-web-gw-core -n cim 8000:80" -ForegroundColor Yellow
}

Write-Host "`n=== Hotovo! Dapr je připraven. ===" -ForegroundColor Green
Write-Host "Nyní můžete deployovat unity pomocí deploy_unit.ps1" -ForegroundColor Green

