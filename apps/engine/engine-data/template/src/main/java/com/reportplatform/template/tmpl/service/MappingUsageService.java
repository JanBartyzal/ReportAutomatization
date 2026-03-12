package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingUsageEntity;
import com.reportplatform.template.tmpl.repository.MappingUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for tracking mapping template usage.
 * Provides usage counting, statistics, and high-usage detection
 * to support smart persistence promotion decisions.
 */
@Service
@Transactional
public class MappingUsageService {

    private static final Logger logger = LoggerFactory.getLogger(MappingUsageService.class);

    private final MappingUsageRepository usageRepository;

    public MappingUsageService(MappingUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    /**
     * Increment usage count for a mapping template used by a specific organization.
     * Creates a new tracking record if one does not already exist.
     *
     * @param mappingTemplateId the mapping template that was used
     * @param orgId             the organization that used it
     */
    public void incrementUsage(UUID mappingTemplateId, UUID orgId) {
        Optional<MappingUsageEntity> existing = usageRepository
                .findByMappingTemplateIdAndOrgId(mappingTemplateId, orgId);

        MappingUsageEntity usage;
        if (existing.isPresent()) {
            usage = existing.get();
            usage.setUsageCount(usage.getUsageCount() + 1);
        } else {
            usage = new MappingUsageEntity();
            usage.setMappingTemplateId(mappingTemplateId);
            usage.setOrgId(orgId);
            usage.setUsageCount(1);
        }
        usage.setLastUsedAt(OffsetDateTime.now());

        // Update distinct org count for this template
        List<MappingUsageEntity> allForTemplate = usageRepository
                .findByMappingTemplateId(mappingTemplateId);
        int distinctOrgs = (int) allForTemplate.stream()
                .map(MappingUsageEntity::getOrgId)
                .distinct()
                .count();
        // Include the current org if it is a new record
        if (existing.isEmpty()) {
            distinctOrgs++;
        }
        usage.setDistinctOrgCount(distinctOrgs);

        usageRepository.save(usage);

        logger.debug("Incremented usage for template={} org={} count={}",
                mappingTemplateId, orgId, usage.getUsageCount());
    }

    /**
     * Get aggregate usage statistics for a mapping template across all organizations.
     *
     * @param mappingTemplateId the mapping template to query
     * @return usage stats containing total count and distinct org count
     */
    @Transactional(readOnly = true)
    public UsageStats getUsageStats(UUID mappingTemplateId) {
        List<MappingUsageEntity> records = usageRepository.findByMappingTemplateId(mappingTemplateId);

        long totalUsageCount = records.stream()
                .mapToLong(MappingUsageEntity::getUsageCount)
                .sum();

        int distinctOrgCount = (int) records.stream()
                .map(MappingUsageEntity::getOrgId)
                .distinct()
                .count();

        return new UsageStats(totalUsageCount, distinctOrgCount);
    }

    /**
     * Find all mapping usage records where usage count meets or exceeds the given threshold.
     *
     * @param threshold minimum usage count
     * @return list of high-usage mapping records
     */
    @Transactional(readOnly = true)
    public List<MappingUsageEntity> getHighUsageMappings(long threshold) {
        return usageRepository.findByUsageCountGreaterThanEqual(threshold);
    }

    /**
     * Aggregate usage statistics for a mapping template.
     */
    public record UsageStats(long totalUsageCount, int distinctOrgCount) {}
}
