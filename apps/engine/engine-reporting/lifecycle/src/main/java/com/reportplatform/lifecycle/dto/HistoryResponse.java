package com.reportplatform.lifecycle.dto;

import com.reportplatform.lifecycle.model.ReportStatusHistoryEntity;

import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        UUID reportId,
        String fromStatus,
        String toStatus,
        String userId,
        String comment,
        Instant createdAt
) {
    public static HistoryResponse from(ReportStatusHistoryEntity entity) {
        return new HistoryResponse(
                entity.getId(),
                entity.getReportId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getUserId(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }
}
