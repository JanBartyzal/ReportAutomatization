package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.DistributionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DistributionRuleRepository extends JpaRepository<DistributionRuleEntity, UUID> {

    List<DistributionRuleEntity> findByOrgId(UUID orgId);

    List<DistributionRuleEntity> findByScheduleId(UUID scheduleId);

    List<DistributionRuleEntity> findByScheduleIdAndEnabledTrue(UUID scheduleId);
}
