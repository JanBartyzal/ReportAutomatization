package com.reportplatform.template.tmpl.repository;

import com.reportplatform.template.tmpl.entity.MappingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for mapping history (learning from successful mappings).
 */
@Repository
public interface MappingHistoryRepository extends JpaRepository<MappingHistoryEntity, UUID> {

    /**
     * Find history entries for an org and source column, ranked by usage frequency.
     */
    List<MappingHistoryEntity> findByOrgIdAndSourceColumnIgnoreCaseOrderByUsedCountDesc(
            String orgId, String sourceColumn);

    /**
     * Find all history entries for an org, ranked by usage frequency.
     */
    List<MappingHistoryEntity> findByOrgIdOrderByUsedCountDesc(String orgId);

    /**
     * Find a specific mapping for upsert purposes.
     */
    Optional<MappingHistoryEntity> findByOrgIdAndSourceColumnAndTargetColumn(
            String orgId, String sourceColumn, String targetColumn);
}
