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

    @SuppressWarnings("unchecked")
    @PostMapping("/{connId}/distributions")
    public ResponseEntity<?> createDistributionRule(
            @PathVariable UUID connId,
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestBody java.util.Map<String, Object> rawBody) {
        logger.info("Creating distribution rule for connection: {} in org: {}", connId, orgId);
        try {
            CreateDistributionRuleRequest request = new CreateDistributionRuleRequest();

            // Map scheduleId (required)
            Object scheduleIdObj = rawBody.get("scheduleId");
            if (scheduleIdObj == null) scheduleIdObj = rawBody.get("schedule_id");
            if (scheduleIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "scheduleId is required"));
            }
            request.setScheduleId(UUID.fromString(scheduleIdObj.toString()));

            // Map reportTemplateId (required)
            Object reportTemplateIdObj = rawBody.get("reportTemplateId");
            if (reportTemplateIdObj == null) reportTemplateIdObj = rawBody.get("report_template_id");
            if (reportTemplateIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "reportTemplateId is required"));
            }
            request.setReportTemplateId(UUID.fromString(reportTemplateIdObj.toString()));

            // Map recipients (required)
            Object recipientsObj = rawBody.get("recipients");
            if (recipientsObj instanceof java.util.List<?> recipientsList && !recipientsList.isEmpty()) {
                request.setRecipients((java.util.List<String>) recipientsList);
            } else {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "recipients list is required and must not be empty"));
            }

            // Map format (optional, default XLSX)
            Object formatObj = rawBody.get("format");
            request.setFormat(formatObj != null ? formatObj.toString().toUpperCase() : "XLSX");

            DistributionRuleDTO created = distributionService.createRule(orgId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for distribution rule creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Invalid request", "detail", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create distribution rule for connection {}: {}", connId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to create distribution rule", "detail", e.getMessage()));
        }
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
