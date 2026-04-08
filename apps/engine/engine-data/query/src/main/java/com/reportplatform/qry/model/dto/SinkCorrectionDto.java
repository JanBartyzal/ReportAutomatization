package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

public record SinkCorrectionDto(
        UUID id,
        UUID parsedTableId,
        Integer rowIndex,
        Integer colIndex,
        String originalValue,
        String correctedValue,
        String correctionType,
        String correctedBy,
        Instant correctedAt,
        Object metadata
) {
}
