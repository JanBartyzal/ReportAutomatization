package com.reportplatform.admin.repository;

import com.reportplatform.admin.model.entity.PromotionCandidateEntity;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for promotion candidate CRUD operations.
 */
@Repository
public interface PromotionCandidateRepository extends JpaRepository<PromotionCandidateEntity, UUID> {

    /**
     * Find all promotion candidates with the given status.
     */
    List<PromotionCandidateEntity> findByStatus(PromotionStatus status);

    /**
     * Find all promotion candidates for a specific mapping template.
     */
    List<PromotionCandidateEntity> findByMappingTemplateId(UUID mappingTemplateId);

    /**
     * Check if a promotion candidate already exists for a mapping template
     * in any of the given statuses (to avoid duplicate proposals).
     */
    boolean existsByMappingTemplateIdAndStatusIn(UUID mappingTemplateId, Collection<PromotionStatus> statuses);

    /**
     * Find all promotion candidates for a specific mapping template with any of the
     * given statuses.
     */
    List<PromotionCandidateEntity> findByMappingTemplateIdAndStatusIn(UUID mappingTemplateId,
            Collection<PromotionStatus> statuses);
}
