package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

public record SinkSelectionDto(
        UUID id,
        UUID parsedTableId,
        String periodId,
        String reportType,
        boolean selected,
        int priority,
        String selectedBy,
        Instant selectedAt,
        String note
) {
}
