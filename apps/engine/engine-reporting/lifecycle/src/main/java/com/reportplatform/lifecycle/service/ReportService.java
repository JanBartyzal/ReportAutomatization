package com.reportplatform.lifecycle.service;

import com.reportplatform.lifecycle.config.ReportEvent;
import com.reportplatform.lifecycle.config.ReportScope;
import com.reportplatform.lifecycle.config.ReportState;
import com.reportplatform.lifecycle.config.ReportStateMachineConfig;
import com.reportplatform.lifecycle.dto.BulkActionResult;
import com.reportplatform.lifecycle.dto.MatrixEntry;
import com.reportplatform.lifecycle.exception.ChecklistIncompleteException;
import com.reportplatform.lifecycle.exception.DataLockedException;
import com.reportplatform.lifecycle.exception.InvalidTransitionException;
import com.reportplatform.lifecycle.exception.ReportNotFoundException;
import com.reportplatform.lifecycle.model.ReportEntity;
import com.reportplatform.lifecycle.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final StateMachineFactory<ReportState, ReportEvent> stateMachineFactory;
    private final AuditService auditService;
    private final DaprEventPublisher eventPublisher;
    private final SubmissionChecklistService checklistService;

    public ReportService(ReportRepository reportRepository,
                         StateMachineFactory<ReportState, ReportEvent> stateMachineFactory,
                         AuditService auditService,
                         DaprEventPublisher eventPublisher,
                         SubmissionChecklistService checklistService) {
        this.reportRepository = reportRepository;
        this.stateMachineFactory = stateMachineFactory;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.checklistService = checklistService;
    }

    @Transactional
    public ReportEntity createReport(String orgId, UUID periodId, String reportType,
                                     String userId) {
        return createReport(orgId, periodId, reportType, userId, ReportScope.CENTRAL);
    }

    @Transactional
    public ReportEntity createReport(String orgId, UUID periodId, String reportType,
                                     String userId, ReportScope scope) {
        reportRepository.findByOrgIdAndPeriodIdAndReportTypeAndScope(orgId, periodId, reportType, scope)
                .ifPresent(existing -> {
                    throw new InvalidTransitionException(
                            "Report already exists for org=" + orgId + " period=" + periodId +
                            " type=" + reportType + " scope=" + scope);
                });

        var report = new ReportEntity(orgId, periodId, reportType, userId, scope);
        report = reportRepository.save(report);

        checklistService.createDefaultChecklist(report.getId());

        auditService.recordTransition(report.getId(), null, ReportState.DRAFT.name(), userId,
                "Report created (scope=" + scope + ")");
        eventPublisher.publishStatusChanged(report.getId(), orgId, null, ReportState.DRAFT.name(), userId);

        log.info("Report created: id={} org={} period={} type={} scope={}",
                report.getId(), orgId, periodId, reportType, scope);
        return report;
    }

    public ReportEntity getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
    }

    public Page<ReportEntity> listReports(String orgId, UUID periodId, Pageable pageable) {
        return listReports(orgId, periodId, null, pageable);
    }

    public Page<ReportEntity> listReports(String orgId, UUID periodId, ReportScope scope, Pageable pageable) {
        if (orgId != null && periodId != null && scope != null) {
            return reportRepository.findByOrgIdAndPeriodIdAndScope(orgId, periodId, scope, pageable);
        } else if (orgId != null && scope != null) {
            return reportRepository.findByOrgIdAndScope(orgId, scope, pageable);
        } else if (periodId != null && scope != null) {
            return reportRepository.findByPeriodIdAndScope(periodId, scope, pageable);
        } else if (orgId != null && periodId != null) {
            return reportRepository.findByOrgIdAndPeriodId(orgId, periodId, pageable);
        } else if (periodId != null) {
            return reportRepository.findByPeriodId(periodId, pageable);
        } else if (orgId != null) {
            return reportRepository.findByOrgId(orgId, pageable);
        }
        return reportRepository.findAll(pageable);
    }

    @Transactional
    public ReportEntity submitReport(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        if (report.isLocked()) {
            throw new DataLockedException(reportId);
        }

        if (!checklistService.isComplete(reportId)) {
            var checklist = checklistService.getChecklist(reportId);
            int pct = checklist.map(c -> c.getCompletedPct()).orElse(0);
            throw new ChecklistIncompleteException(pct);
        }

        return executeTransition(report, ReportEvent.SUBMIT, userId, userRole, null);
    }

    @Transactional
    public ReportEntity startReview(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        var result = executeTransition(report, ReportEvent.START_REVIEW, userId, userRole, null);
        result.setReviewedBy(userId);
        result.setReviewedAt(Instant.now());
        return reportRepository.save(result);
    }

    @Transactional
    public ReportEntity approveReport(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        var result = executeTransition(report, ReportEvent.APPROVE, userId, userRole, null);

        result.setApprovedBy(userId);
        result.setApprovedAt(Instant.now());
        result.setLocked(true);
        result = reportRepository.save(result);

        eventPublisher.publishDataLocked(reportId, result.getOrgId(), userId);
        return result;
    }

    @Transactional
    public ReportEntity rejectReport(UUID reportId, String userId, String userRole, String comment) {
        var report = getReport(reportId);
        return executeTransition(report, ReportEvent.REJECT, userId, userRole, comment);
    }

    @Transactional
    public ReportEntity resubmitReport(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        var result = executeTransition(report, ReportEvent.RESUBMIT, userId, userRole, null);

        result.setLocked(false);
        result.setSubmittedBy(null);
        result.setSubmittedAt(null);
        result.setReviewedBy(null);
        result.setReviewedAt(null);
        result.setApprovedBy(null);
        result.setApprovedAt(null);
        return reportRepository.save(result);
    }

    @Transactional
    public ReportEntity completeLocalReport(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        if (report.getScope() != ReportScope.LOCAL) {
            throw new InvalidTransitionException("COMPLETE transition only allowed for LOCAL scope reports");
        }
        if (report.isLocked()) {
            throw new DataLockedException(reportId);
        }

        var result = executeTransition(report, ReportEvent.COMPLETE, userId, userRole, null);
        result.setCompletedBy(userId);
        result.setCompletedAt(Instant.now());
        result.setLocked(true);
        return reportRepository.save(result);
    }

    @Transactional
    public ReportEntity releaseLocalReport(UUID reportId, String userId, String userRole) {
        var report = getReport(reportId);
        if (report.getScope() != ReportScope.LOCAL) {
            throw new InvalidTransitionException("RELEASE transition only allowed for LOCAL scope reports");
        }
        if (report.getStatus() != ReportState.COMPLETED) {
            throw new InvalidTransitionException("RELEASE only allowed from COMPLETED state");
        }

        var result = executeTransition(report, ReportEvent.RELEASE, userId, userRole, null);
        result.setScope(ReportScope.CENTRAL);
        result.setReleasedBy(userId);
        result.setReleasedAt(Instant.now());
        result.setLocked(false);
        result.setSubmittedBy(userId);
        result.setSubmittedAt(Instant.now());
        result = reportRepository.save(result);

        eventPublisher.publishLocalReleased(reportId, result.getOrgId(), userId);
        return result;
    }

    @Transactional
    public BulkActionResult bulkApprove(List<UUID> reportIds, String userId, String userRole) {
        return executeBulkAction(reportIds, ReportEvent.APPROVE, userId, userRole, null);
    }

    @Transactional
    public BulkActionResult bulkReject(List<UUID> reportIds, String userId, String userRole, String comment) {
        return executeBulkAction(reportIds, ReportEvent.REJECT, userId, userRole, comment);
    }

    public List<MatrixEntry> getMatrix(UUID periodId) {
        return getMatrix(periodId, null);
    }

    public List<MatrixEntry> getMatrix(UUID periodId, ReportScope scope) {
        if (scope != null) {
            return reportRepository.findMatrixByPeriodIdAndScope(periodId, scope).stream()
                    .map(row -> new MatrixEntry(
                            (String) row[0],
                            (UUID) row[1],
                            ((ReportState) row[2]).name(),
                            ((ReportScope) row[3]).name(),
                            (Long) row[4]
                    ))
                    .toList();
        }
        return reportRepository.findMatrixByPeriodIdWithScope(periodId).stream()
                .map(row -> new MatrixEntry(
                        (String) row[0],
                        (UUID) row[1],
                        ((ReportState) row[2]).name(),
                        ((ReportScope) row[3]).name(),
                        (Long) row[4]
                ))
                .toList();
    }

    private ReportEntity executeTransition(ReportEntity report, ReportEvent event,
                                           String userId, String userRole, String comment) {
        ReportState fromState = report.getStatus();
        StateMachine<ReportState, ReportEvent> sm = buildStateMachine(report);

        var message = org.springframework.messaging.support.MessageBuilder
                .withPayload(event)
                .setHeader(ReportStateMachineConfig.HEADER_REPORT_ID, report.getId().toString())
                .setHeader(ReportStateMachineConfig.HEADER_USER_ID, userId)
                .setHeader(ReportStateMachineConfig.HEADER_USER_ROLE, userRole)
                .setHeader(ReportStateMachineConfig.HEADER_COMMENT, comment)
                .setHeader(ReportStateMachineConfig.HEADER_SCOPE,
                        report.getScope() != null ? report.getScope().name() : ReportScope.CENTRAL.name())
                .build();

        boolean accepted = sm.sendEvent(message);
        if (!accepted) {
            throw new InvalidTransitionException(
                    "Transition " + event + " not allowed from state " + fromState +
                    " for role " + userRole);
        }

        ReportState toState = sm.getState().getId();
        report.setStatus(toState);

        if (event == ReportEvent.SUBMIT) {
            report.setSubmittedBy(userId);
            report.setSubmittedAt(Instant.now());
        }

        report = reportRepository.save(report);

        auditService.recordTransition(report.getId(), fromState.name(), toState.name(), userId, comment);
        eventPublisher.publishStatusChanged(report.getId(), report.getOrgId(),
                fromState.name(), toState.name(), userId);

        return report;
    }

    private BulkActionResult executeBulkAction(List<UUID> reportIds, ReportEvent event,
                                               String userId, String userRole, String comment) {
        List<UUID> failedIds = new ArrayList<>();
        int succeeded = 0;

        for (UUID reportId : reportIds) {
            try {
                if (event == ReportEvent.APPROVE) {
                    approveReport(reportId, userId, userRole);
                } else if (event == ReportEvent.REJECT) {
                    rejectReport(reportId, userId, userRole, comment);
                }
                succeeded++;
            } catch (Exception e) {
                log.warn("Bulk {} failed for report {}: {}", event, reportId, e.getMessage());
                failedIds.add(reportId);
            }
        }

        return new BulkActionResult(reportIds.size(), succeeded, failedIds.size(), failedIds);
    }

    private StateMachine<ReportState, ReportEvent> buildStateMachine(ReportEntity report) {
        StateMachine<ReportState, ReportEvent> sm = stateMachineFactory.getStateMachine(report.getId().toString());
        sm.stop();
        sm.getStateMachineAccessor().doWithAllRegions(accessor ->
                accessor.resetStateMachine(new DefaultStateMachineContext<>(report.getStatus(), null, null, null))
        );
        sm.start();
        return sm;
    }
}
