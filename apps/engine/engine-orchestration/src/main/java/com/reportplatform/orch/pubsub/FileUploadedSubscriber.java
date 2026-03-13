package com.reportplatform.orch.pubsub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.reportplatform.orch.service.WorkflowService;
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
 * Dapr Pub/Sub subscriber for file upload events.
 * <p>
 * Listens to the {@code file-uploaded} topic and triggers the file processing
 * workflow
 * when a new file upload event is received.
 * </p>
 */
@RestController
public class FileUploadedSubscriber {

    private static final Logger log = LoggerFactory.getLogger(FileUploadedSubscriber.class);

    private final WorkflowService workflowService;

    public FileUploadedSubscriber(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Handles incoming file-uploaded events from Dapr Pub/Sub.
     *
     * @param cloudEvent the CloudEvent wrapping the FileUploadedEvent payload
     * @return 200 OK on successful processing, triggering Dapr to ACK the message
     */
    @Topic(name = "file-uploaded", pubsubName = "${dapr.pubsub.name:reportplatform-pubsub}")
    @PostMapping(path = "/api/v1/events/file-uploaded", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleFileUploaded(
            @RequestBody CloudEvent<FileUploadedEvent> cloudEvent) {

        FileUploadedEvent event = cloudEvent.getData();
        if (event == null) {
            log.warn("Received file-uploaded event with null data, skipping");
            return ResponseEntity.ok().build();
        }

        log.info("Received file-uploaded event: fileId={}, fileType={}, orgId={}, purpose={}",
                event.fileId(), event.fileType(), event.orgId(), event.uploadPurpose());

        try {
            String workflowId;
            String uploadPurpose = event.uploadPurpose();

            if ("FORM_IMPORT".equals(uploadPurpose)) {
                workflowId = workflowService.startFormImportWorkflow(
                        event.fileId(), event.fileType(), event.orgId(), event.blobUrl());
                log.info("FORM_IMPORT Workflow [{}] started for file [{}]", workflowId, event.fileId());
            } else {
                workflowId = workflowService.startWorkflow(
                        event.fileId(), event.fileType(), event.orgId());
                log.info("FILE_PROCESSING Workflow [{}] started for file [{}]", workflowId, event.fileId());
            }
        } catch (Exception e) {
            log.error("Failed to start workflow for file [{}]: {}",
                    event.fileId(), e.getMessage(), e);
            // Return 200 to prevent Dapr redelivery; the failure is recorded in the DB
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Event payload for file upload notifications.
     * Includes upload_purpose to route to appropriate workflow.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileUploadedEvent(
            String fileId,
            String fileName,
            String fileType,
            String orgId,
            String uploadedBy,
            long fileSizeBytes,
            String uploadPurpose,
            String blobUrl) {
    }
}
