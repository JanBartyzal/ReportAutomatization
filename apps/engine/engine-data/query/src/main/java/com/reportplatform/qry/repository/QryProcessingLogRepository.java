package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.ProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QryProcessingLogRepository extends JpaRepository<ProcessingLogEntity, UUID> {

    List<ProcessingLogEntity> findByFileIdOrderByCreatedAtAsc(String fileId);
}
