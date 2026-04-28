package com.reportplatform.qry.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SnowProjectDto(
        UUID id,
        UUID resolverConnectionId,
        String sysId,
        String number,
        String shortDescription,
        String status,
        String phase,
        String managerName,
        String managerEmail,
        String department,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate projectedEndDate,
        BigDecimal percentComplete,
        BigDecimal totalBudget,
        BigDecimal actualCost,
        BigDecimal projectedCost,
        BigDecimal budgetUtilizationPct,
        Integer scheduleVarianceDays,
        BigDecimal milestoneCompletionRate,
        BigDecimal costForecastAccuracy,
        String ragStatus,
        Instant syncedAt,
        // Only populated in detail view
        List<SnowProjectTaskDto> tasks,
        List<SnowProjectBudgetDto> budgets
) {}
