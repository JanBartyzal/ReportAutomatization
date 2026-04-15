package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary item for sink browser list view.
 */
public record SinkListItemDto(
        UUID id,
        String fileId,
        String filename,
        String sourceSheet,
        int rowCount,
        int columnCount,
        Object metadata,
        Instant createdAt,
        long correctionCount,
        boolean hasSelections
) {
}
