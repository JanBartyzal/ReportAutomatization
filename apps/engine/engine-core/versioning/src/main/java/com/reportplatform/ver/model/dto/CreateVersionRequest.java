package com.reportplatform.ver.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVersionRequest(
        @NotBlank String entityType,
        @NotNull UUID entityId,
        @NotNull JsonNode snapshotData,
        String reason,
        @NotBlank String createdBy
) {}
