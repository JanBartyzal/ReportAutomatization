package com.reportplatform.period.dto;

import com.reportplatform.period.model.PeriodEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PeriodResponse(
        UUID id,
        String name,
        String periodType,
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        Instant submissionDeadline,
        Instant reviewDeadline,
        String status,
        String holdingId,
        UUID clonedFromId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static PeriodResponse from(PeriodEntity entity) {
        return new PeriodResponse(
                entity.getId(),
                entity.getName(),
                entity.getPeriodType().name(),
                entity.getPeriodCode(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getSubmissionDeadline(),
                entity.getReviewDeadline(),
                entity.getStatus().name(),
                entity.getHoldingId(),
                entity.getClonedFromId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
