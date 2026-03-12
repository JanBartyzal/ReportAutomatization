package com.reportplatform.audit.model.dto;

import com.reportplatform.audit.model.ReadAccessLogEntity;

import java.time.Instant;
import java.util.UUID;

public record ReadAccessLogResponse(
        UUID id,
        String userId,
        UUID documentId,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
    public static ReadAccessLogResponse from(ReadAccessLogEntity entity) {
        return new ReadAccessLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getDocumentId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCreatedAt()
        );
    }
}
