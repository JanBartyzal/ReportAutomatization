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
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PaginatedResponse<PromotionCandidateDTO>> listCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        logger.info("Listing promotion candidates: status={}, page={}, pageSize={}", status, page, pageSize);
        return ResponseEntity.ok(promotionApprovalService.listCandidates(status, page, pageSize));
    }

    /**
     * Get a specific promotion candidate with proposed DDL details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PromotionCandidateDTO> getCandidate(@PathVariable UUID id) {
        logger.info("Getting promotion candidate: id={}", id);
        return ResponseEntity.ok(promotionApprovalService.getCandidate(id));
    }

    /**
     * Modify the DDL of a promotion candidate before approval.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PromotionCandidateDTO> updateCandidate(
            @PathVariable UUID id,
            @RequestBody UpdatePromotionRequest request) {
        logger.info("Updating promotion candidate DDL: id={}", id);
        return ResponseEntity.ok(promotionApprovalService.updateCandidate(id, request));
    }

    /**
     * Approve a promotion candidate and trigger table creation in MS-SINK-TBL.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PromotionCandidateDTO> approveCandidate(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Approving promotion candidate: id={}, reviewedBy={}", id, userId);
        PromotionCandidateDTO approved = promotionApprovalService.approveCandidate(id, userId);
        return ResponseEntity.ok(approved);
    }

    /**
     * Dismiss/reject a promotion candidate.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> rejectCandidate(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Rejecting promotion candidate: id={}, reviewedBy={}", id, userId);
        promotionApprovalService.rejectCandidate(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger historical data migration from JSONB to the promoted table.
     */
    @PostMapping("/{id}/migrate")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, String>> triggerMigration(@PathVariable UUID id) {
        logger.info("Triggering migration for promotion candidate: id={}", id);
        promotionApprovalService.triggerMigration(id);
        return ResponseEntity.accepted().body(Map.of(
                "candidate_id", id.toString(),
                "status", "MIGRATING"));
    }

    /**
     * Get migration progress for a promotion candidate.
     */
    @GetMapping("/{id}/migrate/status")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getMigrationStatus(@PathVariable UUID id) {
        logger.info("Getting migration status for promotion candidate: id={}", id);
        return ResponseEntity.ok(promotionApprovalService.getMigrationStatus(id));
    }
}
