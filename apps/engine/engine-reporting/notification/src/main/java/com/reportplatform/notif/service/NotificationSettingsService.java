package com.reportplatform.notif.service;

import com.reportplatform.notif.model.NotificationSettingsEntity;
import com.reportplatform.notif.model.NotificationType;
import com.reportplatform.notif.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing notification settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;

    /**
     * Get all settings for a user in an organization.
     */
    public Map<NotificationType, NotificationSettingsEntity> getSettings(String userId, String orgId) {
        List<NotificationSettingsEntity> settings = settingsRepository.findByUserIdAndOrgId(userId, orgId);
        return settings.stream()
                .collect(Collectors.toMap(NotificationSettingsEntity::getNotificationType, s -> s));
    }

    /**
     * Update settings for a specific notification type.
     */
    @Transactional
    public NotificationSettingsEntity updateSettings(String userId, String orgId, NotificationType type,
            Boolean emailEnabled, Boolean inAppEnabled, Boolean criticalOnly) {
        NotificationSettingsEntity setting = settingsRepository
                .findByUserIdAndOrgIdAndNotificationType(userId, orgId, type)
                .orElse(NotificationSettingsEntity.builder()
                        .userId(userId)
                        .orgId(orgId)
                        .notificationType(type)
                        .build());

        if (emailEnabled != null) {
            setting.setEmailEnabled(emailEnabled);
        }
        if (inAppEnabled != null) {
            setting.setInAppEnabled(inAppEnabled);
        }
        if (criticalOnly != null) {
            setting.setCriticalOnly(criticalOnly);
        }

        return settingsRepository.save(setting);
    }

    /**
     * Check if email is enabled for a specific notification type.
     */
    public boolean isEmailEnabled(String userId, String orgId, NotificationType type) {
        return settingsRepository.findByUserIdAndOrgIdAndNotificationType(userId, orgId, type)
                .map(NotificationSettingsEntity::getEmailEnabled)
                .orElse(true); // Default: email enabled
    }

    /**
     * Check if in-app notification is enabled for a specific notification type.
     */
    public boolean isInAppEnabled(String userId, String orgId, NotificationType type) {
        return settingsRepository.findByUserIdAndOrgIdAndNotificationType(userId, orgId, type)
                .map(NotificationSettingsEntity::getInAppEnabled)
                .orElse(true); // Default: in-app enabled
    }

    /**
     * Check if only critical notifications should be sent via email.
     */
    public boolean isCriticalOnly(String userId, String orgId) {
        // Check if there's a CRITICAL setting or check all types
        return settingsRepository
                .findByUserIdAndOrgIdAndNotificationType(userId, orgId, NotificationType.REPORT_SUBMITTED)
                .map(NotificationSettingsEntity::getCriticalOnly)
                .orElse(false);
    }
}
