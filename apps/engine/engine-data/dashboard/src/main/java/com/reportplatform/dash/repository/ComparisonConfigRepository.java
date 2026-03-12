package com.reportplatform.dash.repository;

import com.reportplatform.dash.model.ComparisonConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComparisonConfigRepository extends JpaRepository<ComparisonConfigEntity, UUID> {

    List<ComparisonConfigEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}
