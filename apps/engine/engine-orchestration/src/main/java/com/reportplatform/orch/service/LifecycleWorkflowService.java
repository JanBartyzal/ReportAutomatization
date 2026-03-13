package com.reportplatform.orch.service;

import com.reportplatform.orch.config.ServiceRoutingConfig;
import com.reportplatform.orch.pubsub.ReportStatusSubscriber.ReportStatusEvent;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LifecycleWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LifecycleWorkflowService.class);

    @Value("${dapr.pubsub.name:reportplatform-pubsub}")
    private String pubsubName;

    private final ServiceRoutingConfig routingConfig;
    private DaprClient daprClient;

    public LifecycleWorkflowService(ServiceRoutingConfig routingConfig) {
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

    public void handleStatusChange(ReportStatusEvent event) {
        switch (event.toStatus()) {
            case "SUBMITTED" -> onSubmitted(event);
            case "APPROVED" -> onApproved(event);
            case "REJECTED" -> onRejected(event);
            default -> log.debug("No workflow action for status: {}", event.toStatus());
        }
    }

    private void onSubmitted(ReportStatusEvent event) {
        log.info("Workflow: SUBMITTED - validating data completeness for report {}", event.reportId());

        // Validate data completeness via engine-data (query service)
        try {
            daprClient.invokeMethod(
                    routingConfig.engineData(),
                    "api/query/validate/" + event.reportId(),
                    null,
                    HttpExtension.GET,
                    Map.class
            ).block();
            log.info("Workflow: SUBMITTED - validation passed for report {}", event.reportId());
        } catch (Exception e) {
            log.warn("Workflow: SUBMITTED - validation check failed for report {}: {}",
                    event.reportId(), e.getMessage());
        }
    }

    private void onApproved(ReportStatusEvent event) {
        log.info("Workflow: APPROVED - triggering central reporting inclusion for report {}",
                event.reportId());

        // Trigger PPTX generation (prepared for P4b)
        try {
            Map<String, String> pptxRequest = Map.of(
                    "reportId", event.reportId(),
                    "orgId", event.orgId(),
                    "triggeredBy", event.userId()
            );
            daprClient.publishEvent(pubsubName, "pptx.generation_requested", pptxRequest).block();
            log.info("Workflow: APPROVED - PPTX generation requested for report {}", event.reportId());
        } catch (Exception e) {
            log.warn("Workflow: APPROVED - failed to trigger PPTX generation for report {}: {}",
                    event.reportId(), e.getMessage());
        }
    }

    private void onRejected(ReportStatusEvent event) {
        log.info("Workflow: REJECTED - notifying editor for report {}", event.reportId());

        Map<String, Object> notification = Map.of(
                "type", "report_rejected",
                "reportId", event.reportId(),
                "orgId", event.orgId(),
                "userId", event.userId(),
                "timestamp", System.currentTimeMillis()
        );

        try {
            daprClient.publishEvent(pubsubName, "notify", notification).block();
            log.info("Workflow: REJECTED - notification published for report {}", event.reportId());
        } catch (Exception e) {
            log.error("Workflow: REJECTED - failed to publish notification for report {}: {}",
                    event.reportId(), e.getMessage());
        }
    }
}
