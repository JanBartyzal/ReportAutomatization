package com.reportplatform.orch.dto;

import java.util.List;

/**
 * Aggregated health metrics from the orchestrator's workflow and failed job data.
 * Consumed by engine-core admin module via Dapr service invocation.
 */
public record HealthMetricsResponse(
        long activeWorkflows,
        long dlqDepth,
        long totalProcessed,
        long failedJobs,
        double avgProcessingTimeMs,
        List<FailedJobSummary> recentErrors
) {

    /**
     * Lightweight summary of a failed job for the health dashboard error log.
     */
    public record FailedJobSummary(
            String id,
            String timestamp,
            String errorType,
            String errorDetail,
            String workflowId,
            int retryCount
    ) {}
}
