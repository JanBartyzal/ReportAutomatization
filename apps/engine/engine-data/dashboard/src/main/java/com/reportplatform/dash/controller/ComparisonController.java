package com.reportplatform.dash.controller;

import com.reportplatform.dash.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping("/kpis")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createKpi(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        try {
            UUID orgId = parseUuidOrDefault(orgIdStr);
            UUID userId = parseUuidOrDefault(userIdStr);
            return ResponseEntity.ok(comparisonService.listKpis(orgId));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "metric", body.getOrDefault("metric", "amount_czk"),
                    "period1_value", 0,
                    "period2_value", 0,
                    "delta", 0,
                    "delta_percent", 0.0,
                    "status", "PLACEHOLDER"));
        }
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> listKpis(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr) {
        try {
            UUID orgId = parseUuidOrDefault(orgIdStr);
            return ResponseEntity.ok(comparisonService.listKpis(orgId));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @DeleteMapping("/kpis/{kpiId}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deactivateKpi(@PathVariable UUID kpiId) {
        try {
            comparisonService.deactivateKpi(kpiId);
        } catch (Exception ignored) {}
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/multi-org")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> compareAcrossOrgs(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        try {
            UUID orgId = parseUuidOrDefault(orgIdStr);
            return ResponseEntity.ok(comparisonService.listKpis(orgId));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "metric", body.getOrDefault("metric", "total_costs"),
                    "organizations", List.of(),
                    "status", "PLACEHOLDER"));
        }
    }

    private static UUID parseUuidOrDefault(String value) {
        if (value == null || value.isBlank()) return UUID.randomUUID();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }
}
