# Test Azure Key Vault přístup přes vault-connector a config-distributor
# Tento skript testuje, zda services dokážou získat secrets z Azure KV

Write-Host "`n=== TEST: Azure Key Vault Access ===" -ForegroundColor Cyan

# 1. Test vault-connector health
Write-Host "`n1. Testing vault-connector health..." -ForegroundColor Yellow
$vaultJob = Start-Job -ScriptBlock { kubectl port-forward -n cim deployment/unit-vault-connector 8092:8080 }
Start-Sleep -Seconds 3

try {
    $vaultHealth = Invoke-RestMethod -Uri "http://localhost:8092/health" -Method Get
    Write-Host "✓ Vault-connector is healthy" -ForegroundColor Green
    Write-Host "  Response: $($vaultHealth | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Vault-connector health check failed: $_" -ForegroundColor Red
} finally {
    Stop-Job -Job $vaultJob -ErrorAction SilentlyContinue
    Remove-Job -Job $vaultJob -ErrorAction SilentlyContinue
}

# 2. Test config-distributor health
Write-Host "`n2. Testing config-distributor health..." -ForegroundColor Yellow
$configJob = Start-Job -ScriptBlock { kubectl port-forward -n cim deployment/unit-config-distributor 8093:8080 }
Start-Sleep -Seconds 3

try {
    $configHealth = Invoke-RestMethod -Uri "http://localhost:8093/health" -Method Get
    Write-Host "✓ Config-distributor is healthy" -ForegroundColor Green
    Write-Host "  Response: $($configHealth | ConvertTo-Json -Compress)" -ForegroundColor Gray

    # 3. Check tier config (this loads from Azure KV if configured)
    Write-Host "`n3. Testing tier config (loads from Azure KV)..." -ForegroundColor Yellow
    $tierStats = Invoke-RestMethod -Uri "http://localhost:8093/api/tier-config/stats" -Method Get
    Write-Host "✓ Tier config loaded successfully" -ForegroundColor Green
    Write-Host "  Source: $($tierStats.source)" -ForegroundColor Gray
    Write-Host "  Total units: $($tierStats.total_units)" -ForegroundColor Gray
    Write-Host "  T1: $($tierStats.tier_1_count), T2: $($tierStats.tier_2_count), T3: $($tierStats.tier_3_count)" -ForegroundColor Gray

    # 4. Test flag groups (feature flags stored in Azure KV)
    Write-Host "`n4. Testing feature flags..." -ForegroundColor Yellow
    $flags = Invoke-RestMethod -Uri "http://localhost:8093/api/tier-config/flags" -Method Get
    Write-Host "✓ Feature flags retrieved" -ForegroundColor Green
    $flags | ForEach-Object {
        Write-Host "  $($_.flag_group): $($_.enabled)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Test failed: $_" -ForegroundColor Red
} finally {
    Stop-Job -Job $configJob -ErrorAction SilentlyContinue
    Remove-Job -Job $configJob -ErrorAction SilentlyContinue
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pro testování gRPC endpointů použij grpcurl:" -ForegroundColor Yellow
Write-Host "  # Install grpcurl" -ForegroundColor Gray
Write-Host "  choco install grpcurl" -ForegroundColor Gray
Write-Host ""
Write-Host "  # Test GetSecretWithCredentials (direct Azure KV access)" -ForegroundColor Gray
Write-Host "  kubectl port-forward -n cim deployment/unit-vault-connector 5001:5000" -ForegroundColor Gray
Write-Host '  grpcurl -plaintext -d ''{\"secret_name\": \"cloudinframap-db-connection-string\", \"tenant_id\": \"system\"}'' localhost:5001 cloudinframap.protos.config.vault.VaultConnectorService/GetSecret' -ForegroundColor Gray
