package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.SyncJobHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SyncJobHistoryRepository extends JpaRepository<SyncJobHistoryEntity, UUID> {

    List<SyncJobHistoryEntity> findByScheduleIdOrderByStartedAtDesc(UUID scheduleId);

    Page<SyncJobHistoryEntity> findByOrgIdOrderByStartedAtDesc(UUID orgId, Pageable pageable);
}
