package com.reportplatform.snow.repository;

import com.reportplatform.snow.model.entity.ServiceNowConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceNowConnectionRepository extends JpaRepository<ServiceNowConnectionEntity, UUID> {

    List<ServiceNowConnectionEntity> findByOrgId(UUID orgId);

    List<ServiceNowConnectionEntity> findByOrgIdAndEnabled(UUID orgId, boolean enabled);
}
