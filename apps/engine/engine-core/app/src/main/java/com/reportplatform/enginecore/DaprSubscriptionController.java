package com.reportplatform.enginecore;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consolidated Dapr subscription endpoint that merges subscriptions from
 * all modules (audit, versioning, etc.) into a single /dapr/subscribe response.
 */
@RestController
public class DaprSubscriptionController {

    @PostMapping("/dapr/subscribe")
    public ResponseEntity<List<Map<String, String>>> subscribe() {
        List<Map<String, String>> subscriptions = new ArrayList<>();

        // Audit module subscriptions
        subscriptions.add(subscription("report.status_changed", "/events/status-changed"));
        subscriptions.add(subscription("data.changed", "/events/audit/data-changed"));
        subscriptions.add(subscription("file.uploaded", "/events/file-uploaded"));
        subscriptions.add(subscription("role.changed", "/events/role-changed"));
        subscriptions.add(subscription("form.field.changed", "/events/form-field-changed"));
        subscriptions.add(subscription("form.comment.added", "/events/form-comment-added"));
        subscriptions.add(subscription("form.import.confirmed", "/events/form-import-confirmed"));
        subscriptions.add(subscription("version.created", "/events/version-created"));

        // Versioning module subscriptions
        subscriptions.add(subscription("data.changed", "/events/versioning/data-changed"));
        subscriptions.add(subscription("report.data_locked", "/events/data-locked"));
        subscriptions.add(subscription("form.response.submitted", "/events/form-submitted"));
        subscriptions.add(subscription("document.updated", "/events/document-updated"));

        return ResponseEntity.ok(subscriptions);
    }

    private Map<String, String> subscription(String topic, String route) {
        return Map.of(
                "pubsubname", "reportplatform-pubsub",
                "topic", topic,
                "route", route
        );
    }
}
