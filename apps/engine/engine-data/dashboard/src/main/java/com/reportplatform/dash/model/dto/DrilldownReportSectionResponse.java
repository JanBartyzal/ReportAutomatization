package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DrilldownReportSectionResponse(
        UUID id,

        @JsonProperty("section_key")
        String sectionKey,

        String title,

        @JsonProperty("component_type")
        String componentType,

        @JsonProperty("source_type")
        String sourceType,

        @JsonProperty("source_ref_id")
        UUID sourceRefId,

        @JsonProperty("query_config")
        Object queryConfig,

        @JsonProperty("drill_config")
        Object drillConfig,

        @JsonProperty("display_order")
        int displayOrder
) {}
