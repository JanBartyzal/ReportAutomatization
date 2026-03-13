package com.reportplatform.orch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the Saga pattern for distributed transactions with exponential
 * backoff retry.
 * <p>
 * Executes a sequence of {@link SagaStep} instances in order. If any step
 * fails,
 * previously completed steps are compensated in reverse order to maintain
 * consistency.
 * Supports exponential backoff retry for transient failures.
 * </p>
 */
@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final int maxRetryCount;
    private final List<Long> retryDelaysMs;
    private final long compensateTimeoutMs;

    public SagaOrchestrator(
            @Value("${workflow.saga.max-retry-count:3}") int maxRetryCount,
            @Value("${workflow.saga.retry-delays-ms:1000,5000,30000}") String retryDelaysMs,
            @Value("${workflow.saga.compensate-timeout-ms:30000}") long compensateTimeoutMs) {
        this.maxRetryCount = maxRetryCount;
        this.retryDelaysMs = parseDelays(retryDelaysMs);
        this.compensateTimeoutMs = compensateTimeoutMs;
        log.info("SagaOrchestrator initialized with maxRetryCount={}, retryDelays={}ms, compensateTimeout={}ms",
                maxRetryCount, this.retryDelaysMs, compensateTimeoutMs);
    }

    private List<Long> parseDelays(String delays) {
        List<Long> result = new ArrayList<>();
        if (delays != null && !delays.isBlank()) {
            for (String part : delays.split(",")) {
                try {
                    result.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid retry delay: {}, using default", part);
                }
            }
        }
        if (result.isEmpty()) {
            result.add(1000L); // Default 1 second
        }
        return result;
    }

    /**
     * Represents a single step in a saga with execute and compensate actions.
     */
    public interface SagaStep {

        /**
         * @return human-readable name for logging
         */
        String name();

        /**
         * Executes the forward action of this step.
         *
         * @param context mutable context shared across all steps
         * @throws Exception if the step fails
         */
        void execute(SagaContext context) throws Exception;

        /**
         * Compensates (rolls back) the action performed by {@link #execute}.
         * Implementations must be idempotent.
         *
         * @param context mutable context shared across all steps
         */
        void compensate(SagaContext context);
    }

    /**
     * Mutable context bag passed through all saga steps.
     */
    public static class SagaContext {

        private final String workflowId;
        private final String fileId;
        private final String fileType;
        private final String orgId;
        private final java.util.Map<String, Object> attributes = new java.util.concurrent.ConcurrentHashMap<>();

        public SagaContext(String workflowId, String fileId, String fileType, String orgId) {
            this.workflowId = workflowId;
            this.fileId = fileId;
            this.fileType = fileType;
            this.orgId = orgId;
        }

        public String workflowId() {
            return workflowId;
        }

        public String fileId() {
            return fileId;
        }

        public String fileType() {
            return fileType;
        }

        public String orgId() {
            return orgId;
        }

        public void put(String key, Object value) {
            attributes.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) attributes.get(key);
        }

        public boolean has(String key) {
            return attributes.containsKey(key);
        }
    }

    /**
     * Result of a saga execution.
     */
    public sealed

    interface SagaResult {
        record Success(SagaContext context) implements SagaResult {}

        record Failure(String failedStep, Exception cause, List<String> compensatedSteps) implements SagaResult {}
    }

    /**
     * Executes all saga steps in order with exponential backoff retry.
     * On failure, compensates completed steps in reverse.
     *
     * @param steps   ordered list of saga steps
     * @param context shared context
     * @return the result indicating success or failure with compensation details
     */
    public SagaResult execute(List<SagaStep> steps, SagaContext context) {
        List<SagaStep> completedSteps = new ArrayList<>();

        for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
            SagaStep step = steps.get(stepIndex);
            int attempt = 0;
            boolean stepCompleted = false;

            while (attempt <= maxRetryCount && !stepCompleted) {
                try {
                    if (attempt > 0) {
                        // Exponential backoff before retry
                        long delay = getRetryDelay(attempt - 1);
                        log.info("Saga [{}]: retrying step [{}], attempt {}/{}, waiting {}ms",
                                context.workflowId(), step.name(), attempt, maxRetryCount, delay);
                        Thread.sleep(delay);
                    }

                    log.info("Saga [{}]: executing step [{}] (attempt {}/{})",
                            context.workflowId(), step.name(), attempt + 1, maxRetryCount + 1);
                    step.execute(context);
                    completedSteps.add(step);
                    stepCompleted = true;
                    log.info("Saga [{}]: step [{}] completed successfully", context.workflowId(), step.name());
                } catch (Exception ex) {
                    attempt++;
                    if (attempt > maxRetryCount) {
                        log.error("Saga [{}]: step [{}] failed after {} attempts: {}",
                                context.workflowId(), step.name(), maxRetryCount + 1, ex.getMessage(), ex);

                        List<String> compensatedStepNames = compensate(completedSteps, context);
                        return new SagaResult.Failure(step.name(), ex, compensatedStepNames);
                    } else {
                        log.warn("Saga [{}]: step [{}] failed (attempt {}/{}): {}",
                                context.workflowId(), step.name(), attempt, maxRetryCount + 1, ex.getMessage());
                    }
                }
            }
        }

        log.info("Saga [{}]: all {} steps completed successfully",
                context.workflowId(), steps.size());
        return new SagaResult.Success(context);
    }

        /**
         * Gets the retry delay for a given attempt using exponential backoff.
         *
         * @param attemptIndex the 0-based index of the attempt (0 = first retry)
         * @return delay in milliseconds
         */
        private long getRetryDelay(int attemptIndex) {
            if (attemptIndex < retryDelaysMs.size()) {
                return retryDelaysMs.get(attemptIndex);
            }
            // Use last configured delay for attempts beyond configured delays
            return retryDelaysMs.get(retryDelaysMs.size() - 1);
        }

        private List<String> compensate(List<SagaStep> completedSteps, SagaContext context) {
            List<SagaStep> reversed = new ArrayList<>(completedSteps);
            Collections.reverse(reversed);

            List<String> compensated = new ArrayList<>();
            for (SagaStep step : reversed) {
                try {
                    log.info("Saga [{}]: compensating step [{}]", context.workflowId(), step.name());
                    step.compensate(context);
                    compensated.add(step.name());
                    log.info("Saga [{}]: step [{}] compensated", context.workflowId(), step.name());
                } catch (Exception ex) {
                    log.error("Saga [{}]: CRITICAL - compensation failed for step [{}]: {}",
                            context.workflowId(), step.name(), ex.getMessage(), ex);
                    compensated.add(step.name() + " (FAILED)");
                }
            }
            return compensated;
        }
}
