package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.ParsedTableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QryParsedTableRepository extends JpaRepository<ParsedTableEntity, UUID> {

    List<ParsedTableEntity> findByFileId(String fileId);

    Page<ParsedTableEntity> findByOrgIdOrderByCreatedAtDesc(String orgId, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
            String orgId, String sourceSheet, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndFileIdOrderByCreatedAtDesc(
            String orgId, String fileId, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
            String orgId, String fileId, String sourceSheet, Pageable pageable);

    // Scope-aware queries
    Page<ParsedTableEntity> findByOrgIdAndScopeOrderByCreatedAtDesc(
            String orgId, String scope, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndScopeAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
            String orgId, String scope, String sourceSheet, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndScopeAndFileIdOrderByCreatedAtDesc(
            String orgId, String scope, String fileId, Pageable pageable);

    Page<ParsedTableEntity> findByOrgIdAndScopeAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
            String orgId, String scope, String fileId, String sourceSheet, Pageable pageable);
}
