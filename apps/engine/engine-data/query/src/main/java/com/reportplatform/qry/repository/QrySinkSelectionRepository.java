package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.SinkSelectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for sink_selections (FS25).
 */
public interface QrySinkSelectionRepository extends JpaRepository<SinkSelectionEntity, UUID> {

    List<SinkSelectionEntity> findByParsedTableId(UUID parsedTableId);

    List<SinkSelectionEntity> findByOrgIdAndPeriodIdAndSelectedTrueOrderByPriorityAsc(
            String orgId, String periodId);

    boolean existsByParsedTableId(UUID parsedTableId);
}
