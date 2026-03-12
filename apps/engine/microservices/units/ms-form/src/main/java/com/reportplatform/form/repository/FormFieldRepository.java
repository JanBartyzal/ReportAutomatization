package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormFieldRepository extends JpaRepository<FormFieldEntity, UUID> {

    List<FormFieldEntity> findByFormVersionIdOrderBySortOrder(UUID formVersionId);

    List<FormFieldEntity> findByFormVersionIdAndSection(UUID formVersionId, String section);

    void deleteByFormVersionId(UUID formVersionId);
}
