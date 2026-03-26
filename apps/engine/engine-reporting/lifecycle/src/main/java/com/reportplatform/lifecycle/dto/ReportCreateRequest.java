package com.reportplatform.lifecycle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ReportCreateRequest(
        @JsonProperty("org_id") @JsonAlias({"orgId", "org_id"}) String orgId,
        @JsonProperty("period_id") @JsonAlias({"periodId", "period_id"}) UUID periodId,
        @JsonProperty("report_type") @JsonAlias({"reportType", "report_type"}) String reportType,
        String scope
) {}
