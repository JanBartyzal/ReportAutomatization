package com.reportplatform.admin.model.dto;

import java.util.List;

/**
 * Complete health dashboard response, matching frontend HealthDashboard interface.
 */
public record HealthDashboardDTO(
        List<ServiceHealthDTO> services,
        SystemMetricsDTO metrics,
        List<ErrorLogEntryDTO> recentErrors,
        String grafanaUrl,
        String lastUpdated
) {}
