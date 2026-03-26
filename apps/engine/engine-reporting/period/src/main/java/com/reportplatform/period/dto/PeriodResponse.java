package com.reportplatform.period.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** Alias: some clients use "state" instead of "status". */
    @JsonProperty("state")
    public String state() {
        return status;
    }

    public static PeriodResponse from(PeriodEntity entity) {
        return new PeriodResponse(
                entity.getId(),
                entity.getName(),
                entity.getPeriodType() != null ? entity.getPeriodType().name() : "QUARTERLY",
                entity.getPeriodCode(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getSubmissionDeadline(),
                entity.getReviewDeadline(),
                entity.getStatus() != null ? entity.getStatus().name() : "OPEN",
                entity.getHoldingId(),
                entity.getClonedFromId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
