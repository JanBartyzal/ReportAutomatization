package com.reportplatform.template.tmpl.controller;

import com.reportplatform.template.tmpl.entity.PromotedTableEntity;
import com.reportplatform.template.tmpl.service.PromotionService;
import com.reportplatform.template.tmpl.service.PromotionService.PromotionCandidate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Smart Persistence Promotion admin operations (FS24).
 * Allows admins to view promotion candidates, approve promotions, and trigger migrations.
 */
@RestController
@RequestMapping("/api/admin/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /**
     * List promotion candidates - mapping templates with high usage that could
     * benefit from a dedicated SQL table.
     */
    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<PromotionCandidate>> getCandidates(
            @RequestParam(value = "threshold", defaultValue = "5") long threshold) {
        List<PromotionCandidate> candidates = promotionService.getCandidates(threshold);
        return ResponseEntity.ok(candidates);
    }

    /**
     * Approve a promotion candidate - creates the dedicated SQL table with RLS.
     */
    @PostMapping("/{mappingTemplateId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> approvePromotion(
            @PathVariable UUID mappingTemplateId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) Map<String, String> body) {

        String finalDdl = body != null ? body.get("ddl") : null;

        PromotedTableEntity result = promotionService.approvePromotion(mappingTemplateId, finalDdl, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", result.getId().toString(),
                "tableName", result.getTableName(),
                "status", result.getStatus(),
                "dualWriteUntil", result.getDualWriteUntil().toString()
        ));
    }

    /**
     * Get routing info for a mapping template - check if it has a promoted table.
     */
    @GetMapping("/{mappingTemplateId}/routing")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PromotionService.RoutingInfo> getRoutingInfo(
            @PathVariable UUID mappingTemplateId) {
        var info = promotionService.getRoutingInfo(mappingTemplateId);
        return ResponseEntity.ok(info);
    }

    /**
     * Trigger data migration from JSONB to the promoted table.
     */
    @PostMapping("/{promotionId}/migrate")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> migrateData(
            @PathVariable UUID promotionId) {
        int count = promotionService.migrateData(promotionId);
        return ResponseEntity.ok(Map.of(
                "promotionId", promotionId.toString(),
                "recordsMigrated", count,
                "status", "COMPLETED"
        ));
    }
}
