package com.reportplatform.auth.repository;

import com.reportplatform.auth.model.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuthOrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    Optional<OrganizationEntity> findByCode(String code);

    Optional<OrganizationEntity> findByTenantId(String tenantId);
}
