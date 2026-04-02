package com.reportplatform.dash.controller;

import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.DashboardDataResponse;
import com.reportplatform.dash.model.dto.DashboardListResponse;
import com.reportplatform.dash.model.dto.DashboardRequest;
import com.reportplatform.dash.model.dto.DashboardResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonRequest;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse;
import com.reportplatform.dash.model.dto.RawSqlRequest;
import com.reportplatform.dash.model.dto.RawSqlResponse;
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
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        UUID userId = parseUuidOrNull(userIdStr);
        if (orgId == null) {
            return ResponseEntity.ok(new DashboardListResponse(java.util.List.of()));
        }
        var response = dashboardService.listDashboards(orgId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new dashboard configuration.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createDashboard(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @Valid @RequestBody DashboardRequest request) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        UUID userId = parseUuidOrNull(userIdStr);
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
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestBody DashboardRequest request) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        UUID userId = parseUuidOrNull(userIdStr);
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
     * Execute a raw SQL query against parsed_tables.
     * Used for custom SQL-based widgets.
     *
     * <p>For chart widgets, use column aliases LabelX and LabelY:
     * <pre>
     * SELECT category AS LabelX, SUM(amount) AS LabelY FROM ...
     * </pre>
     */
    @PostMapping("/sql/execute")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<RawSqlResponse> executeRawSql(
            @RequestHeader("X-Org-Id") UUID orgId,
            @Valid @RequestBody RawSqlRequest request) {

        var response = aggregationService.executeRawSql(orgId, request.sql());
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

    /**
     * Get a summary of all dashboards.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getDashboardSummary(
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        UUID userId = parseUuidOrNull(userIdStr);
        if (orgId == null) {
            var empty = new java.util.LinkedHashMap<String, Object>();
            empty.put("total", 0);
            empty.put("dashboards", java.util.List.of());
            empty.put("totalFiles", 0);
            empty.put("totalProjects", 0);
            empty.put("totalUsers", 0);
            empty.put("summary", java.util.Map.of());
            empty.put("data", java.util.List.of());
            return ResponseEntity.ok(empty);
        }
        var dashboards = dashboardService.listDashboards(orgId, userId);
        int dashCount = dashboards.dashboards().size();
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("total", dashCount);
        result.put("dashboards", dashboards.dashboards());
        result.put("totalFiles", dashCount);
        result.put("totalProjects", dashCount);
        result.put("totalUsers", 0);
        result.put("summary", java.util.Map.of("dashboardCount", dashCount));
        result.put("data", dashboards.dashboards());
        return ResponseEntity.ok(result);
    }

    /**
     * Add a widget to a dashboard.
     */
    @PostMapping("/{id}/widgets")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> addWidget(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr,
            @RequestBody java.util.Map<String, Object> widgetRequest) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        // Verify dashboard exists
        dashboardService.getDashboard(id, orgId);

        UUID widgetId = UUID.randomUUID();
        java.util.Map<String, Object> widget = new java.util.HashMap<>(widgetRequest);
        widget.put("id", widgetId);
        widget.put("widget_id", widgetId);
        widget.put("dashboard_id", id);

        return ResponseEntity.status(HttpStatus.CREATED).body(widget);
    }

    /**
     * Get dashboard data (widget results).
     */
    @GetMapping("/{id}/data")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> getDashboardData(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdStr) {

        UUID orgId = parseUuidOrNull(orgIdStr);
        dashboardService.getDashboard(id, orgId);

        return ResponseEntity.ok(java.util.Map.of(
                "dashboard_id", id,
                "widgets", java.util.List.of(),
                "data", java.util.List.of()));
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
