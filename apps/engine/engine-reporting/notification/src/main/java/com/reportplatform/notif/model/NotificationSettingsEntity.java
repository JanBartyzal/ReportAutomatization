package com.reportplatform.notif.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Entity for per-user notification settings (opt-in/opt-out).
 */
@Entity
@Table(name = "notification_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "org_id", "notification_type" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String orgId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean criticalOnly = false;
}
