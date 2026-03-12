package com.reportplatform.audit.controller;

import com.reportplatform.audit.model.dto.*;
import com.reportplatform.audit.service.AiAuditLogService;
import com.reportplatform.audit.service.AuditLogService;
import com.reportplatform.audit.service.ExportService;
import com.reportplatform.audit.service.ReadAccessLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogService auditLogService;
    private final ReadAccessLogService readAccessLogService;
    private final AiAuditLogService aiAuditLogService;
    private final ExportService exportService;

    public AuditController(AuditLogService auditLogService,
                           ReadAccessLogService readAccessLogService,
                           AiAuditLogService aiAuditLogService,
                           ExportService exportService) {
        this.auditLogService = auditLogService;
        this.readAccessLogService = readAccessLogService;
        this.aiAuditLogService = aiAuditLogService;
        this.exportService = exportService;
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponse>> getLogs(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var filter = new AuditFilterRequest(userId, action, entityType, entityId, dateFrom, dateTo);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(auditLogService.queryLogs(orgId, filter, pageable));
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportLogs(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "csv") String format) {

        var filter = new AuditFilterRequest(userId, action, entityType, entityId, dateFrom, dateTo);
        StreamingResponseBody body = exportService.exportLogs(orgId, filter, format);

        String contentType = "csv".equalsIgnoreCase(format)
                ? "text/csv" : MediaType.APPLICATION_JSON_VALUE;
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=audit_export." + extension)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(body);
    }

    @GetMapping("/access-logs/{documentId}")
    public ResponseEntity<Page<ReadAccessLogResponse>> getAccessLogs(
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(readAccessLogService.getAccessHistory(documentId, pageable));
    }

    @GetMapping("/ai-logs")
    public ResponseEntity<Page<AiAuditLogResponse>> getAiLogs(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(aiAuditLogService.getAiAuditHistory(orgId, pageable));
    }
}
