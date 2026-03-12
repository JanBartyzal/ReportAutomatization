package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.PromotedTableRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for promoted_tables_registry operations.
 * Provides lookups by mapping template, table name, and status.
 */
@Repository
public interface PromotedTableRegistryRepository extends JpaRepository<PromotedTableRegistryEntity, UUID> {

    /**
     * Find a promoted table entry by its mapping template ID.
     */
    Optional<PromotedTableRegistryEntity> findByMappingTemplateId(UUID mappingTemplateId);

    /**
     * Find a promoted table entry by its table name.
     */
    Optional<PromotedTableRegistryEntity> findByTableName(String tableName);

    /**
     * Find all promoted table entries with a given status.
     */
    List<PromotedTableRegistryEntity> findByStatus(String status);
}
