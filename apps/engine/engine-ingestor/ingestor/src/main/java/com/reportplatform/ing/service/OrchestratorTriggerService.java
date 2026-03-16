package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.UploadPurpose;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publishes a FileUploadedEvent to the Dapr Pub/Sub topic after successful upload and scan.
 */
@Service
public class OrchestratorTriggerService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorTriggerService.class);

    private final DaprClient daprClient;

    @Value("${dapr.pubsub.name}")
    private String pubsubName;

    @Value("${dapr.pubsub.topic.file-uploaded}")
    private String topic;

    public OrchestratorTriggerService(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    /**
     * Publishes a file-uploaded event so the orchestrator can start the processing pipeline.
     */
    public void publishFileUploaded(FileEntity fileEntity) {
        var event = new FileUploadedEvent(
                fileEntity.getId(),
                fileEntity.getOrgId(),
                fileEntity.getUserId(),
                fileEntity.getFilename(),
                fileEntity.getMimeType(),
                fileEntity.getSizeBytes(),
                fileEntity.getBlobUrl(),
                fileEntity.getUploadPurpose(),
                System.currentTimeMillis()
        );

        try {
            daprClient.publishEvent(pubsubName, topic, event).block();
            log.info("Published file-uploaded event for file {} to topic '{}'", fileEntity.getId(), topic);
        } catch (Throwable e) {
            log.warn("Failed to publish file-uploaded event for file {} (Dapr sidecar may not be running): {}",
                    fileEntity.getId(), e.getMessage());
        }
    }

    public record FileUploadedEvent(
            UUID fileId,
            UUID orgId,
            UUID userId,
            String filename,
            String mimeType,
            long sizeBytes,
            String blobUrl,
            UploadPurpose uploadPurpose,
            long timestamp
    ) {
    }
}
