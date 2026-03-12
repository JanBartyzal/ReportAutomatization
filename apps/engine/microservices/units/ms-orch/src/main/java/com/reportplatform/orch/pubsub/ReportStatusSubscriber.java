package com.reportplatform.orch.pubsub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reportplatform.orch.service.LifecycleWorkflowService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportStatusSubscriber {

    private static final Logger log = LoggerFactory.getLogger(ReportStatusSubscriber.class);

    private final LifecycleWorkflowService lifecycleWorkflowService;

    public ReportStatusSubscriber(LifecycleWorkflowService lifecycleWorkflowService) {
        this.lifecycleWorkflowService = lifecycleWorkflowService;
    }

    @Topic(name = "report.status_changed", pubsubName = "${dapr.pubsub.name:reportplatform-pubsub}")
    @PostMapping(path = "/api/v1/events/report-status-changed", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleReportStatusChanged(
            @RequestBody CloudEvent<ReportStatusEvent> cloudEvent) {

        ReportStatusEvent event = cloudEvent.getData();
        if (event == null) {
            log.warn("Received report.status_changed event with null data, skipping");
            return ResponseEntity.ok().build();
        }

        log.info("Received report.status_changed: reportId={}, {} -> {}",
                event.reportId(), event.fromStatus(), event.toStatus());

        try {
            lifecycleWorkflowService.handleStatusChange(event);
        } catch (Exception e) {
            log.error("Failed to handle report status change for report [{}]: {}",
                    event.reportId(), e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReportStatusEvent(
            String reportId,
            String orgId,
            String fromStatus,
            String toStatus,
            String userId,
            long timestamp
    ) {}
}
