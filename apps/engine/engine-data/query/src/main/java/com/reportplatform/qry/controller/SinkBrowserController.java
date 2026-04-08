package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.*;
import com.reportplatform.qry.service.SinkQueryService;
import com.reportplatform.sink.tbl.service.SinkCorrectionService;
import com.reportplatform.sink.tbl.service.SinkSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Sink Browser (FS25).
 * Provides endpoints for browsing sinks, managing corrections, and selecting sinks for reports.
 */
@RestController
@RequestMapping("/api/query/sinks")
public class SinkBrowserController {

    private final SinkQueryService sinkQueryService;
    private final SinkCorrectionService correctionService;
    private final SinkSelectionService selectionService;

    public SinkBrowserController(
            SinkQueryService sinkQueryService,
            SinkCorrectionService correctionService,
            SinkSelectionService selectionService) {
        this.sinkQueryService = sinkQueryService;
        this.correctionService = correctionService;
        this.selectionService = selectionService;
    }

    // ========== READ ENDPOINTS ==========

    /**
     * List all sinks with summary info, paginated and filterable.
     * GET /api/query/sinks?page=0&size=20&file_id=...&source_sheet=...
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<SinkListResponse> listSinks(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "file_id", required = false) String fileId,
            @RequestParam(value = "source_sheet", required = false) String sourceSheet) {

        SinkListResponse response = sinkQueryService.listSinks(orgId, page, size, fileId, sourceSheet);
        return ResponseEntity.ok(response);
    }

    /**
     * Get full detail of a sink with corrections applied.
     * GET /api/query/sinks/{parsed_table_id}
     */
    @GetMapping("/{parsed_table_id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<SinkDetailDto> getSinkDetail(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId) {

        SinkDetailDto detail = sinkQueryService.getSinkDetail(orgId, parsedTableId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * Get correction history for a sink.
     * GET /api/query/sinks/{parsed_table_id}/corrections
     */
    @GetMapping("/{parsed_table_id}/corrections")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<SinkCorrectionDto>> getCorrections(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId) {

        List<SinkCorrectionDto> corrections = sinkQueryService.getCorrections(orgId, parsedTableId);
        return ResponseEntity.ok(corrections);
    }

    /**
     * Get selected sinks for a period.
     * GET /api/query/sinks/selections?period_id=...
     */
    @GetMapping("/selections")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<SinkSelectionDto>> getSelections(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("period_id") String periodId) {

        List<SinkSelectionDto> selections = sinkQueryService.getSelections(orgId, periodId);
        return ResponseEntity.ok(selections);
    }

    /**
     * Preview sink data with corrections applied (for dashboard/report consumption).
     * GET /api/query/sinks/{parsed_table_id}/preview
     */
    @GetMapping("/{parsed_table_id}/preview")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<TableDataDto> getSinkPreview(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId) {

        TableDataDto preview = sinkQueryService.getSinkPreview(orgId, parsedTableId);
        if (preview == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(preview);
    }

    // ========== WRITE ENDPOINTS (proxy to sink-tbl services) ==========

    /**
     * Create a correction on a parsed table.
     * POST /api/query/sinks/{parsed_table_id}/corrections
     */
    @PostMapping("/{parsed_table_id}/corrections")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> createCorrection(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CorrectionRequest request) {

        var entity = correctionService.createCorrection(new SinkCorrectionService.CorrectionData(
                parsedTableId,
                orgId,
                request.rowIndex(),
                request.colIndex(),
                request.originalValue(),
                request.correctedValue(),
                request.correctionType(),
                userId,
                request.errorCategory(),
                request.metadata()));

        return ResponseEntity.ok(Map.of(
                "correction_id", entity.getId().toString(),
                "corrected_at", entity.getCorrectedAt().toString()));
    }

    /**
     * Bulk create corrections.
     * POST /api/query/sinks/{parsed_table_id}/corrections/bulk
     */
    @PostMapping("/{parsed_table_id}/corrections/bulk")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> createBulkCorrections(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody List<CorrectionRequest> requests) {

        var corrections = requests.stream()
                .map(r -> new SinkCorrectionService.CorrectionData(
                        parsedTableId, orgId,
                        r.rowIndex(), r.colIndex(),
                        r.originalValue(), r.correctedValue(),
                        r.correctionType(), userId,
                        r.errorCategory(), r.metadata()))
                .toList();

        var entities = correctionService.createBulkCorrections(corrections);

        return ResponseEntity.ok(Map.of(
                "corrections_created", entities.size(),
                "correction_ids", entities.stream().map(e -> e.getId().toString()).toList()));
    }

    /**
     * Delete a specific correction (revert to original).
     * DELETE /api/query/sinks/{parsed_table_id}/corrections/{correction_id}
     */
    @DeleteMapping("/{parsed_table_id}/corrections/{correction_id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteCorrection(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @PathVariable("correction_id") UUID correctionId,
            @RequestHeader("X-Org-Id") String orgId) {

        correctionService.deleteCorrection(correctionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upsert sink selection (select/deselect for report).
     * PUT /api/query/sinks/{parsed_table_id}/selection
     */
    @PutMapping("/{parsed_table_id}/selection")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> upsertSelection(
            @PathVariable("parsed_table_id") UUID parsedTableId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SelectionRequest request) {

        var entity = selectionService.upsertSelection(new SinkSelectionService.SelectionData(
                parsedTableId,
                orgId,
                request.periodId(),
                request.reportType(),
                request.selected(),
                request.priority(),
                userId,
                request.note()));

        return ResponseEntity.ok(Map.of(
                "selection_id", entity.getId().toString(),
                "selected", entity.isSelected(),
                "selected_at", entity.getSelectedAt().toString()));
    }

    // ========== Request DTOs ==========

    public record CorrectionRequest(
            Integer rowIndex,
            Integer colIndex,
            String originalValue,
            String correctedValue,
            String correctionType,
            String errorCategory,
            Map<String, Object> metadata) {
    }

    public record SelectionRequest(
            String periodId,
            String reportType,
            boolean selected,
            int priority,
            String note) {
    }
}
