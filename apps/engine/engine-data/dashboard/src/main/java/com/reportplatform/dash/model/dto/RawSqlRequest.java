package com.reportplatform.dash.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RawSqlRequest(
        @NotBlank(message = "SQL query is required")
        String sql
) {}