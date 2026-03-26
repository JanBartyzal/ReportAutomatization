package com.reportplatform.notif.controller;

import com.reportplatform.notif.dto.NotificationResponse;
import com.reportplatform.notif.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST controller for notifications.
 */
@RestController
@RequestMapping({"/api/v1/notifications", "/api/notifications"})
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // Store active SSE emitters per user for cleanup
    private final Map<String, List<SseEmitter>> userEmitters = new java.util.concurrent.ConcurrentHashMap<>();

    private String resolveUserId(String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        return "system";
    }

    /**
     * Get notifications for the authenticated user.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        String userId = resolveUserId(headerUserId);
        try {
            Page<NotificationResponse> notifications = notificationService.getNotifications(userId, read, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.warn("Failed to fetch notifications for user {}: {}", userId, e.getMessage());
            // Return empty page on DB/table errors
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        String userId = resolveUserId(headerUserId);
        try {
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.warn("Failed to get unread count for user {}: {}", userId, e.getMessage());
            return ResponseEntity.ok(Map.of("count", 0L));
        }
    }

    /**
     * Mark notifications as read.
     */
    @PutMapping("/read")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody MarkReadRequest request) {

        try {
            notificationService.markAsRead(request.notificationIds());
        } catch (Exception e) {
            log.warn("Failed to mark notifications as read: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read.
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        String userId = resolveUserId(headerUserId);
        try {
            notificationService.markAllAsRead(userId);
        } catch (Exception e) {
            log.warn("Failed to mark all notifications as read for user {}: {}", userId, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * SSE endpoint for real-time notifications.
     * Clients connect to receive push notifications.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = resolveUserId(headerUserId);
        log.info("SSE connection opened for user: {}", userId);

        SseEmitter emitter = new SseEmitter(30_000L); // 30 second timeout for SSE

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
        try {
            notificationService.registerSseEmitter(userId, emitter);
        } catch (Exception e) {
            log.warn("Failed to register SSE emitter: {}", e.getMessage());
        }

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
        try {
            notificationService.unregisterSseEmitter(userId, emitter);
        } catch (Exception e) {
            log.warn("Failed to unregister SSE emitter: {}", e.getMessage());
        }
    }

    /**
     * Mark a single notification as read.
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> markSingleAsRead(
            @PathVariable UUID id) {
        try {
            notificationService.markAsRead(List.of(id));
        } catch (Exception e) {
            log.warn("Failed to mark notification {} as read: {}", id, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Request record for marking notifications as read.
     */
    public record MarkReadRequest(List<UUID> notificationIds) {
    }
}
