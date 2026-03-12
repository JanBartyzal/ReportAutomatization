package com.reportplatform.lifecycle.dto;

import com.reportplatform.lifecycle.config.ReportScope;
import com.reportplatform.lifecycle.config.ReportState;
import com.reportplatform.lifecycle.model.ReportEntity;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String orgId,
        UUID periodId,
        String reportType,
        ReportState status,
        ReportScope scope,
        boolean locked,
        String submittedBy,
        Instant submittedAt,
        String reviewedBy,
        Instant reviewedAt,
        String approvedBy,
        Instant approvedAt,
        String completedBy,
        Instant completedAt,
        String releasedBy,
        Instant releasedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReportResponse from(ReportEntity entity) {
        return new ReportResponse(
                entity.getId(),
                entity.getOrgId(),
                entity.getPeriodId(),
                entity.getReportType(),
                entity.getStatus(),
                entity.getScope(),
                entity.isLocked(),
                entity.getSubmittedBy(),
                entity.getSubmittedAt(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getCompletedBy(),
                entity.getCompletedAt(),
                entity.getReleasedBy(),
                entity.getReleasedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
