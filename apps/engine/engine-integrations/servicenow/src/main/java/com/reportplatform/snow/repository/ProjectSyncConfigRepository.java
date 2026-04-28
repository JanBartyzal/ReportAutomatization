package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.ProjectSyncConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectSyncConfigRepository extends JpaRepository<ProjectSyncConfigEntity, UUID> {

    Optional<ProjectSyncConfigEntity> findByConnectionId(UUID connectionId);

    boolean existsByConnectionId(UUID connectionId);
}
