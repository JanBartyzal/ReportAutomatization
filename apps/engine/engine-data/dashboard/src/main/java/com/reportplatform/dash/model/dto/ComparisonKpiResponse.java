package com.reportplatform.dash.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComparisonKpiResponse(
        UUID id,
        String name,
        String description,
        String valueField,
        String aggregation,
        List<String> groupBy,
        String sourceType,
        String normalization,
        boolean active,
        Instant createdAt
) {}
