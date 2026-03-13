package com.reportplatform.notif.controller;

import com.reportplatform.notif.model.NotificationSettingsEntity;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for notification settings.
 */
@RestController
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    /**
     * Get all notification settings for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<SettingsResponse>> getSettings(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Org-Id") String orgId) {

        String userId = user.getUsername();
        Map<NotificationType, NotificationSettingsEntity> settings = settingsService.getSettings(userId, orgId);

        List<SettingsResponse> response = settings.entrySet().stream()
                .map(e -> new SettingsResponse(
                        e.getKey(),
                        e.getValue().getEmailEnabled(),
                        e.getValue().getInAppEnabled(),
                        e.getValue().getCriticalOnly()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Update settings for a specific notification type.
     */
    @PutMapping("/{type}")
    public ResponseEntity<SettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable NotificationType type,
            @RequestBody UpdateSettingsRequest request) {

        String userId = user.getUsername();
        NotificationSettingsEntity updated = settingsService.updateSettings(
                userId, orgId, type,
                request.emailEnabled(),
                request.inAppEnabled(),
                request.criticalOnly());

        return ResponseEntity.ok(new SettingsResponse(
                updated.getNotificationType(),
                updated.getEmailEnabled(),
                updated.getInAppEnabled(),
                updated.getCriticalOnly()));
    }

    public record SettingsResponse(
            NotificationType type,
            Boolean emailEnabled,
            Boolean inAppEnabled,
            Boolean criticalOnly) {
    }

    public record UpdateSettingsRequest(
            Boolean emailEnabled,
            Boolean inAppEnabled,
            Boolean criticalOnly) {
    }
}
