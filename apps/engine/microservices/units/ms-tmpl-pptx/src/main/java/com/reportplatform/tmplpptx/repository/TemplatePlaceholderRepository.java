package com.reportplatform.tmplpptx.repository;

import com.reportplatform.tmplpptx.entity.TemplatePlaceholderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TemplatePlaceholderRepository extends JpaRepository<TemplatePlaceholderEntity, UUID> {

    List<TemplatePlaceholderEntity> findByVersionId(UUID versionId);
}
