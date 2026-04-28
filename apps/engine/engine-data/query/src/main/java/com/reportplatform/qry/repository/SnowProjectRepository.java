package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.SnowProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SnowProjectRepository extends JpaRepository<SnowProjectEntity, UUID> {

    @Query("""
        SELECT p FROM SnowProjectEntity p
        WHERE p.orgId = :orgId
          AND (:ragStatus IS NULL OR p.ragStatus = :ragStatus)
          AND (:status IS NULL OR p.status = :status)
          AND (:managerEmail IS NULL OR LOWER(p.managerEmail) = LOWER(:managerEmail))
          AND (:connectionId IS NULL OR p.resolverConnectionId = :connectionId)
        ORDER BY
          CASE p.ragStatus WHEN 'RED' THEN 1 WHEN 'AMBER' THEN 2 ELSE 3 END,
          p.budgetUtilizationPct DESC NULLS LAST
        """)
    Page<SnowProjectEntity> findFiltered(
            @Param("orgId") UUID orgId,
            @Param("ragStatus") String ragStatus,
            @Param("status") String status,
            @Param("managerEmail") String managerEmail,
            @Param("connectionId") UUID connectionId,
            Pageable pageable);

    Optional<SnowProjectEntity> findByIdAndOrgId(UUID id, UUID orgId);
}
