package com.reportplatform.lifecycle.repository;

import com.reportplatform.lifecycle.config.ReportScope;
import com.reportplatform.lifecycle.config.ReportState;
import com.reportplatform.lifecycle.model.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    Page<ReportEntity> findByOrgId(String orgId, Pageable pageable);

    Page<ReportEntity> findByOrgIdAndPeriodId(String orgId, UUID periodId, Pageable pageable);

    Page<ReportEntity> findByPeriodId(UUID periodId, Pageable pageable);

    Page<ReportEntity> findByPeriodIdAndStatus(UUID periodId, ReportState status, Pageable pageable);

    Optional<ReportEntity> findByOrgIdAndPeriodIdAndReportType(String orgId, UUID periodId, String reportType);

    Optional<ReportEntity> findByOrgIdAndPeriodIdAndReportTypeAndScope(
            String orgId, UUID periodId, String reportType, ReportScope scope);

    List<ReportEntity> findByIdIn(List<UUID> ids);

    // Scope-aware listing
    Page<ReportEntity> findByOrgIdAndScope(String orgId, ReportScope scope, Pageable pageable);

    Page<ReportEntity> findByOrgIdAndPeriodIdAndScope(String orgId, UUID periodId, ReportScope scope, Pageable pageable);

    Page<ReportEntity> findByPeriodIdAndScope(UUID periodId, ReportScope scope, Pageable pageable);

    @Query("SELECT r.orgId, r.periodId, r.status, COUNT(r) FROM ReportEntity r " +
           "WHERE r.periodId = :periodId GROUP BY r.orgId, r.periodId, r.status")
    List<Object[]> findMatrixByPeriodId(@Param("periodId") UUID periodId);

    @Query("SELECT r.orgId, r.periodId, r.status, r.scope, COUNT(r) FROM ReportEntity r " +
           "WHERE r.periodId = :periodId GROUP BY r.orgId, r.periodId, r.status, r.scope")
    List<Object[]> findMatrixByPeriodIdWithScope(@Param("periodId") UUID periodId);

    @Query("SELECT r.orgId, r.periodId, r.status, r.scope, COUNT(r) FROM ReportEntity r " +
           "WHERE r.periodId = :periodId AND r.scope = :scope " +
           "GROUP BY r.orgId, r.periodId, r.status, r.scope")
    List<Object[]> findMatrixByPeriodIdAndScope(
            @Param("periodId") UUID periodId, @Param("scope") ReportScope scope);

    @Query("SELECT r.status, COUNT(r) FROM ReportEntity r " +
           "WHERE r.periodId = :periodId GROUP BY r.status")
    List<Object[]> countByPeriodIdGroupByStatus(@Param("periodId") UUID periodId);

    long countByPeriodIdAndStatus(UUID periodId, ReportState status);
}
