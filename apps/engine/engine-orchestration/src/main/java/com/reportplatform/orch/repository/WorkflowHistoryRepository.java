package com.reportplatform.orch.repository;

import com.reportplatform.orch.model.WorkflowHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting and querying workflow history records.
 */
@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistoryEntity, UUID> {

    Optional<WorkflowHistoryEntity> findByWorkflowId(String workflowId);

    Optional<WorkflowHistoryEntity> findByFileIdAndStatus(String fileId, String status);

    long countByStatusIn(List<String> statuses);

    long countByStatus(String status);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (w.completed_at - w.started_at)) * 1000) FROM workflow_history w WHERE w.completed_at IS NOT NULL", nativeQuery = true)
    Double averageProcessingTimeMs();
}
