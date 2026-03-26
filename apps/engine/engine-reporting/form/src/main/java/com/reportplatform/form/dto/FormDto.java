package com.reportplatform.form.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reportplatform.form.model.FormEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FormDto(
        UUID id,
        String orgId,
        String title,
        String description,
        String scope,
        String status,
        String ownerOrgId,
        Instant releasedAt,
        String releasedBy,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        @JsonProperty("version") Integer latestVersion,
        List<FormFieldDefinition> fields
) {
    /**
     * Alias for title — some clients use "name" instead of "title".
     */
    @JsonProperty("name")
    public String name() {
        return title;
    }

    public static FormDto from(FormEntity entity, Integer latestVersion, List<FormFieldDefinition> fields) {
        return new FormDto(
                entity.getId(),
                entity.getOrgId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getScope(),
                entity.getStatus().name(),
                entity.getOwnerOrgId(),
                entity.getReleasedAt(),
                entity.getReleasedBy(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                latestVersion,
                fields
        );
    }

    public static FormDto from(FormEntity entity) {
        return from(entity, null, null);
    }
}
