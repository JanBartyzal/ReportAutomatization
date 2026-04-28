package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.NamedQueryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NamedQueryRepository extends JpaRepository<NamedQueryEntity, UUID> {

    /** Org queries + system queries (org_id IS NULL). Active only. */
    @Query("SELECT q FROM NamedQueryEntity q WHERE (q.orgId = :orgId OR q.orgId IS NULL) AND q.active = true ORDER BY q.name ASC")
    List<NamedQueryEntity> findAccessibleByOrgId(@Param("orgId") UUID orgId);

    @Query("SELECT q FROM NamedQueryEntity q WHERE (q.orgId = :orgId OR q.orgId IS NULL) AND q.active = true AND q.dataSourceHint = :hint ORDER BY q.name ASC")
    List<NamedQueryEntity> findAccessibleByOrgIdAndDataSourceHint(@Param("orgId") UUID orgId, @Param("hint") String hint);

    @Query("SELECT q FROM NamedQueryEntity q WHERE (q.orgId = :orgId OR q.orgId IS NULL) AND q.active = true ORDER BY q.createdAt DESC")
    Page<NamedQueryEntity> findAccessibleByOrgIdPageable(@Param("orgId") UUID orgId, Pageable pageable);

    /** Find by ID, but only if it's accessible (org or system). */
    @Query("SELECT q FROM NamedQueryEntity q WHERE q.id = :id AND (q.orgId = :orgId OR q.orgId IS NULL)")
    Optional<NamedQueryEntity> findByIdAndOrgAccess(@Param("id") UUID id, @Param("orgId") UUID orgId);

    boolean existsByOrgIdAndName(UUID orgId, String name);
}
