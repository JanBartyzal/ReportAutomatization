package com.reportplatform.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchOrgRequest(
    @NotBlank(message = "organizationId is required")
    String organizationId
) {
}
