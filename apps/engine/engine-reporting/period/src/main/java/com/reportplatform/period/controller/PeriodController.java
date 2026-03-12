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

import java.io.IOException;
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
    public Page<PeriodResponse> listPeriods(
            @RequestParam(required = false) String holdingId,
            Pageable pageable) {
        return periodService.listPeriods(holdingId, pageable)
                .map(PeriodResponse::from);
    }

    @PostMapping
    public ResponseEntity<PeriodResponse> createPeriod(
            @Valid @RequestBody PeriodCreateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var period = periodService.createPeriod(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodResponse.from(period));
    }

    @GetMapping("/{id}")
    public PeriodResponse getPeriod(@PathVariable UUID id) {
        return PeriodResponse.from(periodService.getPeriod(id));
    }

    @PutMapping("/{id}")
    public PeriodResponse updatePeriod(
            @PathVariable UUID id,
            @RequestBody PeriodUpdateRequest request) {
        return PeriodResponse.from(periodService.updatePeriod(id, request));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<PeriodResponse> clonePeriod(
            @PathVariable UUID id,
            @Valid @RequestBody CloneRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var cloned = cloneService.clonePeriod(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodResponse.from(cloned));
    }

    @GetMapping("/{id}/status")
    public PeriodStatusResponse getStatus(@PathVariable UUID id) {
        return completionTrackingService.getCompletionStatus(id);
    }

    @PostMapping("/compare")
    public ResponseEntity<CrossTypeComparisonResponse> comparePeriods(
            @Valid @RequestBody CrossTypeComparisonRequest request) {
        var response = crossTypeComparisonService.buildComparisonContext(request.periodIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportPeriod(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "excel") String format) throws IOException {

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
    }
}
