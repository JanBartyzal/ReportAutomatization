package com.reportplatform.auth.repository;

import com.reportplatform.auth.model.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuthApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findByKeyPrefixAndActiveTrue(String keyPrefix);

    List<ApiKeyEntity> findByOrganizationIdAndActiveTrue(UUID organizationId);
}
