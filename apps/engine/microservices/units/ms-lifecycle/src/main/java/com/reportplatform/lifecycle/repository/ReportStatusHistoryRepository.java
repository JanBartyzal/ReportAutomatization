package com.reportplatform.lifecycle.repository;

import com.reportplatform.lifecycle.model.ReportStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportStatusHistoryRepository extends JpaRepository<ReportStatusHistoryEntity, UUID> {

    List<ReportStatusHistoryEntity> findByReportIdOrderByCreatedAtAsc(UUID reportId);
}
