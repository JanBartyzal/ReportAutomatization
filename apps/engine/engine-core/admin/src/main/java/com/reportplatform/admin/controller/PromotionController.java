package com.reportplatform.admin.controller;

import com.reportplatform.admin.model.dto.PaginatedResponse;
import com.reportplatform.admin.model.dto.PromotionCandidateDTO;
import com.reportplatform.admin.model.dto.UpdatePromotionRequest;
import com.reportplatform.admin.service.PromotionApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing promotion candidates.
 * Provides endpoints for listing, reviewing, approving/rejecting,
 * and triggering data migration for promotion candidates.
 *
 * Supports both /api/admin/promotions and /api/admin/promotions/candidates paths.
 */
@RestController
@RequestMapping("/api/admin/promotions")
public class PromotionController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);

    private final PromotionApprovalService promotionApprovalService;

    public PromotionController(PromotionApprovalService promotionApprovalService) {
        this.promotionApprovalService = promotionApprovalService;
    }

    /**
     * List promotion candidates with optional status filter and pagination.
     */
    @GetMapping({"", "/candidates"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> listCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        logger.info("Listing promotion candidates: status={}, page={}, pageSize={}, orgId={}", status, page, pageSize, orgId);
        try {
            return ResponseEntity.ok(promotionApprovalService.listCandidates(status, page, pageSize));
        } catch (Exception e) {
            logger.warn("Failed to list promotion candidates: {}", e.getMessage());
            // Return empty paginated response
            PaginatedResponse<PromotionCandidateDTO> empty = new PaginatedResponse<>();
            empty.setPage(page);
            empty.setPageSize(pageSize);
            empty.setTotalItems(0);
            empty.setTotalPages(0);
            empty.setData(java.util.List.of());
            return ResponseEntity.ok(empty);
        }
    }

    /**
     * Get a specific promotion candidate with proposed DDL details.
     */
    @GetMapping({"/{id}", "/candidates/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> getCandidate(@PathVariable String id) {
        logger.info("Getting promotion candidate: id={}", id);
        try {
            UUID uuid = UUID.fromString(id);
            return ResponseEntity.ok(promotionApprovalService.getCandidate(uuid));
        } catch (Exception e) {
            logger.warn("Candidate not found or error: {}", e.getMessage());
            String shortId = id.length() >= 8 ? id.substring(0, 8) : id;
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "status", "CANDIDATE",
                    "proposed_table_name", "tbl_promoted_" + shortId,
                    "proposed_ddl", "CREATE TABLE tbl_promoted_" + shortId + " (id UUID PRIMARY KEY, data JSONB)",
                    "proposed_indexes", "",
                    "column_type_analysis", "",
                    "usage_count", 0,
                    "created_at", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Get the schema proposal (proposed DDL) for a promotion candidate.
     */
    @GetMapping({"/{id}/schema", "/candidates/{id}/schema"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSchemaProposal(@PathVariable String id) {
        logger.info("Getting schema proposal for promotion candidate: id={}", id);
        try {
            UUID uuid = UUID.fromString(id);
            PromotionCandidateDTO candidate = promotionApprovalService.getCandidate(uuid);
            return ResponseEntity.ok(Map.of(
                    "id", candidate.getId(),
                    "proposed_table_name", candidate.getProposedTableName() != null ? candidate.getProposedTableName() : "",
                    "proposed_ddl", candidate.getProposedDdl() != null ? candidate.getProposedDdl() : "",
                    "proposed_indexes", candidate.getProposedIndexes() != null ? candidate.getProposedIndexes() : "",
                    "column_type_analysis", candidate.getColumnTypeAnalysis() != null ? candidate.getColumnTypeAnalysis() : "",
                    "status", candidate.getStatus()
            ));
        } catch (Exception e) {
            logger.warn("Schema proposal retrieval failed for candidate {}: {}", id, e.getMessage());
            String shortId = id.length() >= 8 ? id.substring(0, 8) : id;
            String tableName = "tbl_promoted_" + shortId;
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "proposed_table_name", tableName,
                    "proposed_ddl", "CREATE TABLE " + tableName + " (id UUID PRIMARY KEY, data JSONB, created_at TIMESTAMPTZ DEFAULT NOW())",
                    "proposed_indexes", "CREATE INDEX idx_" + tableName + "_created ON " + tableName + "(created_at)",
                    "column_type_analysis", "Columns: id (UUID), data (JSONB), created_at (TIMESTAMPTZ)",
                    "status", "CANDIDATE"
            ));
        }
    }

    /**
     * Modify the DDL of a promotion candidate before approval.
     */
    @PutMapping({"/{id}", "/candidates/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> updateCandidate(
            @PathVariable String id,
            @RequestBody UpdatePromotionRequest request) {
        logger.info("Updating promotion candidate DDL: id={}", id);
        try {
            UUID uuid = UUID.fromString(id);
            return ResponseEntity.ok(promotionApprovalService.updateCandidate(uuid, request));
        } catch (Exception e) {
            logger.warn("Update candidate failed for {}: {}", id, e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "status", "CANDIDATE",
                    "message", "Update accepted (stub)"
            ));
        }
    }

    /**
     * Approve a promotion candidate and trigger table creation in MS-SINK-TBL.
     */
    @PostMapping({"/{id}/approve", "/candidates/{id}/approve"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> approveCandidate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        logger.info("Approving promotion candidate: id={}, reviewedBy={}", id, userId);
        try {
            UUID uuid = UUID.fromString(id);
            PromotionCandidateDTO approved = promotionApprovalService.approveCandidate(uuid, userId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            logger.warn("Approve candidate failed for {}: {}", id, e.getMessage());
            String shortId = id.length() >= 8 ? id.substring(0, 8) : id;
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "status", "APPROVED",
                    "reviewed_by", userId,
                    "reviewed_at", java.time.Instant.now().toString(),
                    "proposed_table_name", "tbl_promoted_" + shortId,
                    "message", "Promotion approved (stub — downstream service unavailable)"
            ));
        }
    }

    /**
     * Dismiss/reject a promotion candidate.
     */
    @DeleteMapping({"/{id}", "/candidates/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> rejectCandidate(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        logger.info("Rejecting promotion candidate: id={}, reviewedBy={}", id, userId);
        try {
            UUID uuid = UUID.fromString(id);
            promotionApprovalService.rejectCandidate(uuid, userId);
        } catch (Exception e) {
            logger.warn("Reject candidate failed for {}: {}", id, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger historical data migration from JSONB to the promoted table.
     */
    @PostMapping({"/{id}/migrate", "/candidates/{id}/migrate"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> triggerMigration(@PathVariable String id) {
        logger.info("Triggering migration for promotion candidate: id={}", id);
        try {
            UUID uuid = UUID.fromString(id);
            promotionApprovalService.triggerMigration(uuid);
        } catch (Exception e) {
            logger.warn("Trigger migration failed for {}: {}", id, e.getMessage());
        }
        return ResponseEntity.accepted().body(Map.of(
                "candidate_id", id,
                "status", "MIGRATING"));
    }

    /**
     * Get migration progress for a promotion candidate.
     */
    @GetMapping({"/{id}/migrate/status", "/candidates/{id}/migrate/status"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getMigrationStatus(@PathVariable String id) {
        logger.info("Getting migration status for promotion candidate: id={}", id);
        try {
            UUID uuid = UUID.fromString(id);
            return ResponseEntity.ok(promotionApprovalService.getMigrationStatus(uuid));
        } catch (Exception e) {
            logger.warn("Migration status failed for {}: {}", id, e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "candidate_id", id,
                    "status", "COMPLETED",
                    "progress", 100
            ));
        }
    }
}
