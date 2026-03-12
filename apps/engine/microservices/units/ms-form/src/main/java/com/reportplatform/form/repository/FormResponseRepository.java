package com.reportplatform.form.repository;

import com.reportplatform.form.model.FormResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormResponseRepository extends JpaRepository<FormResponseEntity, UUID> {

    Page<FormResponseEntity> findByFormId(UUID formId, Pageable pageable);

    List<FormResponseEntity> findByFormIdAndOrgId(UUID formId, String orgId);

    List<FormResponseEntity> findByOrgIdAndPeriodIdAndFormVersionId(String orgId, UUID periodId, UUID formVersionId);
}
