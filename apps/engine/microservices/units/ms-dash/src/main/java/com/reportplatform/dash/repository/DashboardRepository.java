package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.DashboardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID> {

    List<DashboardEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<DashboardEntity> findByOrgIdAndIsPublicTrueOrderByCreatedAtDesc(UUID orgId);

    Optional<DashboardEntity> findByIdAndOrgId(UUID id, UUID orgId);

    @Query("SELECT d FROM DashboardEntity d WHERE d.orgId = :orgId " +
           "AND (d.isPublic = true OR d.createdBy = :userId) " +
           "ORDER BY d.createdAt DESC")
    List<DashboardEntity> findAccessibleDashboards(
            @Param("orgId") UUID orgId,
            @Param("userId") UUID userId);
}
