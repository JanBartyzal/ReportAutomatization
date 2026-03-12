package com.reportplatform.qry.model.dto;

import java.util.List;
import java.util.UUID;

public record FileDataResponse(
        UUID fileId,
        String filename,
        String mimeType,
        List<TableDataDto> tables,
        List<DocumentDto> documents
) {
}
