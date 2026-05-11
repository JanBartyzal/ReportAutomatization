package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DrilldownReportViewStateResponse(
        UUID id,

        @JsonProperty("report_id")
        UUID reportId,

        @JsonProperty("view_state")
        Object viewState,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("expires_at")
        Instant expiresAt
) {}
