package com.reportplatform.template.tmpl.repository;

import com.reportplatform.template.tmpl.entity.MappingUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for mapping usage tracking operations.
 * Supports usage counting and high-usage detection for promotion candidates.
 */
@Repository
public interface MappingUsageRepository extends JpaRepository<MappingUsageEntity, UUID> {

    /**
     * Find all usage records for a given mapping template.
     */
    List<MappingUsageEntity> findByMappingTemplateId(UUID mappingTemplateId);

    /**
     * Find the usage record for a specific mapping template and organization.
     */
    Optional<MappingUsageEntity> findByMappingTemplateIdAndOrgId(UUID mappingTemplateId, UUID orgId);

    /**
     * Find all usage records where usage count meets or exceeds the given threshold.
     */
    List<MappingUsageEntity> findByUsageCountGreaterThanEqual(long threshold);
}
