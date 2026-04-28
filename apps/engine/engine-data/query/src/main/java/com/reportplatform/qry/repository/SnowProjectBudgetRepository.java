package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.SnowProjectBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SnowProjectBudgetRepository extends JpaRepository<SnowProjectBudgetEntity, UUID> {

    List<SnowProjectBudgetEntity> findByProjectIdOrderByFiscalYearAsc(UUID projectId);
}
