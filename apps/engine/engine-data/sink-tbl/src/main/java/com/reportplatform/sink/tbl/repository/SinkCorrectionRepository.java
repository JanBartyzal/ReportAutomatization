package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.SinkCorrectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for sink_corrections table operations (FS25).
 */
public interface SinkCorrectionRepository extends JpaRepository<SinkCorrectionEntity, UUID> {

    List<SinkCorrectionEntity> findByParsedTableIdOrderByCorrectedAtAsc(UUID parsedTableId);

    List<SinkCorrectionEntity> findByParsedTableIdAndOrgIdOrderByCorrectedAtAsc(UUID parsedTableId, String orgId);

    long countByParsedTableId(UUID parsedTableId);

    @Modifying
    @Query("DELETE FROM SinkCorrectionEntity c WHERE c.parsedTableId = :parsedTableId")
    int deleteByParsedTableId(@Param("parsedTableId") UUID parsedTableId);
}
