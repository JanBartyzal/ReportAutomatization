package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.DrilldownReportDrillRequest;
import com.reportplatform.dash.model.dto.DrilldownReportListResponse;
import com.reportplatform.dash.model.dto.DrilldownReportQueryRequest;
import com.reportplatform.dash.model.dto.DrilldownReportQueryResponse;
import com.reportplatform.dash.model.dto.DrilldownReportRequest;
import com.reportplatform.dash.model.dto.DrilldownReportResponse;
import com.reportplatform.dash.model.dto.DrilldownReportViewStateRequest;
import com.reportplatform.dash.model.dto.DrilldownReportViewStateResponse;
import com.reportplatform.dash.service.DrilldownReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/drilldown-reports")
public class DrilldownReportController {

    private final DrilldownReportService drilldownReportService;

    public DrilldownReportController(DrilldownReportService drilldownReportService) {
        this.drilldownReportService = drilldownReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportListResponse> listReports(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        var response = drilldownReportService.listReports(parseUuidOrNull(orgIdStr), parseUuidOrNull(userIdStr));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportResponse> createReport(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @Valid @RequestBody DrilldownReportRequest request) {
        var response = drilldownReportService.createReport(
                parseUuidOrNull(orgIdStr),
                parseUuidOrNull(userIdStr),
                request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportResponse> getReport(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId) {
        return ResponseEntity.ok(drilldownReportService.getReport(id, orgId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportResponse> updateReport(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @Valid @RequestBody DrilldownReportRequest request) {
        return ResponseEntity.ok(drilldownReportService.updateReport(
                id,
                parseUuidOrNull(orgIdStr),
                parseUuidOrNull(userIdStr),
                request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteReport(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId) {
        drilldownReportService.deleteReport(id, orgId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/query")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportQueryResponse> queryReport(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestBody(required = false) DrilldownReportQueryRequest request) {
        return ResponseEntity.ok(drilldownReportService.queryReport(id, orgId, request));
    }

    @PostMapping("/{id}/drill")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> drill(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody DrilldownReportDrillRequest request) {
        return ResponseEntity.ok(drilldownReportService.drill(id, orgId, request));
    }

    @PostMapping("/{id}/view-state")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DrilldownReportViewStateResponse> saveViewState(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestBody(required = false) DrilldownReportViewStateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                drilldownReportService.saveViewState(id, orgId, parseUuidOrNull(userIdStr), request));
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
