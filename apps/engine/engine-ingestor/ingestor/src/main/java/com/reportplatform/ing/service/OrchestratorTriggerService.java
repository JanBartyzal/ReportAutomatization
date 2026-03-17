package com.reportplatform.ing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.UploadPurpose;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes a FileUploadedEvent to the Dapr Pub/Sub topic after successful upload and scan.
 */
@Service
public class OrchestratorTriggerService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorTriggerService.class);

    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;

    @Value("${dapr.pubsub.name}")
    private String pubsubName;

    @Value("${dapr.pubsub.topic.file-uploaded}")
    private String topic;

    @Value("${orchestrator.direct.url:http://engine-orchestrator:8080}")
    private String orchestratorUrl;

    public OrchestratorTriggerService(DaprClient daprClient, ObjectMapper objectMapper) {
        this.daprClient = daprClient;
        this.objectMapper = objectMapper;
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
            log.info("Published file-uploaded event for file {} to topic '{}' via gRPC", fileEntity.getId(), topic);
        } catch (Throwable e) {
            log.warn("gRPC publish failed for file {}, falling back to Dapr HTTP API: {}",
                    fileEntity.getId(), e.getMessage());
            publishViaHttpFallback(fileEntity.getId(), event);
        }
    }

    /**
     * Fallback: call the orchestrator's file-uploaded subscriber endpoint directly
     * when the Dapr gRPC client fails (e.g. missing OpenTelemetry classes).
     * Sends a CloudEvent JSON wrapping the event payload.
     */
    private void publishViaHttpFallback(UUID fileId, FileUploadedEvent event) {
        try {
            // Build the payload matching the orchestrator's FileUploadedEvent record fields
            var orchPayload = Map.of(
                    "fileId", event.fileId().toString(),
                    "fileName", event.filename(),
                    "fileType", event.mimeType(),
                    "orgId", event.orgId().toString(),
                    "uploadedBy", event.userId().toString(),
                    "fileSizeBytes", event.sizeBytes(),
                    "uploadPurpose", event.uploadPurpose().name(),
                    "blobUrl", event.blobUrl() != null ? event.blobUrl() : ""
            );

            // Wrap in CloudEvent structure that Dapr SDK's CloudEvent<T> can deserialize
            var cloudEvent = Map.of(
                    "specversion", "1.0",
                    "type", "com.reportplatform.file.uploaded",
                    "source", "engine-ingestor",
                    "id", UUID.randomUUID().toString(),
                    "datacontenttype", "application/json",
                    "data", orchPayload
            );

            String url = orchestratorUrl + "/api/v1/events/file-uploaded";
            String json = objectMapper.writeValueAsString(cloudEvent);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Published file-uploaded event for file {} to orchestrator via HTTP fallback", fileId);
            } else {
                log.error("HTTP fallback to orchestrator failed for file {} — status: {}, body: {}",
                        fileId, response.statusCode(), response.body());
            }
        } catch (Throwable ex) {
            log.error("HTTP fallback to orchestrator failed for file {}: {}", fileId, ex.getMessage(), ex);
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
