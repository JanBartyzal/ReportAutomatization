package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.ComparisonKpiRequest;
import com.reportplatform.dash.model.dto.MultiOrgComparisonRequest;
import com.reportplatform.dash.service.ComparisonService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonController.class);

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping("/kpis")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createKpi(
            @RequestHeader(value = "X-Org-Id") String orgIdStr,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userIdStr,
            @Valid @RequestBody ComparisonKpiRequest request) {
        try {
            UUID orgId = UUID.fromString(orgIdStr);
            UUID userId = parseUuidOrNull(userIdStr);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(comparisonService.createKpi(orgId, userId, request));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID in createKpi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid X-Org-Id header value", "detail", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create KPI: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create KPI", "detail", e.getMessage()));
        }
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> listKpis(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr) {
        try {
            if (orgIdStr == null || orgIdStr.isBlank()) {
                return ResponseEntity.ok(List.of());
            }
            UUID orgId = UUID.fromString(orgIdStr);
            return ResponseEntity.ok(comparisonService.listKpis(orgId));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid X-Org-Id in listKpis: {}", orgIdStr);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid X-Org-Id header value"));
        } catch (Exception e) {
            logger.error("Failed to list KPIs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list KPIs", "detail", e.getMessage()));
        }
    }

    @DeleteMapping("/kpis/{kpiId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> deactivateKpi(@PathVariable UUID kpiId) {
        try {
            comparisonService.deactivateKpi(kpiId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("KPI not found for deactivation: {}", kpiId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "KPI not found", "id", kpiId.toString()));
        } catch (Exception e) {
            logger.error("Failed to deactivate KPI {}: {}", kpiId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate KPI", "detail", e.getMessage()));
        }
    }

    @PostMapping("/multi-org")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> compareAcrossOrgs(
            @Valid @RequestBody MultiOrgComparisonRequest request) {
        try {
            return ResponseEntity.ok(comparisonService.compareAcrossOrgs(request));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request in compareAcrossOrgs: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid comparison request", "detail", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to compare across orgs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to compare across organizations", "detail", e.getMessage()));
        }
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
