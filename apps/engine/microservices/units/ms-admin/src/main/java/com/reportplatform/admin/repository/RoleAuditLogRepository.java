package com.reportplatform.admin.repository;

import com.reportplatform.admin.model.entity.RoleAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLogEntity, UUID> {

    Page<RoleAuditLogEntity> findByUserId(String userId, Pageable pageable);

    Page<RoleAuditLogEntity> findByPerformedBy(String performedBy, Pageable pageable);
}
