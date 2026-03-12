package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record PeriodComparisonRequest(
        @NotBlank(message = "Period 1 start date is required")
        String period1From,

        @NotBlank(message = "Period 1 end date is required")
        String period1To,

        @NotBlank(message = "Period 2 start date is required")
        String period2From,

        @NotBlank(message = "Period 2 end date is required")
        String period2To,

        @NotEmpty(message = "At least one groupBy field is required")
        List<String> groupBy,

        @NotBlank(message = "Aggregation function is required")
        @Pattern(regexp = "SUM|AVG|COUNT|MIN|MAX", message = "Invalid aggregation function")
        String aggregation,

        @NotBlank(message = "Value field is required")
        String valueField,

        @Pattern(regexp = "FILE|FORM|ALL", message = "Invalid source type")
        String sourceType
) {}
