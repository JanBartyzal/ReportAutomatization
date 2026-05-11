package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

public record DrilldownReportQueryResponse(
        @JsonProperty("report_id")
        UUID reportId,
        Map<String, Object> filters,
        Map<String, Object> sections
) {}
