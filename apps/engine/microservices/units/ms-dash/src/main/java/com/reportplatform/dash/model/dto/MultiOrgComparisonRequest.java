package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record MultiOrgComparisonRequest(
        @NotEmpty List<String> orgIds,
        @NotEmpty List<String> groupBy,
        @NotBlank @Pattern(regexp = "SUM|AVG|COUNT|MIN|MAX") String aggregation,
        @NotBlank String valueField,
        String dateFrom,
        String dateTo,
        @Pattern(regexp = "FILE|FORM|ALL") String sourceType,
        @Pattern(regexp = "NONE|MONTHLY|DAILY|ANNUAL") String normalization
) {}
