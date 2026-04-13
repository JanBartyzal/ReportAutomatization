package com.reportplatform.excelsync.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for resolving secrets from Azure Key Vault.
 * Falls back to an in-memory store when {@code AZURE_KEYVAULT_URI} is not set
 * (e.g., local development / testing).
 */
@Service
public class KeyVaultService {

    private static final Logger log = LoggerFactory.getLogger(KeyVaultService.class);

    private final SecretClient secretClient;
    private final ConcurrentMap<String, String> localStore;
    private final boolean useLocalFallback;

    public KeyVaultService(@Value("${azure.keyvault.uri:}") String keyVaultUri) {
        if (keyVaultUri != null && !keyVaultUri.isBlank()) {
            log.info("KeyVaultService: connecting to Azure Key Vault at {}", keyVaultUri);
            this.secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUri)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            this.localStore = null;
            this.useLocalFallback = false;
        } else {
            log.warn("KeyVaultService: AZURE_KEYVAULT_URI not set – using in-memory secret store " +
                    "(SharePoint connector will not work in production without real Key Vault)");
            this.secretClient = null;
            this.localStore = new ConcurrentHashMap<>();
            this.useLocalFallback = true;
        }
    }

    /**
     * Resolves a secret by name or by a {@code keyvault://secret-name} reference.
     *
     * @param secretRef secret name OR {@code keyvault://secret-name} URI
     * @return the plaintext secret value
     * @throws IllegalArgumentException if the secret is not found
     */
    public String getSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw new IllegalArgumentException("Secret reference must not be blank");
        }
        String name = secretRef.startsWith("keyvault://")
                ? secretRef.substring("keyvault://".length())
                : secretRef;

        if (useLocalFallback) {
            String value = localStore.get(name);
            if (value == null) {
                throw new IllegalArgumentException(
                        "Secret '" + name + "' not found in local store. " +
                        "Set it via KeyVaultService.setSecret() or configure AZURE_KEYVAULT_URI.");
            }
            log.debug("KeyVaultService: resolved '{}' from local store", name);
            return value;
        }

        log.debug("KeyVaultService: resolving '{}' from Azure Key Vault", name);
        return secretClient.getSecret(name).getValue();
    }

    /**
     * Store a secret in the in-memory store (local dev / testing only).
     * No-op when running against real Azure Key Vault.
     */
    public void setSecret(String name, String value) {
        if (useLocalFallback) {
            localStore.put(name, value);
            log.debug("KeyVaultService: stored '{}' in local store", name);
        } else {
            log.warn("KeyVaultService: setSecret() called in Key Vault mode – " +
                    "use Azure Portal to manage secrets");
        }
    }
}
