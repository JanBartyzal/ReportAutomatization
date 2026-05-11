package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DrilldownReportRequest(
        @NotBlank(message = "Name is required")
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

        @Valid
        List<DrilldownReportSectionRequest> sections
) {}
