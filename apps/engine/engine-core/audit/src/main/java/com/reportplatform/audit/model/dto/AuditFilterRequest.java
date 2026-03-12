package com.reportplatform.audit.model.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditFilterRequest(
        String userId,
        String action,
        String entityType,
        UUID entityId,
        Instant dateFrom,
        Instant dateTo
) {}
