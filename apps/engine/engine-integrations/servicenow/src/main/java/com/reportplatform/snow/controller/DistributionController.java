package com.reportplatform.snow.controller;

import com.reportplatform.snow.model.dto.CreateDistributionRuleRequest;
import com.reportplatform.snow.model.dto.DistributionHistoryDTO;
import com.reportplatform.snow.model.dto.DistributionRuleDTO;
import com.reportplatform.snow.service.DistributionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/integrations/servicenow")
public class DistributionController {

    private static final Logger logger = LoggerFactory.getLogger(DistributionController.class);

    private final DistributionService distributionService;

    public DistributionController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    // ==================== Distribution Rules ====================

    @PostMapping("/{connId}/distributions")
    public ResponseEntity<DistributionRuleDTO> createDistributionRule(
            @PathVariable UUID connId,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody CreateDistributionRuleRequest request) {
        logger.info("Creating distribution rule for connection: {} in org: {}", connId, orgId);

        DistributionRuleDTO created = distributionService.createRule(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{connId}/distributions")
    public ResponseEntity<List<DistributionRuleDTO>> listDistributionRules(
            @PathVariable UUID connId,
            @RequestParam(required = false) UUID scheduleId) {
        logger.debug("Listing distribution rules for connection: {}, scheduleId: {}", connId, scheduleId);

        if (scheduleId != null) {
            List<DistributionRuleDTO> rules = distributionService.getRulesBySchedule(scheduleId);
            return ResponseEntity.ok(rules);
        }
        // If no scheduleId filter, return rules by schedule (connId is contextual)
        // In practice, the frontend will always provide a scheduleId
        List<DistributionRuleDTO> rules = distributionService.getRulesBySchedule(connId);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/distributions/{id}")
    public ResponseEntity<DistributionRuleDTO> updateDistributionRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDistributionRuleRequest request) {
        logger.info("Updating distribution rule: {}", id);

        DistributionRuleDTO updated = distributionService.updateRule(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/distributions/{id}")
    public ResponseEntity<Void> deleteDistributionRule(@PathVariable UUID id) {
        logger.info("Deleting distribution rule: {}", id);

        distributionService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Distribution History ====================

    @GetMapping("/distributions/history")
    public ResponseEntity<Page<DistributionHistoryDTO>> listDistributionHistory(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.debug("Listing distribution history for org: {}", orgId);

        Pageable pageable = PageRequest.of(page, size);
        Page<DistributionHistoryDTO> history = distributionService.getHistory(orgId, pageable);
        return ResponseEntity.ok(history);
    }
}
