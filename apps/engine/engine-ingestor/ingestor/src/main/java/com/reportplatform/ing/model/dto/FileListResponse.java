package com.reportplatform.ing.model.dto;

import java.util.List;

public record FileListResponse(
        List<FileDetailResponse> files,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
