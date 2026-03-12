package com.reportplatform.lifecycle.service;

import com.reportplatform.lifecycle.model.ReportStatusHistoryEntity;
import com.reportplatform.lifecycle.repository.ReportStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final ReportStatusHistoryRepository historyRepository;

    public AuditService(ReportStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void recordTransition(UUID reportId, String fromStatus, String toStatus,
                                 String userId, String comment) {
        var entry = new ReportStatusHistoryEntity(reportId, fromStatus, toStatus, userId, comment);
        historyRepository.save(entry);
        log.info("Audit: report={} {} -> {} by user={}", reportId, fromStatus, toStatus, userId);
    }

    public List<ReportStatusHistoryEntity> getHistory(UUID reportId) {
        return historyRepository.findByReportIdOrderByCreatedAtAsc(reportId);
    }
}
