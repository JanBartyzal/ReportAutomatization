package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

public record ProcessingLogDto(
        UUID id,
        String fileId,
        String workflowId,
        String stepName,
        String status,
        Long durationMs,
        String errorDetail,
        Instant createdAt
) {
}
