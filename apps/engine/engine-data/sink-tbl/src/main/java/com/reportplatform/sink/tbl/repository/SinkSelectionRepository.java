package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.SinkSelectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for sink_selections table operations (FS25).
 */
public interface SinkSelectionRepository extends JpaRepository<SinkSelectionEntity, UUID> {

    Optional<SinkSelectionEntity> findByParsedTableIdAndPeriodIdAndReportType(
            UUID parsedTableId, String periodId, String reportType);

    List<SinkSelectionEntity> findByOrgIdAndPeriodIdAndSelectedTrueOrderByPriorityAsc(
            String orgId, String periodId);

    List<SinkSelectionEntity> findByOrgIdAndPeriodIdAndReportTypeAndSelectedTrueOrderByPriorityAsc(
            String orgId, String periodId, String reportType);

    List<SinkSelectionEntity> findByParsedTableId(UUID parsedTableId);
}
