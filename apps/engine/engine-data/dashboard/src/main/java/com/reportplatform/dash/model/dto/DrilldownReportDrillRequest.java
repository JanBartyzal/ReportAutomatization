package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record DrilldownReportDrillRequest(
        @JsonProperty("section_key")
        @NotBlank(message = "Section key is required")
        String sectionKey,

        Map<String, Object> filters,

        @JsonProperty("selected_value")
        Object selectedValue,

        Integer page,

        Integer size
) {}
