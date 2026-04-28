package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.SnowProjectTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SnowProjectTaskRepository extends JpaRepository<SnowProjectTaskEntity, UUID> {

    List<SnowProjectTaskEntity> findByProjectIdOrderByDueDateAsc(UUID projectId);
}
