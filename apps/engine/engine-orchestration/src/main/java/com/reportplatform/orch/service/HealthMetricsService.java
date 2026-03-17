package com.reportplatform.orch.service;

import com.reportplatform.orch.dto.HealthMetricsResponse;
import com.reportplatform.orch.dto.HealthMetricsResponse.FailedJobSummary;
import com.reportplatform.orch.model.FailedJobEntity;
import com.reportplatform.orch.repository.FailedJobRepository;
import com.reportplatform.orch.repository.WorkflowHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides aggregated health metrics from workflow_history and failed_jobs tables.
 */
@Service
public class HealthMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(HealthMetricsService.class);

    private static final List<String> ACTIVE_STATUSES = List.of(
            "RECEIVED", "SCANNING", "PARSING", "MAPPING", "STORING", "AWAITING_CONFIRMATION"
    );

    private final WorkflowHistoryRepository workflowHistoryRepository;
    private final FailedJobRepository failedJobRepository;

    public HealthMetricsService(WorkflowHistoryRepository workflowHistoryRepository,
                                FailedJobRepository failedJobRepository) {
        this.workflowHistoryRepository = workflowHistoryRepository;
        this.failedJobRepository = failedJobRepository;
    }

    public HealthMetricsResponse getMetrics() {
        long activeWorkflows = workflowHistoryRepository.countByStatusIn(ACTIVE_STATUSES);
        long totalProcessed = workflowHistoryRepository.countByStatus("COMPLETED");
        long failedJobs = failedJobRepository.count();
        long dlqDepth = failedJobRepository.countByRetryCountGreaterThanEqual(3);

        Double avgTime = workflowHistoryRepository.averageProcessingTimeMs();
        double avgProcessingTimeMs = avgTime != null ? avgTime : 0.0;

        List<FailedJobEntity> recentFailures = failedJobRepository.findTop20ByOrderByFailedAtDesc();
        List<FailedJobSummary> recentErrors = recentFailures.stream()
                .map(job -> new FailedJobSummary(
                        job.getId().toString(),
                        job.getFailedAt().toString(),
                        job.getErrorType(),
                        job.getErrorDetail(),
                        job.getWorkflowId(),
                        job.getRetryCount()
                ))
                .toList();

        logger.debug("Health metrics: active={}, completed={}, failed={}, dlq={}",
                activeWorkflows, totalProcessed, failedJobs, dlqDepth);

        return new HealthMetricsResponse(
                activeWorkflows, dlqDepth, totalProcessed, failedJobs,
                avgProcessingTimeMs, recentErrors
        );
    }
}
