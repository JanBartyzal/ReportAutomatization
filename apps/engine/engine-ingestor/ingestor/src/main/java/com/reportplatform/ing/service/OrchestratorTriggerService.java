package com.reportplatform.ing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.UploadPurpose;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes a FileUploadedEvent to the Dapr Pub/Sub topic after successful upload and scan.
 * The publish is fire-and-forget (@Async) so that the REST caller returns immediately.
 */
@Service
public class OrchestratorTriggerService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorTriggerService.class);

    private static final Duration DAPR_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);

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
     * Runs asynchronously — the REST endpoint returns immediately without waiting for Dapr/orchestrator.
     */
    @Async
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
            daprClient.publishEvent(pubsubName, topic, event).block(DAPR_TIMEOUT);
            log.info("Published file-uploaded event for file {} to topic '{}' via Dapr", fileEntity.getId(), topic);
        } catch (Throwable e) {
            log.warn("Dapr publish failed for file {}, falling back to orchestrator HTTP: {}",
                    fileEntity.getId(), e.getMessage());
            publishViaHttpFallback(fileEntity.getId(), event);
        }
    }

    /**
     * Fallback: call the orchestrator's file-uploaded subscriber endpoint directly
     * when the Dapr client fails. Sends a CloudEvent JSON wrapping the event payload.
     */
    private void publishViaHttpFallback(UUID fileId, FileUploadedEvent event) {
        try {
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

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(HTTP_CONNECT_TIMEOUT)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Fire-and-forget: the orchestrator's endpoint triggers a long-running workflow
            // and may not respond quickly. We don't need to wait for a response — the event
            // is delivered once the HTTP connection is established.
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("HTTP fallback: orchestrator accepted event for file {}", fileId);
                        } else {
                            log.error("HTTP fallback: orchestrator returned {} for file {} — body: {}",
                                    response.statusCode(), fileId, response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("HTTP fallback: connection to orchestrator failed for file {}: {}", fileId, ex.getMessage());
                        return null;
                    });
            log.info("HTTP fallback: event dispatched to orchestrator for file {} (async)", fileId);
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
