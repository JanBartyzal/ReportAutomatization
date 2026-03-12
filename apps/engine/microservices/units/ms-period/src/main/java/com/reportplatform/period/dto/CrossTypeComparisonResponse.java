package com.reportplatform.period.dto;

import java.time.LocalDate;
import java.util.List;

public record CrossTypeComparisonResponse(
        List<PeriodInfo> periods,
        List<NormalizationInfo> normalizations
) {
    public record PeriodInfo(
            String id,
            String name,
            String periodType,
            String periodCode,
            LocalDate startDate,
            LocalDate endDate,
            long durationDays,
            double monthlyNormalizationFactor,
            double dailyNormalizationFactor
    ) {}

    public record NormalizationInfo(
            String fromPeriodId,
            String toPeriodId,
            double durationRatio,
            String hint
    ) {}
}
