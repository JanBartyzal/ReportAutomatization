package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ver.model.dto.CreateVersionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class DaprEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(DaprEventSubscriber.class);

    private final VersionService versionService;
    private final ObjectMapper objectMapper;

    public DaprEventSubscriber(VersionService versionService, ObjectMapper objectMapper) {
        this.versionService = versionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/dapr/subscribe")
    public ResponseEntity<List<Map<String, String>>> subscribe() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "pubsubname", "reportplatform-pubsub",
                        "topic", "data.changed",
                        "route", "/events/data-changed"
                ),
                Map.of(
                        "pubsubname", "reportplatform-pubsub",
                        "topic", "report.data_locked",
                        "route", "/events/data-locked"
                ),
                Map.of(
                        "pubsubname", "reportplatform-pubsub",
                        "topic", "form.response.submitted",
                        "route", "/events/form-submitted"
                ),
                Map.of(
                        "pubsubname", "reportplatform-pubsub",
                        "topic", "document.updated",
                        "route", "/events/document-updated"
                )
        ));
    }

    @PostMapping("/events/data-changed")
    public ResponseEntity<Void> onDataChanged(@RequestBody JsonNode event) {
        try {
            JsonNode data = event.path("data");
            String entityType = data.path("entityType").asText("TABLE_RECORD");
            UUID entityId = UUID.fromString(data.path("entityId").asText());
            UUID orgId = UUID.fromString(data.path("orgId").asText());
            String userId = data.path("userId").asText("system");
            JsonNode snapshotData = data.path("snapshotData");

            var request = new CreateVersionRequest(entityType, entityId, snapshotData,
                    "Data changed", userId);

            if (versionService.isLatestVersionLocked(entityType, entityId)) {
                versionService.createVersionOnLockedEntity(request, orgId);
            } else {
                versionService.createVersion(request, orgId);
            }

            log.info("Processed data.changed event for {}/{}", entityType, entityId);
        } catch (Exception e) {
            log.error("Error processing data.changed event", e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/data-locked")
    public ResponseEntity<Void> onDataLocked(@RequestBody JsonNode event) {
        try {
            JsonNode data = event.path("data");
            String entityType = data.path("entityType").asText("TABLE_RECORD");
            UUID entityId = UUID.fromString(data.path("entityId").asText());

            versionService.lockVersions(entityType, entityId);
            log.info("Processed report.data_locked event for {}/{}", entityType, entityId);
        } catch (Exception e) {
            log.error("Error processing report.data_locked event", e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/form-submitted")
    public ResponseEntity<Void> onFormSubmitted(@RequestBody JsonNode event) {
        try {
            JsonNode data = event.path("data");
            UUID entityId = UUID.fromString(data.path("formResponseId").asText());
            UUID orgId = UUID.fromString(data.path("orgId").asText());
            String userId = data.path("userId").asText("system");
            JsonNode snapshotData = data.path("responseData");

            var request = new CreateVersionRequest("FORM_RESPONSE", entityId, snapshotData,
                    "Form response submitted", userId);
            versionService.createVersion(request, orgId);

            log.info("Processed form.response.submitted for {}", entityId);
        } catch (Exception e) {
            log.error("Error processing form.response.submitted event", e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/document-updated")
    public ResponseEntity<Void> onDocumentUpdated(@RequestBody JsonNode event) {
        try {
            JsonNode data = event.path("data");
            UUID entityId = UUID.fromString(data.path("documentId").asText());
            UUID orgId = UUID.fromString(data.path("orgId").asText());
            String userId = data.path("userId").asText("system");
            JsonNode snapshotData = data.path("metadata");

            var request = new CreateVersionRequest("DOCUMENT", entityId, snapshotData,
                    "Document updated", userId);

            if (versionService.isLatestVersionLocked("DOCUMENT", entityId)) {
                versionService.createVersionOnLockedEntity(request, orgId);
            } else {
                versionService.createVersion(request, orgId);
            }

            log.info("Processed document.updated for {}", entityId);
        } catch (Exception e) {
            log.error("Error processing document.updated event", e);
        }
        return ResponseEntity.ok().build();
    }
}
