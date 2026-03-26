package com.reportplatform.form.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FormCreateRequest(
        @JsonProperty("org_id") @JsonAlias({"orgId", "org_id"}) String orgId,
        @Size(max = 500) @JsonAlias({"title", "name"}) String title,
        String description,
        String scope,
        @JsonProperty("owner_org_id") @JsonAlias({"ownerOrgId", "owner_org_id"}) String ownerOrgId,
        List<FormFieldDefinition> fields
) {
}
