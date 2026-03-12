package com.reportplatform.admin.model.dto;

import java.time.Instant;
import java.util.UUID;

public class ApiKeyCreatedResponse {
    private UUID keyId;
    private String key;
    private Instant createdAt;

    public ApiKeyCreatedResponse() {
    }

    public ApiKeyCreatedResponse(UUID keyId, String key, Instant createdAt) {
        this.keyId = keyId;
        this.key = key;
        this.createdAt = createdAt;
    }

    public UUID getKeyId() {
        return keyId;
    }

    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
