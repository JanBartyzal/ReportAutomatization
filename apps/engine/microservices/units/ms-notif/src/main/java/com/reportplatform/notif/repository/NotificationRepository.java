package com.reportplatform.notif.repository;

import com.reportplatform.notif.model.NotificationEntity;
import com.reportplatform.notif.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for notification entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndReadOrderByCreatedAtDesc(String userId, Boolean read, Pageable pageable);

    List<NotificationEntity> findByUserIdAndReadFalse(String userId);

    long countByUserIdAndReadFalse(String userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsReadByUserId(String userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.id IN :ids")
    int markAsReadByIds(List<UUID> ids);

    @Query("SELECT n FROM NotificationEntity n WHERE n.type = :type AND n.createdAt > :since")
    List<NotificationEntity> findByTypeAndCreatedAtAfter(NotificationType type, Instant since);
}
