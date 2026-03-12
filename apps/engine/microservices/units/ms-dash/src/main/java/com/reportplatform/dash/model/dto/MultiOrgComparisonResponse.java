package com.reportplatform.dash.model.dto;

import java.util.List;
import java.util.Map;

public record MultiOrgComparisonResponse(
        List<OrgComparisonRow> rows,
        Map<String, Object> metadata
) {
    public record OrgComparisonRow(
            String orgId,
            Map<String, Object> groupKey,
            double value,
            Double normalizedValue
    ) {}
}
