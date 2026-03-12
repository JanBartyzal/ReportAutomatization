package com.reportplatform.period.repository;

import com.reportplatform.period.model.PeriodOrgAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PeriodOrgAssignmentRepository extends JpaRepository<PeriodOrgAssignmentEntity, UUID> {

    List<PeriodOrgAssignmentEntity> findByPeriodId(UUID periodId);

    List<PeriodOrgAssignmentEntity> findByOrgId(String orgId);

    void deleteByPeriodId(UUID periodId);
}
