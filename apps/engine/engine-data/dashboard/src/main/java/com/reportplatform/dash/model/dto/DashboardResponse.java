package com.reportplatform.dash.model.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardResponse(
        UUID id,
        UUID orgId,
        UUID createdBy,
        String name,
        String description,
        Object config,
        String chartType,
        boolean isPublic,
        Instant createdAt,
        Instant updatedAt
) {}
