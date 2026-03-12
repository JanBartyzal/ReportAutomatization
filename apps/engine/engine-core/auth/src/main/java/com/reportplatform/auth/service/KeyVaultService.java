package com.reportplatform.auth.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads secrets from Azure Key Vault via Managed Identity (MSI).
 * Falls back to environment variables for local development.
 */
@Service
public class KeyVaultService {

    private static final Logger log = LoggerFactory.getLogger(KeyVaultService.class);

    @Value("${azure.keyvault.uri:}")
    private String keyVaultUri;

    @Value("${azure.keyvault.enabled:false}")
    private boolean keyVaultEnabled;

    private SecretClient secretClient;
    private final Map<String, CachedSecret> secretCache = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 300_000; // 5 minutes

    @PostConstruct
    void init() {
        if (keyVaultEnabled && keyVaultUri != null && !keyVaultUri.isBlank()) {
            try {
                secretClient = new SecretClientBuilder()
                        .vaultUrl(keyVaultUri)
                        .credential(new DefaultAzureCredentialBuilder().build())
                        .buildClient();
                log.info("Azure Key Vault client initialized for: {}", keyVaultUri);
            } catch (Exception e) {
                log.warn("Failed to initialize Key Vault client, falling back to env vars: {}", e.getMessage());
                secretClient = null;
            }
        } else {
            log.info("Azure Key Vault is disabled, using environment variables for secrets");
        }
    }

    /**
     * Retrieves a secret by name. Checks cache first, then Key Vault, then env vars.
     *
     * @param secretName the secret name (Key Vault name or env var name)
     * @return the secret value, or empty if not found
     */
    public Optional<String> getSecret(String secretName) {
        // Check cache
        CachedSecret cached = secretCache.get(secretName);
        if (cached != null && !cached.isExpired()) {
            return Optional.ofNullable(cached.value());
        }

        // Try Key Vault
        if (secretClient != null) {
            try {
                String value = secretClient.getSecret(secretName).getValue();
                cacheSecret(secretName, value);
                return Optional.ofNullable(value);
            } catch (Exception e) {
                log.warn("Failed to read secret '{}' from Key Vault: {}", secretName, e.getMessage());
            }
        }

        // Fallback to environment variable
        String envVarName = secretName.replace("-", "_").toUpperCase();
        String envValue = System.getenv(envVarName);
        if (envValue != null) {
            cacheSecret(secretName, envValue);
            return Optional.of(envValue);
        }

        log.debug("Secret '{}' not found in Key Vault or environment", secretName);
        return Optional.empty();
    }

    /**
     * Retrieves a secret, throwing an exception if not found.
     */
    public String getSecretOrThrow(String secretName) {
        return getSecret(secretName)
                .orElseThrow(() -> new IllegalStateException(
                        "Required secret not found: " + secretName));
    }

    private void cacheSecret(String name, String value) {
        secretCache.put(name, new CachedSecret(value, System.currentTimeMillis() + CACHE_TTL_MS));
    }

    /**
     * Evicts a specific secret from the cache.
     */
    public void evict(String secretName) {
        secretCache.remove(secretName);
    }

    /**
     * Clears the entire secret cache.
     */
    public void clearCache() {
        secretCache.clear();
    }

    private record CachedSecret(String value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
