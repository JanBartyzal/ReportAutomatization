package com.reportplatform.lifecycle.dto;

import com.reportplatform.lifecycle.model.SubmissionChecklistEntity;

import java.util.UUID;

public record ChecklistResponse(
        UUID reportId,
        String checklistJson,
        int completedPct
) {
    public static ChecklistResponse from(SubmissionChecklistEntity entity) {
        return new ChecklistResponse(
                entity.getReportId(),
                entity.getChecklistJson(),
                entity.getCompletedPct()
        );
    }
}
