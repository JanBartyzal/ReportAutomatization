package com.reportplatform.tmpl.repository;

import com.reportplatform.tmpl.entity.TextTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TextTemplateRepository extends JpaRepository<TextTemplateEntity, UUID> {

    /** Org templates + system templates (orgId IS NULL). Active only. */
    @Query("SELECT t FROM TextTemplateEntity t WHERE (t.orgId = :orgId OR t.orgId IS NULL) AND t.active = true ORDER BY t.name ASC")
    List<TextTemplateEntity> findAccessibleByOrgId(@Param("orgId") UUID orgId);

    @Query("SELECT t FROM TextTemplateEntity t WHERE (t.orgId = :orgId OR t.orgId IS NULL) AND t.active = true ORDER BY t.createdAt DESC")
    Page<TextTemplateEntity> findAccessibleByOrgIdPageable(@Param("orgId") UUID orgId, Pageable pageable);

    @Query("SELECT t FROM TextTemplateEntity t WHERE t.id = :id AND (t.orgId = :orgId OR t.orgId IS NULL)")
    Optional<TextTemplateEntity> findByIdAndOrgAccess(@Param("id") UUID id, @Param("orgId") UUID orgId);
}
