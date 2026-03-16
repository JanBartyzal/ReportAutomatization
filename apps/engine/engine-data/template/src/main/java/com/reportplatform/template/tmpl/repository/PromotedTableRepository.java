package com.reportplatform.template.tmpl.repository;

import com.reportplatform.template.tmpl.entity.PromotedTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotedTableRepository extends JpaRepository<PromotedTableEntity, UUID> {

    Optional<PromotedTableEntity> findByMappingTemplateIdAndStatusIn(UUID mappingTemplateId, List<String> statuses);

    Optional<PromotedTableEntity> findByMappingTemplateId(UUID mappingTemplateId);

    List<PromotedTableEntity> findByStatus(String status);
}
