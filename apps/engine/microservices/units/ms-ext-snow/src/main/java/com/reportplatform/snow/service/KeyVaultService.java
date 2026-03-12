package com.reportplatform.snow.service;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class KeyVaultService {

    private static final Logger logger = LoggerFactory.getLogger(KeyVaultService.class);

    private final SecretClient secretClient;
    private final ConcurrentMap<String, String> localSecretStore;
    private final boolean useLocalFallback;

    public KeyVaultService(@Value("${azure.keyvault.uri:}") String keyVaultUri) {
        if (keyVaultUri != null && !keyVaultUri.isBlank()) {
            logger.info("Initializing Azure KeyVault client with URI: {}", keyVaultUri);
            this.secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUri)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            this.localSecretStore = null;
            this.useLocalFallback = false;
        } else {
            logger.warn("Azure KeyVault URI not configured. Using in-memory secret store for local development.");
            this.secretClient = null;
            this.localSecretStore = new ConcurrentHashMap<>();
            this.useLocalFallback = true;
        }
    }

    /**
     * Retrieve a secret value by name.
     *
     * @param secretName the name of the secret
     * @return the secret value
     */
    public String getSecret(String secretName) {
        if (useLocalFallback) {
            logger.debug("Retrieving secret '{}' from in-memory store", secretName);
            String value = localSecretStore.get(secretName);
            if (value == null) {
                throw new IllegalArgumentException("Secret not found in local store: " + secretName);
            }
            return value;
        }

        logger.debug("Retrieving secret '{}' from Azure KeyVault", secretName);
        return secretClient.getSecret(secretName).getValue();
    }

    /**
     * Store a secret value.
     *
     * @param secretName the name of the secret
     * @param value      the secret value
     */
    public void setSecret(String secretName, String value) {
        if (useLocalFallback) {
            logger.debug("Storing secret '{}' in in-memory store", secretName);
            localSecretStore.put(secretName, value);
            return;
        }

        logger.debug("Storing secret '{}' in Azure KeyVault", secretName);
        secretClient.setSecret(secretName, value);
    }
}
