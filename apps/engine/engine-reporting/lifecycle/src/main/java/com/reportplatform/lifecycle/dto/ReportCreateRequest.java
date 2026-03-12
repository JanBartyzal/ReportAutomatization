package com.reportplatform.lifecycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportCreateRequest(
        @NotBlank String orgId,
        @NotNull UUID periodId,
        @NotBlank String reportType,
        String scope
) {}
