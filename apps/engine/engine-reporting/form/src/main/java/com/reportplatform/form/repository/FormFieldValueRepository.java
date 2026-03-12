package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormFieldValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormFieldValueRepository extends JpaRepository<FormFieldValueEntity, UUID> {

    List<FormFieldValueEntity> findByResponseId(UUID responseId);

    Optional<FormFieldValueEntity> findByResponseIdAndFieldKey(UUID responseId, String fieldKey);

    void deleteByResponseId(UUID responseId);
}
