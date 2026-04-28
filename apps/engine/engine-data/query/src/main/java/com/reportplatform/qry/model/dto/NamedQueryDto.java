package com.reportplatform.qry.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Read DTO for a Named Query catalog entry. */
public record NamedQueryDto(
        UUID id,
        UUID orgId,
        String name,
        String description,
        String sqlQuery,
        String paramsSchema,
        String dataSourceHint,
        boolean system,
        boolean active,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
