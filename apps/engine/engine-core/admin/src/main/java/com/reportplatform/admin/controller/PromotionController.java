package com.reportplatform.admin.controller;

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
            logger.error("Failed to list promotion candidates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list promotion candidates", "detail", e.getMessage()));
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
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for candidate id: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid candidate id format", "id", id));
        } catch (Exception e) {
            logger.warn("Candidate not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Promotion candidate not found", "id", id));
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
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for candidate id: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid candidate id format", "id", id));
        } catch (Exception e) {
            logger.warn("Schema proposal not found for candidate {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Schema proposal not found for candidate", "id", id));
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
            logger.error("Update candidate failed for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update promotion candidate", "detail", e.getMessage()));
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
            logger.error("Approve candidate failed for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to approve promotion candidate", "detail", e.getMessage()));
        }
    }

    /**
     * Dismiss/reject a promotion candidate.
     */
    @DeleteMapping({"/{id}", "/candidates/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> rejectCandidate(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        logger.info("Rejecting promotion candidate: id={}, reviewedBy={}", id, userId);
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid candidate id format", "id", id));
        }
        try {
            promotionApprovalService.rejectCandidate(uuid, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Reject candidate failed for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reject promotion candidate", "detail", e.getMessage()));
        }
    }

    /**
     * Trigger historical data migration from JSONB to the promoted table.
     */
    @PostMapping({"/{id}/migrate", "/candidates/{id}/migrate"})
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> triggerMigration(@PathVariable String id) {
        logger.info("Triggering migration for promotion candidate: id={}", id);
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid candidate id format", "id", id));
        }
        try {
            promotionApprovalService.triggerMigration(uuid);
            return ResponseEntity.accepted().body(Map.of(
                    "candidate_id", id,
                    "status", "MIGRATING"));
        } catch (Exception e) {
            logger.error("Trigger migration failed for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to trigger migration", "detail", e.getMessage()));
        }
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
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for candidate id: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid candidate id format", "id", id));
        } catch (Exception e) {
            logger.error("Failed to get migration status for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve migration status", "id", id));
        }
    }
}
