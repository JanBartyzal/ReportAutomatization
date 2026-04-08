package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full detail of a sink (parsed table) with applied corrections and selection state.
 */
public record SinkDetailDto(
        UUID id,
        String fileId,
        String sourceSheet,
        Object headers,
        Object rows,
        Object metadata,
        Instant createdAt,
        int correctionCount,
        List<SinkCorrectionDto> corrections,
        List<SinkSelectionDto> selections,
        Object correctedHeaders,
        Object correctedRows
) {
}
