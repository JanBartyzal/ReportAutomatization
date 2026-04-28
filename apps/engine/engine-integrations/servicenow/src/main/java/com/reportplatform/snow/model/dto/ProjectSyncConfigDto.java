package com.reportplatform.snow.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectSyncConfigDto(
        UUID id,
        UUID connectionId,
        UUID orgId,
        String syncScope,
        String filterManagerEmails,
        String budgetCurrency,
        BigDecimal ragAmberBudgetThreshold,
        BigDecimal ragRedBudgetThreshold,
        int ragAmberScheduleDays,
        int ragRedScheduleDays,
        boolean syncEnabled,
        Instant lastSyncedAt,
        Instant createdAt,
        Instant updatedAt
) {}
