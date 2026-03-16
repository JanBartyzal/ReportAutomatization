package com.reportplatform.ing.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FileListResponse(
        List<FileDetailResponse> data,
        PaginationMeta pagination
) {

    public record PaginationMeta(
            int page,
            @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems,
            @JsonProperty("total_pages") int totalPages
    ) {
    }
}
