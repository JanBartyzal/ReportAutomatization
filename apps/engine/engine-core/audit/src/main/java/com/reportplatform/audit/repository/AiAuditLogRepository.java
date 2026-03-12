package com.reportplatform.audit.repository;

import com.reportplatform.audit.model.AiAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiAuditLogRepository extends JpaRepository<AiAuditLogEntity, UUID> {

    Page<AiAuditLogEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);
}
