package com.reportplatform.admin.repository;

import com.reportplatform.admin.model.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    Optional<OrganizationEntity> findByCode(String code);

    List<OrganizationEntity> findByParentIsNull();

    List<OrganizationEntity> findByParentId(UUID parentId);

    @Query("SELECT o FROM AdminOrganizationEntity o WHERE o.parent IS NULL AND o.active = true")
    List<OrganizationEntity> findTopLevelOrganizations();

    @Query("SELECT o FROM AdminOrganizationEntity o WHERE o.type = :type AND o.active = true")
    List<OrganizationEntity> findByType(OrganizationEntity.OrganizationType type);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(o) > 0 FROM AdminOrganizationEntity o WHERE o.parent.id = :parentId")
    boolean hasChildren(UUID parentId);
}
