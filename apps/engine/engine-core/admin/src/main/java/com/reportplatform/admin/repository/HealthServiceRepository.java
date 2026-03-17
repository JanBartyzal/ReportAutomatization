package com.reportplatform.admin.repository;

import com.reportplatform.admin.model.entity.HealthServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the health_service_registry table.
 */
@Repository
public interface HealthServiceRepository extends JpaRepository<HealthServiceEntity, UUID> {

    List<HealthServiceEntity> findAllByEnabledTrueOrderBySortOrder();

    List<HealthServiceEntity> findAllByOrderBySortOrder();

    Optional<HealthServiceEntity> findByServiceId(String serviceId);
}
