package com.reportplatform.notif.repository;

import com.reportplatform.notif.model.NotificationSettingsEntity;
import com.reportplatform.notif.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for notification settings.
 */
@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettingsEntity, UUID> {

    /**
     * Find all settings for a user in an organization.
     */
    List<NotificationSettingsEntity> findByUserIdAndOrgId(String userId, String orgId);

    /**
     * Find specific notification type setting for a user.
     */
    Optional<NotificationSettingsEntity> findByUserIdAndOrgIdAndNotificationType(
            String userId, String orgId, NotificationType notificationType);

    /**
     * Delete all settings for a user in an organization.
     */
    void deleteByUserIdAndOrgId(String userId, String orgId);
}
