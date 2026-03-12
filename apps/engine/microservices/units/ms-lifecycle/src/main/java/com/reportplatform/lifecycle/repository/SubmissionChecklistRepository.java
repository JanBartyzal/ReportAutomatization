package com.reportplatform.lifecycle.repository;

import com.reportplatform.lifecycle.model.SubmissionChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionChecklistRepository extends JpaRepository<SubmissionChecklistEntity, UUID> {

    Optional<SubmissionChecklistEntity> findByReportId(UUID reportId);
}
