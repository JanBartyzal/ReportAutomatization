package com.reportplatform.snow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpsertProjectSyncConfigRequest(
        /** ALL | ACTIVE_ONLY | BY_MANAGER */
        String syncScope,
        /** Comma-separated manager emails; required when syncScope=BY_MANAGER */
        String filterManagerEmails,
        @NotBlank @Size(min = 3, max = 3) String budgetCurrency,
        BigDecimal ragAmberBudgetThreshold,
        BigDecimal ragRedBudgetThreshold,
        Integer ragAmberScheduleDays,
        Integer ragRedScheduleDays,
        Boolean syncEnabled
) {}
