package com.reportplatform.orch.repository;

import com.reportplatform.orch.model.WorkflowHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting and querying workflow history records.
 */
@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistoryEntity, UUID> {

    Optional<WorkflowHistoryEntity> findByWorkflowId(String workflowId);

    Optional<WorkflowHistoryEntity> findByFileIdAndStatus(String fileId, String status);
}
