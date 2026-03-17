package com.reportplatform.admin.model.dto;

/**
 * System-wide metrics, matching frontend SystemMetrics interface.
 */
public record SystemMetricsDTO(
        long activeWorkflows,
        long dlqDepth,
        long totalProcessed,
        long failedJobs,
        double avgProcessingTime
) {}
