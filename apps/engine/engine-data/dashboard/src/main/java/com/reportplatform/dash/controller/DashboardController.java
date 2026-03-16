package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.DashboardDataResponse;
import com.reportplatform.dash.model.dto.DashboardListResponse;
import com.reportplatform.dash.model.dto.DashboardRequest;
import com.reportplatform.dash.model.dto.DashboardResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonRequest;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse;
import com.reportplatform.dash.service.AggregationService;
import com.reportplatform.dash.service.DashboardExcelExportService;
import com.reportplatform.dash.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AggregationService aggregationService;
    private final DashboardExcelExportService excelExportService;

    public DashboardController(DashboardService dashboardService,
                               AggregationService aggregationService,
                               DashboardExcelExportService excelExportService) {
        this.dashboardService = dashboardService;
        this.aggregationService = aggregationService;
        this.excelExportService = excelExportService;
    }

    /**
     * List all dashboards accessible to the user (public + user's own).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DashboardListResponse> listDashboards(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader("X-User-Id") UUID userId) {

        var response = dashboardService.listDashboards(orgId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new dashboard configuration.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DashboardResponse> createDashboard(
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DashboardRequest request) {

        var response = dashboardService.createDashboard(orgId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a single dashboard by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId) {

        var response = dashboardService.getDashboard(id, orgId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing dashboard configuration.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DashboardResponse> updateDashboard(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody DashboardRequest request) {

        var response = dashboardService.updateDashboard(id, orgId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a dashboard.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId) {

        dashboardService.deleteDashboard(id, orgId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Execute an aggregation query against the dashboard's underlying data.
     * Queries parsed_tables JSONB data with dynamic GROUP BY and aggregation.
     */
    @PostMapping("/{id}/data")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DashboardDataResponse> executeDashboardQuery(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody DashboardDataRequest request) {

        // Verify the dashboard exists and belongs to the org
        dashboardService.getDashboard(id, orgId);

        var response = aggregationService.executeQuery(orgId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Compare aggregated data between two time periods.
     */
    @PostMapping("/period-comparison")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PeriodComparisonResponse> periodComparison(
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody PeriodComparisonRequest request) {

        var response = aggregationService.comparePeriods(orgId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Export dashboard aggregation results as Excel (.xlsx).
     * Executes the aggregation query and returns the result as a downloadable file.
     */
    @PostMapping("/{id}/data/export/excel")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> exportDashboardDataAsExcel(
            @PathVariable UUID id,
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody DashboardDataRequest request) throws IOException {

        dashboardService.getDashboard(id, orgId);

        var response = aggregationService.executeQuery(orgId, request);
        byte[] excelBytes = excelExportService.exportToExcel(response.data(), response.metadata());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "dashboard_export.xlsx");
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
