package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.ComparisonKpiEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComparisonKpiRepository extends JpaRepository<ComparisonKpiEntity, UUID> {

    List<ComparisonKpiEntity> findByOrgIdAndActiveTrueOrderByNameAsc(UUID orgId);

    List<ComparisonKpiEntity> findByActiveTrueOrderByNameAsc();
}
