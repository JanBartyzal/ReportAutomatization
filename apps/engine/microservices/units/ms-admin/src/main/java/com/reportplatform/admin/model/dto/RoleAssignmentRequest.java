package com.reportplatform.admin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoleAssignmentRequest(
        @NotBlank String targetUserId,
        @NotNull UUID orgId,
        @NotBlank String role
) {
}
