package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormAssignmentRepository extends JpaRepository<FormAssignmentEntity, UUID> {

    List<FormAssignmentEntity> findByFormId(UUID formId);

    List<FormAssignmentEntity> findByOrgId(String orgId);

    Optional<FormAssignmentEntity> findByFormIdAndOrgId(UUID formId, String orgId);
}
