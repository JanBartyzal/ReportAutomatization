package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

public record DashboardDataRequest(
        @NotEmpty(message = "At least one groupBy field is required")
        List<String> groupBy,

        @NotBlank(message = "Aggregation function is required")
        @Pattern(regexp = "SUM|AVG|COUNT|MIN|MAX", message = "Invalid aggregation function")
        String aggregation,

        @NotBlank(message = "Value field is required")
        String valueField,

        Map<String, String> filters,

        String dateFrom,

        String dateTo,

        @Pattern(regexp = "FILE|FORM|ALL", message = "Invalid source type")
        String sourceType
) {}
