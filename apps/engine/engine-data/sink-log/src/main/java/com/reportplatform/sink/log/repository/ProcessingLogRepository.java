package com.reportplatform.sink.log.repository;

import com.reportplatform.sink.log.entity.ProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessingLogRepository extends JpaRepository<ProcessingLogEntity, UUID> {
    List<ProcessingLogEntity> findByFileId(String fileId);

    List<ProcessingLogEntity> findByWorkflowId(String workflowId);
}
