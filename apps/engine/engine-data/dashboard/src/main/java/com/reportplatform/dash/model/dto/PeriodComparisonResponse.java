package com.reportplatform.dash.model.dto;

import java.util.List;
import java.util.Map;

public record PeriodComparisonResponse(List<PeriodComparisonRow> rows) {

    public record PeriodComparisonRow(
            Map<String, Object> groupKey,
            double period1Value,
            double period2Value,
            double absoluteDelta,
            double percentageDelta
    ) {}
}
