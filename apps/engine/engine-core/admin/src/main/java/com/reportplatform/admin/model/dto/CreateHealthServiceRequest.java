package com.reportplatform.admin.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating a health service registry entry.
 */
public record CreateHealthServiceRequest(
        @NotBlank String serviceId,
        @NotBlank String displayName,
        @NotBlank String healthUrl,
        boolean enabled,
        int sortOrder
) {}
