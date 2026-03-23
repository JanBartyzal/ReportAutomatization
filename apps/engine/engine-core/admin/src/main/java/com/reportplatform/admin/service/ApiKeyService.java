package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.ApiKeyCreatedResponse;
import com.reportplatform.admin.model.dto.ApiKeyDTO;
import com.reportplatform.admin.model.dto.CreateApiKeyRequest;
import com.reportplatform.admin.model.entity.ApiKeyEntity;
import com.reportplatform.admin.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("adminApiKeyService")
@Transactional
public class ApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);
    private static final int API_KEY_LENGTH = 32;

    private final ApiKeyRepository apiKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDTO> getAllApiKeys() {
        return apiKeyRepository.findAllNonRevoked().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ApiKeyCreatedResponse createApiKey(CreateApiKeyRequest request, String createdBy) {
        // Generate the raw API key (shown only once)
        String rawKey = generateApiKey();

        // Hash the key for storage
        String keyHash = passwordEncoder.encode(rawKey);

        // Key prefix = first 8 chars, used for efficient lookup
        String keyPrefix = rawKey.substring(0, Math.min(8, rawKey.length()));

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(request.getName());
        apiKey.setScopes(request.getScopes() != null ? request.getScopes() : new String[0]);
        apiKey.setCreatedBy(createdBy);
        apiKey.setCreatedAt(Instant.now());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setRevoked(false);

        // Set organization if provided
        if (request.getOrgId() != null) {
            var orgRef = new com.reportplatform.admin.model.entity.OrganizationEntity();
            orgRef.setId(request.getOrgId());
            apiKey.setOrganization(orgRef);
        }

        ApiKeyEntity saved = apiKeyRepository.save(apiKey);

        logger.info("Created API key: {} ({}) for user: {}", saved.getName(), saved.getId(), createdBy);

        // Return the raw key only once
        return new ApiKeyCreatedResponse(saved.getId(), rawKey, saved.getCreatedAt());
    }

    public void revokeApiKey(UUID keyId) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        apiKey.setRevoked(true);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        logger.info("Revoked API key: {}", keyId);
    }

    public boolean validateApiKey(String rawKey) {
        List<ApiKeyEntity> activeKeys = apiKeyRepository.findActiveKeys();

        for (ApiKeyEntity key : activeKeys) {
            if (passwordEncoder.matches(rawKey, key.getKeyHash())) {
                // Update last used
                key.setLastUsedAt(Instant.now());
                apiKeyRepository.save(key);
                return true;
            }
        }

        return false;
    }

    private ApiKeyDTO toDTO(ApiKeyEntity entity) {
        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setKeyId(entity.getId());
        dto.setName(entity.getName());
        dto.setScopes(entity.getScopes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastUsedAt(entity.getLastUsedAt());
        return dto;
    }

    private String generateApiKey() {
        byte[] keyBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}
