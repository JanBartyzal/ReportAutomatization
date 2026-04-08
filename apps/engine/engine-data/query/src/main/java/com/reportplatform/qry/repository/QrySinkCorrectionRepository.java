package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.SinkCorrectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for sink_corrections (FS25).
 */
public interface QrySinkCorrectionRepository extends JpaRepository<SinkCorrectionEntity, UUID> {

    List<SinkCorrectionEntity> findByParsedTableIdOrderByCorrectedAtAsc(UUID parsedTableId);

    long countByParsedTableId(UUID parsedTableId);
}
