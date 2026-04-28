package com.reportplatform.snow.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Read DTO for a resolver group configuration. */
public record ResolverGroupDto(
        UUID id,
        UUID connectionId,
        UUID orgId,
        String groupSysId,
        String groupName,
        String dataTypes,
        boolean syncEnabled,
        Instant lastSyncedAt,
        Instant createdAt
) {}
