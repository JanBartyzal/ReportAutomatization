package com.reportplatform.form.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FormUpdateRequest(
        @Size(max = 500) @JsonAlias({"title", "name"}) String title,
        String description,
        String scope,
        @JsonAlias({"ownerOrgId", "owner_org_id"}) String ownerOrgId,
        List<FormFieldDefinition> fields
) {
}
