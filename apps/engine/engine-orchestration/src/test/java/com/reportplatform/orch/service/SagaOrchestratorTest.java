package com.reportplatform.orch.service;

import com.reportplatform.orch.service.SagaOrchestrator.SagaContext;
import com.reportplatform.orch.service.SagaOrchestrator.SagaResult;
import com.reportplatform.orch.service.SagaOrchestrator.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SagaOrchestrator with exponential backoff retry.
 */
class SagaOrchestratorTest {

    private SagaOrchestrator sagaOrchestrator;

    @BeforeEach
    void setUp() {
        // Use small delays for testing (10ms, 20ms)
        sagaOrchestrator = new SagaOrchestrator(3, "10,20,30", 5000);
    }

    @Test
    @DisplayName("Should execute all steps successfully on first attempt")
    void shouldExecuteAllStepsSuccessfully() {
        // Given
        List<String> executionOrder = new ArrayList<>();
        List<SagaStep> steps = List.of(
                createStep("step1", executionOrder),
                createStep("step2", executionOrder),
                createStep("step3", executionOrder));
        SagaContext context = new SagaContext("workflow-1", "file-1", "PPTX", "org-1");

        // When
        SagaResult result = sagaOrchestrator.execute(steps, context);

        // Then
        assertTrue(result instanceof SagaResult.Success);
        assertEquals(3, executionOrder.size());
        assertEquals("step1", executionOrder.get(0));
        assertEquals("step2", executionOrder.get(1));
        assertEquals("step3", executionOrder.get(2));
    }

    @Test
    @DisplayName("Should retry failed step with exponential backoff")
    void shouldRetryFailedStepWithExponentialBackoff() throws InterruptedException {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        List<String> executionOrder = new ArrayList<>();

        SagaStep failingStep = new SagaStep() {
            @Override
            public String name() {
                return "failing-step";
            }

            @Override
            public void execute(SagaContext context) {
                executionOrder.add("attempt-" + attemptCount.incrementAndGet());
                if (attemptCount.get() < 2) {
                    throw new RuntimeException("Transient failure");
                }
            }

            @Override
            public void compensate(SagaContext context) {
                executionOrder.add("compensate");
            }
        };

        SagaStep successStep = createStep("success-step", executionOrder);
        List<SagaStep> steps = List.of(failingStep, successStep);
        SagaContext context = new SagaContext("workflow-2", "file-2", "XLS", "org-2");

        // When
        SagaResult result = sagaOrchestrator.execute(steps, context);

        // Then
        assertTrue(result instanceof SagaResult.Success);
        assertEquals(3, executionOrder.size()); // attempt-1, attempt-2, success-step
        assertEquals("attempt-1", executionOrder.get(0));
        assertEquals("attempt-2", executionOrder.get(1));
        assertEquals("success-step", executionOrder.get(2));
    }

    @Test
    @DisplayName("Should fail after max retry attempts exceeded")
    void shouldFailAfterMaxRetryAttemptsExceeded() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        List<String> executionOrder = new ArrayList<>();

        SagaStep alwaysFailingStep = new SagaStep() {
            @Override
            public String name() {
                return "always-failing";
            }

            @Override
            public void execute(SagaContext context) {
                attemptCount.incrementAndGet();
                executionOrder.add("attempt-" + attemptCount.get());
                throw new RuntimeException("Permanent failure");
            }

            @Override
            public void compensate(SagaContext context) {
                executionOrder.add("compensate");
            }
        };

        List<SagaStep> steps = List.of(alwaysFailingStep);
        SagaContext context = new SagaContext("workflow-3", "file-3", "CSV", "org-3");

        // When
        SagaResult result = sagaOrchestrator.execute(steps, context);

        // Then
        assertTrue(result instanceof SagaResult.Failure);
        SagaResult.Failure failure = (SagaResult.Failure) result;
        assertEquals("always-failing", failure.failedStep());
        assertEquals(4, attemptCount.get()); // Initial attempt + 3 retries
        assertTrue(failure.compensatedSteps().isEmpty()); // No steps completed before failure
    }

    @Test
    @DisplayName("Should compensate completed steps on failure")
    void shouldCompensateCompletedStepsOnFailure() {
        // Given
        List<String> executionOrder = new ArrayList<>();

        SagaStep successStep1 = createStep("step1", executionOrder);

        SagaStep failingStep = new SagaStep() {
            @Override
            public String name() {
                return "failing-step";
            }

            @Override
            public void execute(SagaContext context) {
                executionOrder.add("execute-failing");
                throw new RuntimeException("Failure");
            }

            @Override
            public void compensate(SagaContext context) {
                executionOrder.add("compensate-failing");
            }
        };

        SagaStep successStep2 = createStep("step2", executionOrder);

        // step1 succeeds, then failingStep fails (step2 hasn't run yet)
        List<SagaStep> steps = List.of(successStep1, failingStep, successStep2);
        SagaContext context = new SagaContext("workflow-4", "file-4", "PDF", "org-4");

        // When
        SagaResult result = sagaOrchestrator.execute(steps, context);

        // Then
        assertTrue(result instanceof SagaResult.Failure);
        assertTrue(executionOrder.contains("execute-failing"));
        assertTrue(executionOrder.contains("compensate-step1"));
    }

    private SagaStep createStep(String name, List<String> executionOrder) {
        return new SagaStep() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(SagaContext context) {
                executionOrder.add(name);
            }

            @Override
            public void compensate(SagaContext context) {
                executionOrder.add("compensate-" + name);
            }
        };
    }
}
