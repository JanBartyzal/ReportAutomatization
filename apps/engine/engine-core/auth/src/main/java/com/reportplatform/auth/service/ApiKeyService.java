package com.reportplatform.auth.service;

import com.reportplatform.auth.model.ApiKeyEntity;
import com.reportplatform.auth.repository.AuthApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service("authApiKeyService")
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final int KEY_PREFIX_LENGTH = 8;

    private final AuthApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ApiKeyService(AuthApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    /**
     * Validates a raw API key against stored bcrypt hashes.
     * Keys are looked up by their prefix (first 8 chars) for efficient retrieval,
     * then the full key is verified against the stored hash.
     *
     * @param rawKey the raw API key from the request
     * @return the matching ApiKeyEntity if valid, empty otherwise
     */
    @Transactional
    public Optional<ApiKeyEntity> validateApiKey(String rawKey) {
        if (rawKey == null || rawKey.length() < KEY_PREFIX_LENGTH) {
            return Optional.empty();
        }

        String prefix = rawKey.substring(0, KEY_PREFIX_LENGTH);
        List<ApiKeyEntity> candidates = apiKeyRepository.findByKeyPrefixAndActiveTrue(prefix);

        for (ApiKeyEntity candidate : candidates) {
            if (candidate.isExpired()) {
                log.debug("API key {} is expired, skipping", candidate.getKeyName());
                continue;
            }

            if (passwordEncoder.matches(rawKey, candidate.getKeyHash())) {
                // Update last used timestamp
                candidate.setLastUsedAt(Instant.now());
                apiKeyRepository.save(candidate);

                log.info("API key validated: name={}, org={}",
                        candidate.getKeyName(), candidate.getOrganization().getId());
                return Optional.of(candidate);
            }
        }

        log.warn("API key validation failed for prefix: {}", prefix);
        return Optional.empty();
    }

    /**
     * Hashes a raw API key using bcrypt.
     * Used during key creation (not part of validation flow).
     */
    public String hashApiKey(String rawKey) {
        return passwordEncoder.encode(rawKey);
    }

    /**
     * Extracts the prefix from a raw key for storage.
     */
    public String extractPrefix(String rawKey) {
        if (rawKey == null || rawKey.length() < KEY_PREFIX_LENGTH) {
            throw new IllegalArgumentException("API key must be at least " + KEY_PREFIX_LENGTH + " characters");
        }
        return rawKey.substring(0, KEY_PREFIX_LENGTH);
    }
}
