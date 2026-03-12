package com.reportplatform.audit.repository;

import com.reportplatform.audit.model.ReadAccessLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReadAccessLogRepository extends JpaRepository<ReadAccessLogEntity, UUID> {

    Page<ReadAccessLogEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);

    Page<ReadAccessLogEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);
}
