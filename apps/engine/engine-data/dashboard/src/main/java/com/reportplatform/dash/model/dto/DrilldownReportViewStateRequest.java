package com.reportplatform.dash.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record DrilldownReportViewStateRequest(
        @JsonProperty("view_state")
        Object viewState,

        @JsonProperty("expires_at")
        Instant expiresAt
) {}
