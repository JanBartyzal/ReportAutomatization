package com.reportplatform.notif.dto;

import com.reportplatform.notif.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String userId;
    private NotificationType type;
    private String title;
    private String body;
    private Boolean read;
    private Map<String, String> data;
    private Instant createdAt;
}
