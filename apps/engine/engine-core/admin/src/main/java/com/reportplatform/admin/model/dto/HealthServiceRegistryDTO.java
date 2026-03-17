package com.reportplatform.admin.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a health service registry entry.
 */
public record HealthServiceRegistryDTO(
        UUID id,
        String serviceId,
        String displayName,
        String healthUrl,
        boolean enabled,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
