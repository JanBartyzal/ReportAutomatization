package com.reportplatform.tmplpptx.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaceholderMappingResponse(
    UUID templateId,
    List<MappingEntry> mappings
) {
    public record MappingEntry(
        UUID id,
        String placeholderKey,
        String dataSourceType,
        String dataSourceRef,
        String transformExpression,
        Instant updatedAt
    ) {}
}
