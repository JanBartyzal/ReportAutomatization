package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DashboardRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Config is required")
        Object config,

        @NotBlank(message = "Chart type is required")
        @Pattern(regexp = "bar|line|pie|heatmap|table", message = "Invalid chart type")
        String chartType,

        boolean isPublic
) {}
