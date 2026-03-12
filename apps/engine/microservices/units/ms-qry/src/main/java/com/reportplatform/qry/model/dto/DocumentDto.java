package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        String fileId,
        String documentType,
        Object content,
        Object metadata,
        Instant createdAt
) {
}
