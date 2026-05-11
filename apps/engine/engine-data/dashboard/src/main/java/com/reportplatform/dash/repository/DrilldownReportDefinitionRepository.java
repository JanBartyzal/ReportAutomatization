package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.DrilldownReportDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrilldownReportDefinitionRepository extends JpaRepository<DrilldownReportDefinitionEntity, UUID> {

    Optional<DrilldownReportDefinitionEntity> findByIdAndOrgId(UUID id, UUID orgId);

    @Query("SELECT r FROM DrilldownReportDefinitionEntity r WHERE r.orgId = :orgId " +
            "AND (r.isPublic = true OR r.createdBy = :userId) " +
            "ORDER BY r.updatedAt DESC")
    List<DrilldownReportDefinitionEntity> findAccessibleReports(
            @Param("orgId") UUID orgId,
            @Param("userId") UUID userId);
}
