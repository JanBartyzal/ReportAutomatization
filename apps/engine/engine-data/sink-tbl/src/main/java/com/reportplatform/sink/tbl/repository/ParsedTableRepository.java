package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.ParsedTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for parsed_tables table operations.
 */
@Repository
public interface ParsedTableRepository extends JpaRepository<ParsedTableEntity, UUID> {

    /**
     * Find all parsed tables for a specific file.
     */
    List<ParsedTableEntity> findByFileId(String fileId);

    /**
     * Find all parsed tables for a specific file and org.
     */
    List<ParsedTableEntity> findByFileIdAndOrgId(String fileId, String orgId);

    /**
     * Delete all parsed tables for a specific file.
     * Used for Saga compensating action.
     */
    @Modifying
    @Query("DELETE FROM ParsedTableEntity p WHERE p.fileId = :fileId")
    int deleteByFileId(@Param("fileId") String fileId);
}
