package com.reportplatform.notif.controller;

import com.reportplatform.notif.dto.NotificationResponse;
import com.reportplatform.notif.service.NotificationService;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST controller for notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // Store active SSE emitters per user for cleanup
    private final Map<String, List<SseEmitter>> userEmitters = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Get notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        String userId = user.getUsername();
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, read, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails user) {

        String userId = user.getUsername();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark notifications as read.
     */
    @PutMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody MarkReadRequest request) {

        String userId = user.getUsername();
        notificationService.markAsRead(request.notificationIds());
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails user) {

        String userId = user.getUsername();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * SSE endpoint for real-time notifications.
     * Clients connect to receive push notifications.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        log.info("SSE connection opened for user: {}", userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        // Register emitter for this user
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Handle client disconnect
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for user: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onError(e -> {
            log.error("SSE error for user {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        // Send initial event to confirm connection
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("status", "connected", "userId", userId)));
        } catch (IOException e) {
            log.error("Failed to send initial SSE event: {}", e.getMessage());
        }

        // Register with notification service for push notifications
        notificationService.registerSseEmitter(userId, emitter);

        return emitter;
    }

    /**
     * Remove emitter from user's list.
     */
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
        notificationService.unregisterSseEmitter(userId, emitter);
    }

    /**
     * Request record for marking notifications as read.
     */
    public record MarkReadRequest(List<UUID> notificationIds) {
    }
}
