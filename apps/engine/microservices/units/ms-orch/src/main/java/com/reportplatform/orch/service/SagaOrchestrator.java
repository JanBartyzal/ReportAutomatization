package com.reportplatform.orch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the Saga pattern for distributed transactions.
 * <p>
 * Executes a sequence of {@link SagaStep} instances in order. If any step fails,
 * previously completed steps are compensated in reverse order to maintain consistency.
 * </p>
 */
@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

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

        public String workflowId() { return workflowId; }
        public String fileId() { return fileId; }
        public String fileType() { return fileType; }
        public String orgId() { return orgId; }

        public void put(String key, Object value) { attributes.put(key, value); }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) attributes.get(key);
        }

        public boolean has(String key) { return attributes.containsKey(key); }
    }

    /**
     * Result of a saga execution.
     */
    public sealed interface SagaResult {
        record Success(SagaContext context) implements SagaResult {}
        record Failure(String failedStep, Exception cause, List<String> compensatedSteps) implements SagaResult {}
    }

    /**
     * Executes all saga steps in order. On failure, compensates completed steps in reverse.
     *
     * @param steps   ordered list of saga steps
     * @param context shared context
     * @return the result indicating success or failure with compensation details
     */
    public SagaResult execute(List<SagaStep> steps, SagaContext context) {
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            try {
                log.info("Saga [{}]: executing step [{}]", context.workflowId(), step.name());
                step.execute(context);
                completedSteps.add(step);
                log.info("Saga [{}]: step [{}] completed", context.workflowId(), step.name());
            } catch (Exception ex) {
                log.error("Saga [{}]: step [{}] failed: {}",
                        context.workflowId(), step.name(), ex.getMessage(), ex);

                List<String> compensatedStepNames = compensate(completedSteps, context);
                return new SagaResult.Failure(step.name(), ex, compensatedStepNames);
            }
        }

        log.info("Saga [{}]: all {} steps completed successfully",
                context.workflowId(), steps.size());
        return new SagaResult.Success(context);
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
