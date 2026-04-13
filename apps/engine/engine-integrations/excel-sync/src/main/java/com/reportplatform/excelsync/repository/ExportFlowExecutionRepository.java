package com.reportplatform.excelsync.repository;

import com.reportplatform.excelsync.model.entity.ExportFlowExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportFlowExecutionRepository extends JpaRepository<ExportFlowExecutionEntity, UUID> {

    Page<ExportFlowExecutionEntity> findByFlowIdAndOrgIdOrderByStartedAtDesc(
            UUID flowId, UUID orgId, Pageable pageable);

    Optional<ExportFlowExecutionEntity> findFirstByFlowIdOrderByStartedAtDesc(UUID flowId);
}
