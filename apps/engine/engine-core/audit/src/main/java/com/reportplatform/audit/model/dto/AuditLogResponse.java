package com.reportplatform.audit.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.audit.model.AuditLogEntity;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID orgId,
        String userId,
        String action,
        String entityType,
        UUID entityId,
        JsonNode details,
        String ipAddress,
        Instant createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AuditLogResponse from(AuditLogEntity entity) {
        JsonNode detailsNode = null;
        if (entity.getDetails() != null) {
            try {
                detailsNode = MAPPER.readTree(entity.getDetails());
            } catch (Exception e) {
                // leave as null
            }
        }
        return new AuditLogResponse(
                entity.getId(),
                entity.getOrgId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                detailsNode,
                entity.getIpAddress(),
                entity.getCreatedAt()
        );
    }
}
