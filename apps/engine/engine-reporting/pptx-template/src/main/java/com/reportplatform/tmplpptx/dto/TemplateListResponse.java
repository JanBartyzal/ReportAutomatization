package com.reportplatform.tmplpptx.dto;

import java.time.Instant;
import java.util.UUID;

public record TemplateListResponse(
    UUID id,
    String name,
    String description,
    String scope,
    String reportType,
    int currentVersion,
    Instant createdAt,
    Instant updatedAt
) {}
