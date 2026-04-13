package com.reportplatform.excelsync.repository;

import com.reportplatform.excelsync.model.entity.ExportFlowDefinitionEntity;
import com.reportplatform.excelsync.model.entity.TriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportFlowDefinitionRepository extends JpaRepository<ExportFlowDefinitionEntity, UUID> {

    List<ExportFlowDefinitionEntity> findByOrgIdAndIsActiveTrue(UUID orgId);

    List<ExportFlowDefinitionEntity> findByOrgId(UUID orgId);

    Optional<ExportFlowDefinitionEntity> findByIdAndOrgId(UUID id, UUID orgId);

    List<ExportFlowDefinitionEntity> findByOrgIdAndIsActiveTrueAndTriggerType(UUID orgId, TriggerType triggerType);
}
