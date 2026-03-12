package com.reportplatform.qry.model.dto;

public record OrgMetricValue(
        String orgId,
        String orgName,
        double value,
        String scope
) {}
