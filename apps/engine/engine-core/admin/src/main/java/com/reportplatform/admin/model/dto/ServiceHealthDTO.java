package com.reportplatform.admin.model.dto;

/**
 * Health status of an individual service, matching frontend ServiceHealth interface.
 */
public record ServiceHealthDTO(
        String id,
        String name,
        String status,
        String lastCheck,
        long responseTime,
        double uptime,
        String version,
        int errorCount
) {}
