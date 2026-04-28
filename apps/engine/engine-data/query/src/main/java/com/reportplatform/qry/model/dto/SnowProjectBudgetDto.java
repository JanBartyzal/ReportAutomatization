package com.reportplatform.qry.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SnowProjectBudgetDto(
        UUID id,
        String sysId,
        String category,
        String fiscalYear,
        BigDecimal plannedAmount,
        BigDecimal actualAmount
) {}
