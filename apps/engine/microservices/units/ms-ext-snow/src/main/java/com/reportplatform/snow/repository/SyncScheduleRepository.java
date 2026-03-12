package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.SyncScheduleEntity;
import com.reportplatform.snow.model.entity.SyncScheduleEntity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncScheduleRepository extends JpaRepository<SyncScheduleEntity, UUID> {

    List<SyncScheduleEntity> findByConnectionId(UUID connectionId);

    List<SyncScheduleEntity> findByOrgId(UUID orgId);

    List<SyncScheduleEntity> findByEnabledTrueAndStatusAndNextRunAtBefore(SyncStatus status, Instant time);
}
