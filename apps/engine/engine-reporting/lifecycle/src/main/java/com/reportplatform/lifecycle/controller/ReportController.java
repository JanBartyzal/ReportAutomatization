package com.reportplatform.lifecycle.controller;

import com.reportplatform.lifecycle.config.ReportScope;
import com.reportplatform.lifecycle.dto.BulkActionRequest;
import com.reportplatform.lifecycle.dto.BulkActionResult;
import com.reportplatform.lifecycle.dto.ChecklistResponse;
import com.reportplatform.lifecycle.dto.HistoryResponse;
import com.reportplatform.lifecycle.dto.MatrixEntry;
import com.reportplatform.lifecycle.dto.RejectRequest;
import com.reportplatform.lifecycle.dto.ReportCreateRequest;
import com.reportplatform.lifecycle.dto.ReportResponse;
import com.reportplatform.lifecycle.service.AuditService;
import com.reportplatform.lifecycle.service.ReportService;
import com.reportplatform.lifecycle.service.SubmissionChecklistService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuditService auditService;
    private final SubmissionChecklistService checklistService;

    public ReportController(ReportService reportService,
                            AuditService auditService,
                            SubmissionChecklistService checklistService) {
        this.reportService = reportService;
        this.auditService = auditService;
        this.checklistService = checklistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<ReportResponse> listReports(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) UUID periodId,
            @RequestParam(required = false) String scope,
            Pageable pageable) {
        ReportScope reportScope = scope != null ? ReportScope.valueOf(scope) : null;
        return reportService.listReports(orgId, periodId, reportScope, pageable)
                .map(ReportResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody ReportCreateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        ReportScope scope = request.scope() != null
                ? ReportScope.valueOf(request.scope())
                : ReportScope.CENTRAL;
        var report = reportService.createReport(
                request.orgId(), request.periodId(), request.reportType(), userId, scope);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ReportResponse getReport(@PathVariable UUID id) {
        return ReportResponse.from(reportService.getReport(id));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ReportResponse submitReport(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.submitReport(id, userId, userRole));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ReportResponse startReview(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.startReview(id, userId, userRole));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ReportResponse approveReport(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.approveReport(id, userId, userRole));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ReportResponse rejectReport(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(
                reportService.rejectReport(id, userId, userRole, request.comment()));
    }

    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ReportResponse resubmitReport(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.resubmitReport(id, userId, userRole));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ReportResponse completeReport(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.completeLocalReport(id, userId, userRole));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ReportResponse releaseReport(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return ReportResponse.from(reportService.releaseLocalReport(id, userId, userRole));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<HistoryResponse> getHistory(@PathVariable UUID id) {
        return auditService.getHistory(id).stream()
                .map(HistoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}/checklist")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<ChecklistResponse> getChecklist(@PathVariable UUID id) {
        return checklistService.getChecklist(id)
                .map(c -> ResponseEntity.ok(ChecklistResponse.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public BulkActionResult bulkApprove(
            @Valid @RequestBody BulkActionRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        return reportService.bulkApprove(request.reportIds(), userId, userRole);
    }

    @PostMapping("/bulk-reject")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public BulkActionResult bulkReject(
            @Valid @RequestBody BulkActionRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        if (request.comment() == null || request.comment().isBlank()) {
            throw new IllegalArgumentException("Comment is required for bulk reject");
        }
        return reportService.bulkReject(request.reportIds(), userId, userRole, request.comment());
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<MatrixEntry> getMatrix(
            @RequestParam UUID periodId,
            @RequestParam(required = false) String scope) {
        ReportScope reportScope = scope != null ? ReportScope.valueOf(scope) : null;
        return reportService.getMatrix(periodId, reportScope);
    }
}
