package com.reportplatform.tmplpptx.repository;

import com.reportplatform.tmplpptx.entity.PptxTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PptxTemplateRepository extends JpaRepository<PptxTemplateEntity, UUID> {

    Page<PptxTemplateEntity> findByOrgIdAndActiveTrueOrderByCreatedAtDesc(String orgId, Pageable pageable);

    Page<PptxTemplateEntity> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<PptxTemplateEntity> findByScopeAndActiveTrueOrderByCreatedAtDesc(String scope, Pageable pageable);

    Optional<PptxTemplateEntity> findByIdAndActiveTrue(UUID id);

    Page<PptxTemplateEntity> findByOwnerOrgIdAndActiveTrueOrderByCreatedAtDesc(String ownerOrgId, Pageable pageable);

    @Query("SELECT t FROM PptxTemplateEntity t WHERE t.active = true AND (" +
           "t.scope = 'CENTRAL' " +
           "OR (t.scope = 'LOCAL' AND t.ownerOrgId = :orgId) " +
           "OR t.scope = 'SHARED_WITHIN_HOLDING'" +
           ") ORDER BY t.createdAt DESC")
    Page<PptxTemplateEntity> findVisibleTemplates(@Param("orgId") String orgId, Pageable pageable);
}
