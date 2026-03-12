package com.reportplatform.qry.model.dto;

import java.util.List;

public record ComparisonResult(
        String metric,
        String periodId,
        List<OrgMetricValue> values
) {}
