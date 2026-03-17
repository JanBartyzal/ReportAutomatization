package com.reportplatform.orch.repository;

import com.reportplatform.orch.model.FailedJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for persisting and querying failed job records.
 */
@Repository
public interface FailedJobRepository extends JpaRepository<FailedJobEntity, UUID> {

    List<FailedJobEntity> findByOrgId(String orgId);

    Page<FailedJobEntity> findByOrgId(String orgId, Pageable pageable);

    List<FailedJobEntity> findByWorkflowId(String workflowId);

    List<FailedJobEntity> findByFileId(String fileId);

    long countByOrgId(String orgId);

    List<FailedJobEntity> findTop20ByOrderByFailedAtDesc();

    long countByRetryCountGreaterThanEqual(int retryCount);
}
