package com.reportplatform.tmplpptx.repository;

import com.reportplatform.tmplpptx.entity.TemplateVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, UUID> {

    Optional<TemplateVersionEntity> findByTemplateIdAndCurrentTrue(UUID templateId);

    @Query("SELECT COALESCE(MAX(v.version), 0) FROM TemplateVersionEntity v WHERE v.template.id = :templateId")
    int findMaxVersionByTemplateId(@Param("templateId") UUID templateId);

    @Modifying
    @Query("UPDATE TemplateVersionEntity v SET v.current = false WHERE v.template.id = :templateId AND v.current = true")
    void clearCurrentVersion(@Param("templateId") UUID templateId);
}
