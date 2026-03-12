package com.reportplatform.form.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record FormUpdateRequest(
        @Size(max = 500) String title,
        String description,
        String scope,
        String ownerOrgId,
        List<FormFieldDefinition> fields
) {
}
