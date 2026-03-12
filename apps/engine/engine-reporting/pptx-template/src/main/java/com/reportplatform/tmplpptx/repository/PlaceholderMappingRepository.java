package com.reportplatform.tmplpptx.repository;

import com.reportplatform.tmplpptx.entity.PlaceholderMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaceholderMappingRepository extends JpaRepository<PlaceholderMappingEntity, UUID> {

    List<PlaceholderMappingEntity> findByTemplateId(UUID templateId);

    Optional<PlaceholderMappingEntity> findByTemplateIdAndPlaceholderKey(UUID templateId, String placeholderKey);

    void deleteByTemplateId(UUID templateId);
}
