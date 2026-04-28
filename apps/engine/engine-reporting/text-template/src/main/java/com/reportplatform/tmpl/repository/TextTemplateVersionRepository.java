package com.reportplatform.tmpl.repository;

import com.reportplatform.tmpl.entity.TextTemplateVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TextTemplateVersionRepository extends JpaRepository<TextTemplateVersionEntity, UUID> {

    List<TextTemplateVersionEntity> findByTemplateIdOrderByVersionDesc(UUID templateId);
}
