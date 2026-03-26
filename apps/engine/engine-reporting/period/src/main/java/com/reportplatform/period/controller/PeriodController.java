package com.reportplatform.period.controller;

import com.reportplatform.period.dto.CloneRequest;
import com.reportplatform.period.dto.CrossTypeComparisonRequest;
import com.reportplatform.period.dto.CrossTypeComparisonResponse;
import com.reportplatform.period.dto.PeriodCreateRequest;
import com.reportplatform.period.dto.PeriodResponse;
import com.reportplatform.period.dto.PeriodStatusResponse;
import com.reportplatform.period.dto.PeriodUpdateRequest;
import com.reportplatform.period.service.CompletionTrackingService;
import com.reportplatform.period.service.CrossTypeComparisonService;
import com.reportplatform.period.service.ExportService;
import com.reportplatform.period.service.PeriodCloneService;
import com.reportplatform.period.service.PeriodService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/periods")
public class PeriodController {

    private final PeriodService periodService;
    private final PeriodCloneService cloneService;
    private final CompletionTrackingService completionTrackingService;
    private final ExportService exportService;
    private final CrossTypeComparisonService crossTypeComparisonService;

    public PeriodController(PeriodService periodService,
                            PeriodCloneService cloneService,
                            CompletionTrackingService completionTrackingService,
                            ExportService exportService,
                            CrossTypeComparisonService crossTypeComparisonService) {
        this.periodService = periodService;
        this.cloneService = cloneService;
        this.completionTrackingService = completionTrackingService;
        this.exportService = exportService;
        this.crossTypeComparisonService = crossTypeComparisonService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<PeriodResponse> listPeriods(
            @RequestParam(required = false) String holdingId,
            Pageable pageable) {
        return periodService.listPeriods(holdingId, pageable)
                .map(PeriodResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createPeriod(
            @Valid @RequestBody PeriodCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        var period = periodService.createPeriod(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodResponse.from(period));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public PeriodResponse getPeriod(@PathVariable UUID id) {
        return PeriodResponse.from(periodService.getPeriod(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public PeriodResponse updatePeriod(
            @PathVariable UUID id,
            @RequestBody PeriodUpdateRequest request) {
        return PeriodResponse.from(periodService.updatePeriod(id, request));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<PeriodResponse> clonePeriod(
            @PathVariable UUID id,
            @Valid @RequestBody CloneRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var cloned = cloneService.clonePeriod(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodResponse.from(cloned));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public PeriodStatusResponse getStatus(@PathVariable UUID id) {
        return completionTrackingService.getCompletionStatus(id);
    }

    @PostMapping("/{id}/collect")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public PeriodResponse collectPeriod(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return PeriodResponse.from(periodService.transitionToCollecting(id, userId));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public PeriodResponse closePeriod(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return PeriodResponse.from(periodService.closePeriod(id, userId));
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public PeriodStatusResponse getDashboard(@PathVariable UUID id) {
        return completionTrackingService.getCompletionStatus(id);
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<CrossTypeComparisonResponse> comparePeriods(
            @RequestBody CrossTypeComparisonRequest request) {
        try {
            var response = crossTypeComparisonService.buildComparisonContext(request.effectivePeriodIds());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return empty comparison on error
            return ResponseEntity.ok(new CrossTypeComparisonResponse(
                    java.util.List.of(), java.util.List.of()));
        }
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<CrossTypeComparisonResponse> comparePeriodsGet(
            @RequestParam(name = "period1") UUID period1,
            @RequestParam(name = "period2") UUID period2) {
        try {
            var response = crossTypeComparisonService.buildComparisonContext(
                    java.util.List.of(period1, period2));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new CrossTypeComparisonResponse(
                    java.util.List.of(), java.util.List.of()));
        }
    }

    @PostMapping("/compare/export")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> exportComparison(
            @RequestBody(required = false) java.util.Map<String, Object> body,
            @RequestParam(defaultValue = "pptx") String format) {

        try {
            byte[] data = exportService.exportAsExcel(null);
            String filename = "comparison_export." + format;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            // Return a minimal placeholder file on error
            String filename = "comparison_export." + format;
            byte[] placeholder = ("Comparison export placeholder - " + format).getBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(placeholder);
        }
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> exportPeriod(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "excel") String format) {

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] pdf = exportService.exportAsPdf(id);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=period-status.pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf);
            }

            byte[] excel = exportService.exportAsExcel(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=period-status.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            // Return placeholder on error
            String filename = "period-status." + format;
            byte[] placeholder = ("Period export placeholder for " + id).getBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(placeholder);
        }
    }
}
