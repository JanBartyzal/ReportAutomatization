package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.*;
import com.reportplatform.dash.service.ComparisonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping("/kpis")
    public ResponseEntity<ComparisonKpiResponse> createKpi(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ComparisonKpiRequest request) {
        var response = comparisonService.createKpi(orgId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/kpis")
    public ResponseEntity<List<ComparisonKpiResponse>> listKpis(
            @RequestHeader("X-Org-Id") UUID orgId) {
        return ResponseEntity.ok(comparisonService.listKpis(orgId));
    }

    @DeleteMapping("/kpis/{kpiId}")
    public ResponseEntity<Void> deactivateKpi(@PathVariable UUID kpiId) {
        comparisonService.deactivateKpi(kpiId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/multi-org")
    public ResponseEntity<MultiOrgComparisonResponse> compareAcrossOrgs(
            @Valid @RequestBody MultiOrgComparisonRequest request) {
        return ResponseEntity.ok(comparisonService.compareAcrossOrgs(request));
    }
}
