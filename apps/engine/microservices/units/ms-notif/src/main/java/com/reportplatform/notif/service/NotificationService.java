package com.reportplatform.notif.service;

import com.reportplatform.notif.dto.NotificationEvent;
import com.reportplatform.notif.dto.NotificationResponse;
import com.reportplatform.notif.model.NotificationEntity;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailNotificationService emailNotificationService;

    // Store active SSE emitters per user
    private final Map<String, List<SseEmitter>> userSseEmitters = new ConcurrentHashMap<>();

    /**
     * Register an SSE emitter for a user.
     */
    public void registerSseEmitter(String userId, SseEmitter emitter) {
        userSseEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("Registered SSE emitter for user: {}", userId);
    }

    /**
     * Unregister an SSE emitter for a user.
     */
    public void unregisterSseEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userSseEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userSseEmitters.remove(userId);
            }
        }
        log.debug("Unregistered SSE emitter for user: {}", userId);
    }

    /**
     * Create and store a notification, then push to WebSocket and SSE.
     */
    @Transactional
    public NotificationResponse createNotification(NotificationEvent event) {
        NotificationEntity entity = NotificationEntity.builder()
                .userId(event.getTargetUserId())
                .type(event.getType())
                .title(event.getTitle())
                .body(event.getBody())
                .data(event.getData() != null ? event.getData() : Map.of())
                .eventId(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString())
                .read(false)
                .build();

        entity = notificationRepository.save(entity);
        log.info("Created notification {} for user {}", entity.getId(), entity.getUserId());

        NotificationResponse response = toResponse(entity);

        // Push to WebSocket for real-time notification
        pushToWebSocket(response);

        // Push to SSE for browser clients
        pushToSse(response);

        // Send email notification if applicable
        emailNotificationService.sendEmailNotification(event, response);

        return response;
    }

    /**
     * Get notifications for a user with pagination.
     */
    public Page<NotificationResponse> getNotifications(String userId, Boolean read, Pageable pageable) {
        Page<NotificationEntity> page;
        if (read != null) {
            page = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, read, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toResponse);
    }

    /**
     * Get unread notification count for a user.
     */
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Mark notifications as read.
     */
    @Transactional
    public void markAsRead(List<UUID> notificationIds) {
        int updated = notificationRepository.markAsReadByIds(notificationIds);
        log.info("Marked {} notifications as read", updated);
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all {} notifications as read for user {}", updated, userId);
    }

    /**
     * Push notification to WebSocket for real-time delivery.
     */
    private void pushToWebSocket(NotificationResponse notification) {
        try {
            String destination = "/user/" + notification.getUserId() + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Pushed notification {} to WebSocket {}", notification.getId(), destination);
        } catch (Exception e) {
            log.warn("Failed to push notification {} to WebSocket: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * Push notification to SSE for browser clients.
     */
    private void pushToSse(NotificationResponse notification) {
        List<SseEmitter> emitters = userSseEmitters.get(notification.getUserId());
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notification));
                    log.debug("Pushed notification {} to SSE for user {}", notification.getId(),
                            notification.getUserId());
                } catch (IOException e) {
                    log.warn("Failed to push notification {} to SSE: {}", notification.getId(), e.getMessage());
                    // Remove dead emitter
                    unregisterSseEmitter(notification.getUserId(), emitter);
                }
            }
        }
    }

    /**
     * Route notification based on type.
     */
    public String routeNotification(NotificationType type, Map<String, String> data) {
        switch (type) {
            case REPORT_SUBMITTED:
                return data.get("holdingAdminId");
            case REPORT_APPROVED:
                return data.get("editorId");
            case REPORT_REJECTED:
                return data.get("editorId");
            case DEADLINE_APPROACHING:
                return data.get("userIds");
            case DEADLINE_MISSED:
                return data.get("holdingAdminId");
            default:
                return null;
        }
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .read(entity.getRead())
                .data(entity.getData())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
