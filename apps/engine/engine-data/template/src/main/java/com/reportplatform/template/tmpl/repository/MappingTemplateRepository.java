package com.reportplatform.template.tmpl.repository;

import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for mapping template CRUD operations.
 */
@Repository
public interface MappingTemplateRepository extends JpaRepository<MappingTemplateEntity, UUID> {

    /**
     * Find templates scoped to the given org OR global templates (org_id IS NULL).
     */
    List<MappingTemplateEntity> findByOrgIdOrOrgIdIsNull(String orgId);

    /**
     * Find active templates for an org (including global ones).
     */
    List<MappingTemplateEntity> findByIsActiveTrueAndOrgIdOrOrgIdIsNull(String orgId);

    /**
     * Find templates by org only (excluding global).
     */
    List<MappingTemplateEntity> findByOrgId(String orgId);
}
