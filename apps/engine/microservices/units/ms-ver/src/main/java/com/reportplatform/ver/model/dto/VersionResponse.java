package com.reportplatform.ver.model.dto;

import com.reportplatform.ver.model.VersionEntity;

import java.time.Instant;
import java.util.UUID;

public record VersionResponse(
        UUID id,
        String entityType,
        UUID entityId,
        Integer versionNumber,
        Boolean locked,
        String createdBy,
        Instant createdAt,
        String reason
) {
    public static VersionResponse from(VersionEntity entity) {
        return new VersionResponse(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getVersionNumber(),
                entity.getLocked(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getReason()
        );
    }
}
