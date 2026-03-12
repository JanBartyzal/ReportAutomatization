package com.reportplatform.orch.pubsub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reportplatform.orch.service.PptxGenerationWorkflowService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dapr Pub/Sub subscriber for PPTX generation requests.
 * Triggered when a report is APPROVED (published by LifecycleWorkflowService.onApproved).
 */
@RestController
public class PptxGenerationSubscriber {

    private static final Logger log = LoggerFactory.getLogger(PptxGenerationSubscriber.class);

    private final PptxGenerationWorkflowService generationService;

    public PptxGenerationSubscriber(PptxGenerationWorkflowService generationService) {
        this.generationService = generationService;
    }

    @Topic(name = "pptx.generation_requested", pubsubName = "${dapr.pubsub.name:reportplatform-pubsub}")
    @PostMapping(path = "/api/v1/events/pptx-generation-requested", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleGenerationRequested(
            @RequestBody CloudEvent<PptxGenerationEvent> cloudEvent) {

        PptxGenerationEvent event = cloudEvent.getData();
        if (event == null) {
            log.warn("Received pptx.generation_requested event with null data, skipping");
            return ResponseEntity.ok().build();
        }

        log.info("Received pptx.generation_requested: reportId={}, orgId={}, triggeredBy={}",
                event.reportId(), event.orgId(), event.triggeredBy());

        try {
            generationService.handleGenerationRequest(event);
        } catch (Exception e) {
            log.error("Failed to handle PPTX generation for report [{}]: {}",
                    event.reportId(), e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PptxGenerationEvent(
            String reportId,
            String orgId,
            String triggeredBy,
            String templateId,
            String periodId,
            boolean batchMode
    ) {}
}
