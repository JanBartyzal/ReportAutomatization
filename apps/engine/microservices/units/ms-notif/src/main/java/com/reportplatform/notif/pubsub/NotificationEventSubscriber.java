package com.reportplatform.notif.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.notif.dto.NotificationEvent;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.service.NotificationService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dapr Pub/Sub subscriber for notification events.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationEventSubscriber {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Subscribe to the notify topic for all notification events.
     */
    @Topic(name = "notify", pubsubName = "reportplatform-pubsub")
    @PostMapping(path = "/api/v1/pubsub/notify")
    public ResponseEntity<Void> handleNotificationEvent(@RequestBody CloudEvent<NotificationEvent> cloudEvent) {
        try {
            NotificationEvent event = cloudEvent.getData();
            log.info("Received notification event: type={}, title={}", event.getType(), event.getTitle());

            if (event.getEventId() == null || event.getEventId().isBlank()) {
                event.setEventId(UUID.randomUUID().toString());
            }

            // Route notification based on type
            resolveAndCreateNotification(event);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Resolve the target user and create notification based on event type.
     */
    private void resolveAndCreateNotification(NotificationEvent event) {
        Map<String, String> data = event.getData();

        NotificationType type = event.getType();

        if (type == NotificationType.REPORT_SUBMITTED) {
            String holdingAdminId = data != null ? data.get("holdingAdminId") : null;
            String targetUserId = holdingAdminId != null ? holdingAdminId : event.getTargetUserId();
            if (targetUserId != null) {
                event.setTargetUserId(targetUserId);
                notificationService.createNotification(event);
                log.info("Notification routed to HoldingAdmin: {}", targetUserId);
            }
        } else if (type == NotificationType.REPORT_APPROVED || type == NotificationType.REPORT_REJECTED) {
            String editorId = data != null ? data.get("editorId") : null;
            String targetUserId = editorId != null ? editorId : event.getTargetUserId();
            if (targetUserId != null) {
                event.setTargetUserId(targetUserId);
                notificationService.createNotification(event);
                log.info("Notification routed to Editor: {}", targetUserId);
            }
        } else if (type == NotificationType.DEADLINE_APPROACHING) {
            String userIds = data != null ? data.get("userIds") : null;
            if (userIds != null) {
                List<String> users = Arrays.asList(userIds.split(","));
                for (String userId : users) {
                    NotificationEvent singleEvent = NotificationEvent.builder()
                            .type(event.getType())
                            .title(event.getTitle())
                            .body(event.getBody())
                            .targetUserId(userId.trim())
                            .data(data)
                            .eventId(event.getEventId())
                            .build();
                    notificationService.createNotification(singleEvent);
                }
                log.info("Deadline approaching notifications sent to {} users", users.size());
            } else if (event.getTargetUserId() != null) {
                notificationService.createNotification(event);
            }
        } else if (type == NotificationType.DEADLINE_MISSED) {
            String holdingAdminId = data != null ? data.get("holdingAdminId") : null;
            String targetUserId = holdingAdminId != null ? holdingAdminId : event.getTargetUserId();
            if (targetUserId != null) {
                event.setTargetUserId(targetUserId);
                notificationService.createNotification(event);
                log.info("Deadline missed notification routed to HoldingAdmin: {}", targetUserId);
            }
        } else {
            // Default: use targetUserId
            if (event.getTargetUserId() != null) {
                notificationService.createNotification(event);
            }
        }
    }
}
