package com.reportplatform.qry.model.dto;

import java.util.List;
import java.util.UUID;

public record TableQueryResponse(
        List<TableDataDto> tables,
        int page,
        int size,
        long totalElements,
        int totalPages,
        UUID nextCursor
) {
}
