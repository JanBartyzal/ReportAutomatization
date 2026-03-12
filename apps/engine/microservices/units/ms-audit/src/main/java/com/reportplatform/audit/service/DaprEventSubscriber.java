package com.reportplatform.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.audit.model.dto.CreateAuditLogRequest;
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

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public DaprEventSubscriber(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/dapr/subscribe")
    public ResponseEntity<List<Map<String, String>>> subscribe() {
        return ResponseEntity.ok(List.of(
                subscription("report.status_changed", "/events/status-changed"),
                subscription("data.changed", "/events/data-changed"),
                subscription("file.uploaded", "/events/file-uploaded"),
                subscription("role.changed", "/events/role-changed"),
                subscription("form.field.changed", "/events/form-field-changed"),
                subscription("form.comment.added", "/events/form-comment-added"),
                subscription("form.import.confirmed", "/events/form-import-confirmed"),
                subscription("version.created", "/events/version-created")
        ));
    }

    @PostMapping("/events/status-changed")
    public ResponseEntity<Void> onStatusChanged(@RequestBody JsonNode event) {
        processEvent(event, "REPORT_STATUS_CHANGED", data -> {
            JsonNode details = objectMapper.createObjectNode()
                    .put("fromState", data.path("fromState").asText())
                    .put("toState", data.path("toState").asText())
                    .put("comment", data.path("comment").asText(""));
            return details;
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/data-changed")
    public ResponseEntity<Void> onDataChanged(@RequestBody JsonNode event) {
        processEvent(event, "DATA_CHANGED", null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/file-uploaded")
    public ResponseEntity<Void> onFileUploaded(@RequestBody JsonNode event) {
        processEvent(event, "FILE_UPLOADED", null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/role-changed")
    public ResponseEntity<Void> onRoleChanged(@RequestBody JsonNode event) {
        processEvent(event, "ROLE_CHANGED", data -> {
            return objectMapper.createObjectNode()
                    .put("previousRole", data.path("previousRole").asText())
                    .put("newRole", data.path("newRole").asText());
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/form-field-changed")
    public ResponseEntity<Void> onFormFieldChanged(@RequestBody JsonNode event) {
        processEvent(event, "FORM_FIELD_CHANGED", data -> {
            return objectMapper.createObjectNode()
                    .put("fieldName", data.path("fieldName").asText())
                    .put("oldValue", data.path("oldValue").asText())
                    .put("newValue", data.path("newValue").asText());
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/form-comment-added")
    public ResponseEntity<Void> onFormCommentAdded(@RequestBody JsonNode event) {
        processEvent(event, "FORM_COMMENT_ADDED", null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/form-import-confirmed")
    public ResponseEntity<Void> onFormImportConfirmed(@RequestBody JsonNode event) {
        processEvent(event, "FORM_IMPORT_CONFIRMED", null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/version-created")
    public ResponseEntity<Void> onVersionCreated(@RequestBody JsonNode event) {
        processEvent(event, "VERSION_CREATED", data -> {
            return objectMapper.createObjectNode()
                    .put("versionNumber", data.path("versionNumber").asInt());
        });
        return ResponseEntity.ok().build();
    }

    private void processEvent(JsonNode event, String action, DetailExtractor detailExtractor) {
        try {
            JsonNode data = event.path("data");
            UUID orgId = UUID.fromString(data.path("orgId").asText());
            String userId = data.path("userId").asText("system");
            String entityType = data.path("entityType").asText("UNKNOWN");
            UUID entityId = data.has("entityId") && !data.path("entityId").asText().isBlank()
                    ? UUID.fromString(data.path("entityId").asText()) : null;

            JsonNode details = detailExtractor != null ? detailExtractor.extract(data) : data;

            var request = new CreateAuditLogRequest(
                    orgId, userId, action, entityType, entityId,
                    details, null, null);

            auditLogService.createAuditLog(request);
            log.debug("Audit log created from event: {}", action);
        } catch (Exception e) {
            log.error("Error processing {} event", action, e);
        }
    }

    private Map<String, String> subscription(String topic, String route) {
        return Map.of(
                "pubsubname", "reportplatform-pubsub",
                "topic", topic,
                "route", route
        );
    }

    @FunctionalInterface
    private interface DetailExtractor {
        JsonNode extract(JsonNode data);
    }
}
