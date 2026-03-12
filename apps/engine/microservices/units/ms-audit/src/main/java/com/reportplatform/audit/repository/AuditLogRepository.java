package com.reportplatform.audit.repository;

import com.reportplatform.audit.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>,
        JpaSpecificationExecutor<AuditLogEntity> {

    Stream<AuditLogEntity> findByOrgIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID orgId, Instant from, Instant to);
}
