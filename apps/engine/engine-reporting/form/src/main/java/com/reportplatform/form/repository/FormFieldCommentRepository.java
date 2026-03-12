package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormFieldCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormFieldCommentRepository extends JpaRepository<FormFieldCommentEntity, UUID> {

    List<FormFieldCommentEntity> findByResponseId(UUID responseId);

    List<FormFieldCommentEntity> findByResponseIdAndFieldKey(UUID responseId, String fieldKey);
}
