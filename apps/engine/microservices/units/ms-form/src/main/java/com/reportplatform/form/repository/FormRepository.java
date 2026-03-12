package com.reportplatform.form.repository;

import com.reportplatform.form.config.FormState;
import com.reportplatform.form.model.FormEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormRepository extends JpaRepository<FormEntity, UUID> {

    Page<FormEntity> findByOrgId(String orgId, Pageable pageable);

    Page<FormEntity> findByOrgIdAndStatus(String orgId, FormState status, Pageable pageable);

    List<FormEntity> findByStatus(FormState status);

    Page<FormEntity> findByScope(String scope, Pageable pageable);

    Page<FormEntity> findByOwnerOrgId(String ownerOrgId, Pageable pageable);

    @Query("SELECT f FROM FormEntity f WHERE f.scope = :scope AND f.ownerOrgId = :ownerOrgId")
    Page<FormEntity> findByScopeAndOwnerOrgId(
            @Param("scope") String scope,
            @Param("ownerOrgId") String ownerOrgId,
            Pageable pageable);

    @Query("SELECT f FROM FormEntity f WHERE " +
           "(f.scope = 'CENTRAL') " +
           "OR (f.scope = 'LOCAL' AND f.ownerOrgId = :orgId) " +
           "OR (f.scope = 'SHARED_WITHIN_HOLDING')")
    Page<FormEntity> findVisibleForms(@Param("orgId") String orgId, Pageable pageable);

    @Query("SELECT f FROM FormEntity f WHERE f.releasedAt IS NOT NULL")
    Page<FormEntity> findReleasedForms(Pageable pageable);
}
