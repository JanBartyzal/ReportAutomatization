package com.reportplatform.tmplpptx.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateUploadResponse(
    UUID id,
    String name,
    String scope,
    int version,
    String blobUrl,
    List<PlaceholderResponse> placeholders,
    Instant createdAt
) {}
