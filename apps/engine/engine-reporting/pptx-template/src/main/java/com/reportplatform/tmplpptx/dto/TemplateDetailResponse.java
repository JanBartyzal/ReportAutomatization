package com.reportplatform.tmplpptx.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateDetailResponse(
    UUID id,
    String name,
    String description,
    String scope,
    String reportType,
    boolean active,
    String createdBy,
    Instant createdAt,
    Instant updatedAt,
    VersionInfo currentVersion,
    List<PlaceholderResponse> placeholders
) {
    public record VersionInfo(
        UUID id,
        int version,
        String blobUrl,
        Long fileSizeBytes,
        String uploadedBy,
        Instant uploadedAt
    ) {}
}
