package com.reportplatform.audit.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAuditLogRequest(
        @NotNull UUID orgId,
        @NotBlank String userId,
        @NotBlank String action,
        @NotBlank String entityType,
        UUID entityId,
        JsonNode details,
        String ipAddress,
        String userAgent
) {}
