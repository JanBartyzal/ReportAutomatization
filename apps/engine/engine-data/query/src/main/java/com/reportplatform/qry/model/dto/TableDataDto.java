package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

public record TableDataDto(
        UUID id,
        String fileId,
        String sourceSheet,
        Object headers,
        Object rows,
        Object metadata,
        Instant createdAt
) {
}
