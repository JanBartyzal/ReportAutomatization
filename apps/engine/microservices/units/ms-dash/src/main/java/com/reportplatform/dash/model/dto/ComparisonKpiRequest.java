package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ComparisonKpiRequest(
        @NotBlank String name,
        String description,
        @NotBlank String valueField,
        @Pattern(regexp = "SUM|AVG|COUNT|MIN|MAX") String aggregation,
        @NotEmpty List<String> groupBy,
        @Pattern(regexp = "FILE|FORM|ALL") String sourceType,
        @Pattern(regexp = "NONE|MONTHLY|DAILY|ANNUAL") String normalization
) {}
