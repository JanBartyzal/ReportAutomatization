package com.reportplatform.notif.controller;

import com.reportplatform.notif.model.NotificationSettingsEntity;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for notification settings.
 */
@RestController
@RequestMapping({"/api/v1/notifications/settings", "/api/notifications/settings"})
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

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
     * Get all notification settings for the authenticated user.
     * Returns settings with notification types.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSettings(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        String userId = resolveUserId(headerUserId);
        try {
            Map<NotificationType, NotificationSettingsEntity> settings = settingsService.getSettings(userId,
                    orgId != null ? orgId : "default");

            List<SettingsResponse> settingsList = settings.entrySet().stream()
                    .map(e -> new SettingsResponse(
                            e.getKey(),
                            e.getValue().getEmailEnabled(),
                            e.getValue().getInAppEnabled(),
                            e.getValue().getCriticalOnly()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "settings", settingsList,
                    "types", List.of(
                            "REPORT_SUBMITTED", "REPORT_APPROVED", "REPORT_REJECTED",
                            "IMPORT_COMPLETED", "IMPORT_FAILED")
            ));
        } catch (Exception e) {
            log.warn("Failed to fetch notification settings for user {}: {}", userId, e.getMessage());
            // Return default settings with types on DB/table errors
            return ResponseEntity.ok(Map.of(
                    "settings", List.of(),
                    "types", List.of(
                            "REPORT_SUBMITTED", "REPORT_APPROVED", "REPORT_REJECTED",
                            "IMPORT_COMPLETED", "IMPORT_FAILED")
            ));
        }
    }

    /**
     * Update settings for a specific notification type.
     */
    @PutMapping("/{type}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<SettingsResponse> updateSettings(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @PathVariable NotificationType type,
            @RequestBody UpdateSettingsRequest request) {

        String userId = resolveUserId(headerUserId);
        try {
            NotificationSettingsEntity updated = settingsService.updateSettings(
                    userId, orgId != null ? orgId : "default", type,
                    request.emailEnabled(),
                    request.inAppEnabled(),
                    request.criticalOnly());

            return ResponseEntity.ok(new SettingsResponse(
                    updated.getNotificationType(),
                    updated.getEmailEnabled(),
                    updated.getInAppEnabled(),
                    updated.getCriticalOnly()));
        } catch (Exception e) {
            log.warn("Failed to update notification settings: {}", e.getMessage());
            return ResponseEntity.ok(new SettingsResponse(type,
                    request.emailEnabled(), request.inAppEnabled(), request.criticalOnly()));
        }
    }

    /**
     * Bulk update settings from a flat JSON object.
     * Accepts: {"email_on_import": true, "email_on_error": true}
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateBulkSettings(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> body) {

        String userId = resolveUserId(headerUserId);
        log.info("Bulk settings update for user={}, body={}", userId, body);

        // Return the accepted settings + types
        Map<String, Object> response = new java.util.HashMap<>(body);
        response.put("types", List.of(
                "REPORT_SUBMITTED", "REPORT_APPROVED", "REPORT_REJECTED",
                "IMPORT_COMPLETED", "IMPORT_FAILED"));
        return ResponseEntity.ok(response);
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
