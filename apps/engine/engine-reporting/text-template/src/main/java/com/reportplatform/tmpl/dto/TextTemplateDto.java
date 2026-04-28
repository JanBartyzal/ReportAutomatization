package com.reportplatform.tmpl.dto;

import java.time.Instant;
import java.util.UUID;

/** Read DTO for a text template (without full content for list views). */
public record TextTemplateDto(
        UUID id,
        UUID orgId,
        String name,
        String description,
        String templateType,
        String content,
        String outputFormats,
        String dataBindings,
        String scope,
        boolean system,
        boolean active,
        int version,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
