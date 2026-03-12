package com.reportplatform.period.dto;

import java.util.List;
import java.util.UUID;

public record PeriodStatusResponse(
        UUID periodId,
        String periodName,
        int totalOrgs,
        int totalReports,
        int approvedReports,
        int completionPct,
        List<OrgStatusEntry> orgStatuses
) {
    public record OrgStatusEntry(
            String orgId,
            String status,
            int reportCount
    ) {}
}
