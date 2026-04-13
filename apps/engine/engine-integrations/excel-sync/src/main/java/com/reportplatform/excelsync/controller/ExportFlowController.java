package com.reportplatform.excelsync.controller;

import com.reportplatform.excelsync.config.ExcelSyncProperties;
import com.reportplatform.excelsync.connector.LocalPathWriter;
import com.reportplatform.excelsync.model.dto.*;
import com.reportplatform.excelsync.model.entity.ExecutionStatus;
import com.reportplatform.excelsync.model.entity.ExportFlowExecutionEntity;
import com.reportplatform.excelsync.repository.ExportFlowExecutionRepository;
import com.reportplatform.excelsync.service.ExportFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Export Flows", description = "Live Excel Export & External Sync – FS27")
@RestController
@RequestMapping("/api/export-flows")
public class ExportFlowController {

    private static final Logger log = LoggerFactory.getLogger(ExportFlowController.class);

    private final ExportFlowService exportFlowService;
    private final ExportFlowExecutionRepository executionRepository;
    private final ExcelSyncProperties excelSyncProperties;

    public ExportFlowController(ExportFlowService exportFlowService,
                                ExportFlowExecutionRepository executionRepository,
                                ExcelSyncProperties excelSyncProperties) {
        this.exportFlowService = exportFlowService;
        this.executionRepository = executionRepository;
        this.excelSyncProperties = excelSyncProperties;
    }

    @Operation(summary = "Module health check", description = "Returns UP/DOWN status for local-path and SharePoint connectors. Does not require authentication.")
    @ApiResponse(responseCode = "200", description = "Module status")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");

        // Check local path accessibility
        String localPathStatus = "UP";
        for (String allowedPath : excelSyncProperties.getAllowedPaths()) {
            File dir = new File(allowedPath);
            if (!dir.exists() || !dir.canWrite()) {
                localPathStatus = "DOWN";
                break;
            }
        }
        status.put("localPath", localPathStatus);

        // SharePoint status is reported as UNKNOWN unless actively configured
        status.put("sharepoint", "UNKNOWN");

        return ResponseEntity.ok(status);
    }

    @Operation(summary = "List export flows", description = "Returns all active export flow definitions for the requesting organisation.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "List of export flows") })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER', 'HOLDING_ADMIN')")
    public ResponseEntity<?> listFlows(
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId) {
        if (orgId == null) {
            return ResponseEntity.ok(java.util.List.of());
        }
        return ResponseEntity.ok(exportFlowService.listFlows(orgId));
    }

    @Operation(summary = "Get export flow", description = "Returns a single export flow definition by ID.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Export flow found"), @ApiResponse(responseCode = "404", description = "Not found") })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER', 'HOLDING_ADMIN')")
    public ResponseEntity<?> getFlow(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId) {
        try {
            return ResponseEntity.ok(exportFlowService.getFlow(id, orgId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Create export flow", description = "Creates a new export flow definition. Returns 201 with the created resource.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error") })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'HOLDING_ADMIN')")
    public ResponseEntity<?> createFlow(
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @Valid @RequestBody CreateExportFlowRequest request) {
        try {
            UUID effectiveOrgId = orgId != null ? orgId : UUID.randomUUID();
            ExportFlowDTO created = exportFlowService.createFlow(effectiveOrgId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update export flow", description = "Updates an existing export flow definition.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Updated"), @ApiResponse(responseCode = "400", description = "Validation error"), @ApiResponse(responseCode = "404", description = "Not found") })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'HOLDING_ADMIN')")
    public ResponseEntity<?> updateFlow(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @Valid @RequestBody UpdateExportFlowRequest request) {
        try {
            return ResponseEntity.ok(exportFlowService.updateFlow(id, orgId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Delete export flow", description = "Soft-deletes an export flow by setting is_active=false.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Deleted"), @ApiResponse(responseCode = "404", description = "Not found") })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteFlow(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId) {
        try {
            exportFlowService.softDeleteFlow(id, orgId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Manually trigger export flow", description = "Creates a PENDING execution record. The async worker picks it up and performs the export. Returns 202 Accepted with the executionId.")
    @ApiResponses({ @ApiResponse(responseCode = "202", description = "Execution queued"), @ApiResponse(responseCode = "404", description = "Flow not found") })
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'HOLDING_ADMIN')")
    public ResponseEntity<?> executeFlow(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            // Create a pending execution record
            ExportFlowDTO flow = exportFlowService.getFlow(id, orgId);
            ExportFlowExecutionEntity execution = new ExportFlowExecutionEntity();
            execution.setFlowId(id);
            execution.setOrgId(orgId);
            execution.setTriggerSource("MANUAL");
            execution.setStatus(ExecutionStatus.PENDING);
            execution.setStartedAt(Instant.now());
            execution = executionRepository.save(execution);

            log.info("Manual execution [{}] created for flow [{}] by user [{}]",
                    execution.getId(), id, userId);

            return ResponseEntity.accepted().body(Map.of(
                    "executionId", execution.getId().toString(),
                    "flowId", id.toString(),
                    "status", "PENDING"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get execution history", description = "Returns paginated execution history for the given flow, sorted by started_at DESC.")
    @ApiResponse(responseCode = "200", description = "Page of executions")
    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER', 'HOLDING_ADMIN')")
    public ResponseEntity<?> getExecutions(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<ExportFlowExecutionDTO> executions = executionRepository
                .findByFlowIdAndOrgIdOrderByStartedAtDesc(id, orgId, pageRequest)
                .map(ExportFlowExecutionDTO::fromEntity);
        return ResponseEntity.ok(executions);
    }

    @Operation(summary = "Dry-run test export flow", description = "Executes the SQL query and returns a preview of the rows that would be exported. No file is written.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Preview data"), @ApiResponse(responseCode = "400", description = "Query error or flow not found") })
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'HOLDING_ADMIN')")
    public ResponseEntity<?> testFlow(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) UUID orgId) {
        try {
            ExportFlowTestResult result = exportFlowService.testFlow(id, orgId);
            if (result.getError() != null) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
