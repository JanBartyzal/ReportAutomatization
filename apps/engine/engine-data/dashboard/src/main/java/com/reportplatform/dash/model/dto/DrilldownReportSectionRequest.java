package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record DrilldownReportSectionRequest(
        @JsonProperty("section_key")
        @NotBlank(message = "Section key is required")
        String sectionKey,

        @NotBlank(message = "Title is required")
        String title,

        @JsonProperty("component_type")
        @Pattern(regexp = "KPI|CHART|TABLE|TEXT", message = "Invalid component type")
        String componentType,

        @JsonProperty("source_type")
        @Pattern(regexp = "DASHBOARD_WIDGET|NAMED_QUERY|SINK_SELECTION|REPORT_FORM|RAW_SQL|AGGREGATION",
                message = "Invalid source type")
        String sourceType,

        @JsonProperty("source_ref_id")
        UUID sourceRefId,

        @JsonProperty("query_config")
        Object queryConfig,

        @JsonProperty("drill_config")
        Object drillConfig,

        @JsonProperty("display_order")
        Integer displayOrder
) {}
