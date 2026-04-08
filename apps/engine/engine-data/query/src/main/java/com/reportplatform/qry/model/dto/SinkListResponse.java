package com.reportplatform.qry.model.dto;

import java.util.List;
import java.util.UUID;

public record SinkListResponse(
        List<SinkListItemDto> sinks,
        int page,
        int size,
        long totalElements,
        int totalPages,
        UUID nextCursor
) {
}
