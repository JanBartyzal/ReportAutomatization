package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DrilldownReportResponse(
        UUID id,

        @JsonProperty("org_id")
        UUID orgId,

        String name,

        String description,

        @JsonProperty("report_type")
        String reportType,

        @JsonProperty("base_period_type")
        String basePeriodType,

        @JsonProperty("default_filters")
        Object defaultFilters,

        @JsonProperty("layout_config")
        Object layoutConfig,

        @JsonProperty("is_public")
        boolean isPublic,

        @JsonProperty("created_by")
        UUID createdBy,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt,

        List<DrilldownReportSectionResponse> sections
) {}
