package com.reportplatform.tmplpptx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceholderMappingRequest(
    @NotEmpty @Valid List<MappingEntry> mappings
) {
    public record MappingEntry(
        @NotBlank String placeholderKey,
        @NotBlank String dataSourceType,
        @NotBlank String dataSourceRef,
        String transformExpression
    ) {}
}
