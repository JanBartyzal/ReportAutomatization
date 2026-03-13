package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.DistributionHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DistributionHistoryRepository extends JpaRepository<DistributionHistoryEntity, UUID> {

    Page<DistributionHistoryEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

    List<DistributionHistoryEntity> findByRuleId(UUID ruleId);
}
