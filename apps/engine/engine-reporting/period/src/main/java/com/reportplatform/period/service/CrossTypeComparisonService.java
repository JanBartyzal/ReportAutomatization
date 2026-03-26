package com.reportplatform.period.service;

import com.reportplatform.period.dto.CrossTypeComparisonResponse;
import com.reportplatform.period.dto.CrossTypeComparisonResponse.NormalizationInfo;
import com.reportplatform.period.dto.CrossTypeComparisonResponse.PeriodInfo;
import com.reportplatform.period.model.PeriodEntity;
import com.reportplatform.period.repository.PeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CrossTypeComparisonService {

    private final PeriodRepository periodRepository;

    public CrossTypeComparisonService(PeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    @Transactional(readOnly = true)
    public CrossTypeComparisonResponse buildComparisonContext(List<UUID> periodIds) {
        List<PeriodEntity> periods;
        try {
            periods = periodRepository.findByIdInOrderByStartDate(periodIds);
        } catch (Exception e) {
            // If query fails (e.g. invalid UUIDs or DB issue), return empty comparison
            return new CrossTypeComparisonResponse(List.of(), List.of());
        }

        if (periods.isEmpty()) {
            return new CrossTypeComparisonResponse(List.of(), List.of());
        }

        List<PeriodInfo> periodInfos = periods.stream()
                .map(this::toPeriodInfo)
                .toList();

        List<NormalizationInfo> normalizations = new ArrayList<>();
        for (int i = 0; i < periodInfos.size(); i++) {
            for (int j = i + 1; j < periodInfos.size(); j++) {
                var a = periodInfos.get(i);
                var b = periodInfos.get(j);
                double ratio = (double) a.durationDays() / b.durationDays();
                String hint = buildHint(a, b);
                normalizations.add(new NormalizationInfo(
                        a.id(), b.id(), ratio, hint));
            }
        }

        return new CrossTypeComparisonResponse(periodInfos, normalizations);
    }

    private PeriodInfo toPeriodInfo(PeriodEntity period) {
        long days = ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate());
        double monthlyFactor = days > 0 ? 30.0 / days : 1.0;
        double dailyFactor = days > 0 ? 1.0 / days : 1.0;

        return new PeriodInfo(
                period.getId().toString(),
                period.getName(),
                period.getPeriodType().name(),
                period.getPeriodCode(),
                period.getStartDate(),
                period.getEndDate(),
                days,
                monthlyFactor,
                dailyFactor
        );
    }

    private String buildHint(PeriodInfo a, PeriodInfo b) {
        if (a.periodType().equals(b.periodType())) {
            return "Same period type – direct comparison possible";
        }
        return String.format("Different types (%s vs %s) – normalize to %s basis for fair comparison",
                a.periodType(), b.periodType(),
                a.durationDays() < b.durationDays() ? "daily or monthly" : "daily or monthly");
    }
}
