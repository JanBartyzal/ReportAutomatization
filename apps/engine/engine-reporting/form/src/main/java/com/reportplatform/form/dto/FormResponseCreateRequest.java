package com.reportplatform.form.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

public record FormResponseCreateRequest(
        @JsonProperty("org_id") @JsonAlias({"orgId", "org_id"}) String orgId,
        @JsonAlias({"periodId", "period_id"}) UUID periodId,
        Map<String, Object> data
) {
}
