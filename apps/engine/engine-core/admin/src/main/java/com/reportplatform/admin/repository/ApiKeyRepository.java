package com.reportplatform.admin.repository;

import com.reportplatform.admin.model.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.revoked = false AND (k.expiresAt IS NULL OR k.expiresAt > CURRENT_TIMESTAMP)")
    List<ApiKeyEntity> findActiveKeys();

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.revoked = false")
    List<ApiKeyEntity> findAllNonRevoked();

    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.organization.id = :orgId AND k.revoked = false")
    List<ApiKeyEntity> findActiveKeysByOrgId(UUID orgId);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.createdBy = :createdBy")
    List<ApiKeyEntity> findByCreatedBy(String createdBy);
}
