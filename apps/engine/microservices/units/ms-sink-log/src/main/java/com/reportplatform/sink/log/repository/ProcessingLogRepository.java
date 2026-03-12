package com.reportplatform.sink.log.repository;

import com.reportplatform.sink.log.entity.ProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLogEntity, UUID> {
    List<ProcessingLogEntity> findByFileId(String fileId);

    List<ProcessingLogEntity> findByWorkflowId(String workflowId);
}
