package com.reportplatform.orch.service;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import com.reportplatform.orch.pubsub.PptxGenerationSubscriber.PptxGenerationEvent;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the PPTX report generation workflow.
 *
 * Steps:
 * 1. Load template config from engine-reporting (PPTX template service)
 * 2. Load report data from engine-data (query service)
 * 3. Apply placeholder mapping
 * 4. Call processor-generators to generate the report
 * 5. Store generated file URL on the report entity via engine-reporting
 * 6. Notify user via Pub/Sub
 */
@Service
public class PptxGenerationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(PptxGenerationWorkflowService.class);

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 2000;

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    private final ServiceRoutingConfig routingConfig;
    private DaprClient daprClient;

    public PptxGenerationWorkflowService(ServiceRoutingConfig routingConfig) {
        this.routingConfig = routingConfig;
    }

    @PostConstruct
    void init() {
        this.daprClient = new DaprClientBuilder().build();
    }

    @PreDestroy
    void destroy() {
        if (daprClient != null) {
            try {
                daprClient.close();
            } catch (Exception e) {
                log.warn("Failed to close Dapr client: {}", e.getMessage());
            }
        }
    }

    /**
     * Handle a single report generation request.
     */
    public void handleGenerationRequest(PptxGenerationEvent event) {
        log.info("Starting PPTX generation workflow for report {}", event.reportId());

        try {
            // Step 1: Load template configuration from engine-reporting (PPTX template)
            Map templateConfig = loadTemplateConfig(event);

            // Step 2: Load report data from engine-data (query)
            Map reportData = loadReportData(event);

            // Step 3: Apply mapping and call processor-generators
            Map generationResult = generateReport(event, templateConfig, reportData);

            // Step 4: Store generated file URL on report entity
            storeGeneratedUrl(event, generationResult);

            // Step 5: Notify completion
            publishCompletion(event, true, null);

            log.info("PPTX generation workflow completed for report {}", event.reportId());

        } catch (Exception e) {
            log.error("PPTX generation workflow failed for report {}: {}", event.reportId(), e.getMessage(), e);
            publishCompletion(event, false, e.getMessage());
        }
    }

    /**
     * Handle batch generation for all APPROVED reports in a period.
     */
    public void handleBatchGeneration(String periodId, String orgId, String triggeredBy) {
        log.info("Starting batch PPTX generation for period={}, org={}", periodId, orgId);

        // Fetch approved reports for the period from engine-reporting (lifecycle)
        List<Map> reports;
        try {
            reports = daprClient.invokeMethod(
                    routingConfig.engineReporting(),
                    "api/reports?periodId=" + periodId + "&status=APPROVED",
                    null,
                    HttpExtension.GET,
                    List.class
            ).block();
        } catch (Exception e) {
            log.error("Failed to fetch approved reports for period {}: {}", periodId, e.getMessage());
            return;
        }

        if (reports == null || reports.isEmpty()) {
            log.info("No approved reports found for period {}", periodId);
            return;
        }

        int success = 0;
        int failed = 0;

        for (var report : reports) {
            String reportId = String.valueOf(report.get("id"));
            var event = new PptxGenerationEvent(reportId, orgId, triggeredBy, null, periodId, true);

            boolean generated = false;
            for (int attempt = 0; attempt <= MAX_RETRIES && !generated; attempt++) {
                try {
                    if (attempt > 0) {
                        log.info("Retry {} for report {}", attempt, reportId);
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    }
                    handleGenerationRequest(event);
                    generated = true;
                    success++;
                } catch (Exception e) {
                    log.warn("Attempt {} failed for report {}: {}", attempt + 1, reportId, e.getMessage());
                    if (attempt == MAX_RETRIES) {
                        failed++;
                        log.error("All retries exhausted for report {}. Sending to DLQ.", reportId);
                    }
                }
            }
        }

        log.info("Batch generation complete: {} successful, {} failed out of {} reports",
                success, failed, reports.size());
    }

    private Map loadTemplateConfig(PptxGenerationEvent event) {
        String templateId = event.templateId();

        // If no template specified, get default for the org
        String endpoint = templateId != null
                ? "api/templates/pptx/" + templateId
                : "api/templates/pptx?scope=CENTRAL&size=1";

        try {
            return daprClient.invokeMethod(
                    routingConfig.engineReporting(),
                    endpoint,
                    null,
                    HttpExtension.GET,
                    Map.class
            ).block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template config: " + e.getMessage(), e);
        }
    }

    private Map loadReportData(PptxGenerationEvent event) {
        try {
            return daprClient.invokeMethod(
                    routingConfig.engineData(),
                    "api/query/report/" + event.reportId() + "/data",
                    null,
                    HttpExtension.GET,
                    Map.class
            ).block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load report data: " + e.getMessage(), e);
        }
    }

    private Map generateReport(PptxGenerationEvent event, Map templateConfig, Map reportData) {
        // Build generation request combining template config, mappings, and report data
        Map<String, Object> genRequest = Map.of(
                "reportId", event.reportId(),
                "templateId", templateConfig.getOrDefault("id", ""),
                "templateConfig", templateConfig,
                "reportData", reportData
        );

        try {
            return daprClient.invokeMethod(
                    routingConfig.processorGenerators(),
                    "generate",
                    genRequest,
                    HttpExtension.POST,
                    Map.class
            ).block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PPTX: " + e.getMessage(), e);
        }
    }

    private void storeGeneratedUrl(PptxGenerationEvent event, Map generationResult) {
        Map<String, Object> updateRequest = Map.of(
                "reportId", event.reportId(),
                "generatedFileUrl", generationResult.getOrDefault("generatedFileUrl", ""),
                "generatedAt", generationResult.getOrDefault("generatedAt", "")
        );

        try {
            daprClient.invokeMethod(
                    routingConfig.engineReporting(),
                    "api/reports/" + event.reportId() + "/generated",
                    updateRequest,
                    HttpExtension.POST,
                    Void.class
            ).block();
        } catch (Exception e) {
            log.warn("Failed to store generated URL for report {}: {}", event.reportId(), e.getMessage());
        }
    }

    private void publishCompletion(PptxGenerationEvent event, boolean success, String errorMessage) {
        Map<String, Object> notification = Map.of(
                "type", success ? "pptx_generation_completed" : "pptx_generation_failed",
                "reportId", event.reportId(),
                "orgId", event.orgId(),
                "triggeredBy", event.triggeredBy(),
                "success", success,
                "errorMessage", errorMessage != null ? errorMessage : "",
                "timestamp", System.currentTimeMillis()
        );

        try {
            daprClient.publishEvent(pubsubName, "pptx.generation_completed", notification).block();
            log.info("Published pptx.generation_completed for report {} (success={})",
                    event.reportId(), success);
        } catch (Exception e) {
            log.error("Failed to publish generation completion for report {}: {}",
                    event.reportId(), e.getMessage());
        }
    }
}
