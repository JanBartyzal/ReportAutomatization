package com.reportplatform.sink.tbl.repository;

import com.reportplatform.sink.tbl.entity.FormResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for form_responses table operations.
 */
@Repository
public interface FormResponseRepository extends JpaRepository<FormResponseEntity, UUID> {

    /**
     * Find all form responses for a specific organization, period, and form
     * version.
     */
    List<FormResponseEntity> findByOrgIdAndPeriodIdAndFormVersionId(
            String orgId, String periodId, String formVersionId);

    /**
     * Find all form responses for a specific organization and period.
     */
    List<FormResponseEntity> findByOrgIdAndPeriodId(String orgId, String periodId);
}
