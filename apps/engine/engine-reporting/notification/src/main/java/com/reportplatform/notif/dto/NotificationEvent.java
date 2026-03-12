package com.reportplatform.notif.dto;

import com.reportplatform.notif.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Event payload from Dapr pub/sub for notification events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private NotificationType type;
    private String title;
    private String body;
    private String userId;
    private String targetUserId;
    private Map<String, String> data;
    private String eventId;
}
