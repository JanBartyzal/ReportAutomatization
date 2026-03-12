package com.reportplatform.qry.repository;

import com.reportplatform.qry.model.FileSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileSummaryRepository extends JpaRepository<FileSummaryView, UUID> {

    Page<FileSummaryView> findByOrgIdOrderByCreatedAtDesc(UUID orgId, Pageable pageable);

    Optional<FileSummaryView> findByFileIdAndOrgId(UUID fileId, UUID orgId);
}
