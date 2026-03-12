package com.reportplatform.template.tmpl.repository;

import com.reportplatform.template.tmpl.entity.MappingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for mapping rule operations.
 */
@Repository
public interface MappingRuleRepository extends JpaRepository<MappingRuleEntity, UUID> {

    /**
     * Find rules for a template, ordered by priority (highest first).
     */
    List<MappingRuleEntity> findByTemplateIdOrderByPriorityDesc(UUID templateId);

    /**
     * Find rules by type within a template.
     */
    List<MappingRuleEntity> findByTemplateIdAndRuleType(UUID templateId, String ruleType);
}
