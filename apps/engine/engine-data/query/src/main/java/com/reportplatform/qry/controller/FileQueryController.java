package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.DocumentDto;
import com.reportplatform.qry.model.dto.FileDataResponse;
import com.reportplatform.qry.model.dto.ProcessingLogDto;
import com.reportplatform.qry.model.dto.SlideDataResponse;
import com.reportplatform.qry.model.dto.TableQueryResponse;
import com.reportplatform.qry.service.ExcelExportService;
import com.reportplatform.qry.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/query")
public class FileQueryController {

    private static final Logger log = LoggerFactory.getLogger(FileQueryController.class);

    private final QueryService queryService;
    private final ExcelExportService excelExportService;

    public FileQueryController(QueryService queryService, ExcelExportService excelExportService) {
        this.queryService = queryService;
        this.excelExportService = excelExportService;
    }

    /**
     * Returns all parsed data (tables + documents) for a file.
     */
    @GetMapping("/files/{file_id}/data")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<FileDataResponse> getFileData(
            @PathVariable("file_id") UUID fileId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        FileDataResponse response = queryService.getFileData(orgId, fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns slide content with image URLs for a file.
     */
    @GetMapping("/files/{file_id}/slides")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<SlideDataResponse> getSlideData(
            @PathVariable("file_id") UUID fileId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        SlideDataResponse response = queryService.getSlideData(orgId, fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Query OPEX table data, filterable and paginated.
     * org_id is taken from the header for RLS enforcement.
     */
    @GetMapping("/tables")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<TableQueryResponse> queryTables(
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "source_sheet", required = false) String sourceSheet,
            @RequestParam(value = "file_id", required = false) String fileId,
            @RequestParam(value = "scope", required = false) String scope) {

        TableQueryResponse response = queryService.queryTables(orgId, page, size, sourceSheet, fileId, scope);
        return ResponseEntity.ok(response);
    }

    /**
     * Query documents by file_id or other filters.
     * GET /api/query/documents?file_id=...
     */
    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<DocumentDto>> queryDocuments(
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestParam(value = "file_id", required = false) String fileId,
            @RequestParam(value = "document_id", required = false) UUID documentId) {

        if (documentId != null) {
            return queryService.getDocument(orgId, documentId)
                    .map(d -> ResponseEntity.ok(List.of(d)))
                    .orElse(ResponseEntity.ok(List.of()));
        }
        // Query by file_id
        List<DocumentDto> docs = queryService.getDocumentsByFileId(orgId, fileId);
        return ResponseEntity.ok(docs);
    }

    /**
     * Returns slide image for a specific slide.
     * GET /api/query/files/{file_id}/slides/{slide_num}/image
     */
    @GetMapping("/files/{file_id}/slides/{slide_num}/image")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> getSlideImage(
            @PathVariable("file_id") UUID fileId,
            @PathVariable("slide_num") int slideNum,
            @RequestHeader(value = "X-Org-Id") String orgId) {

        // Slide image rendering requires processor-atomizers PPTX renderer (not yet integrated).
        // Returning 501 is correct — the endpoint exists but the feature is not yet available.
        log.warn("getSlideImage not implemented: fileId={}, slideNum={}, orgId={}", fileId, slideNum, orgId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(null);
    }

    /**
     * Returns a single document by ID.
     */
    @GetMapping("/documents/{document_id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<DocumentDto> getDocument(
            @PathVariable("document_id") UUID documentId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        return queryService.getDocument(orgId, documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns processing step timeline for a file.
     */
    @GetMapping("/processing-logs/{file_id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<ProcessingLogDto>> getProcessingLogs(
            @PathVariable("file_id") String fileId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        List<ProcessingLogDto> logs = queryService.getProcessingLogs(orgId, fileId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Returns data for a specific sheet of a file.
     * GET /api/query/files/{file_id}/sheets/{sheet_num}
     */
    @GetMapping("/files/{file_id}/sheets/{sheet_num}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<TableQueryResponse> getSheetData(
            @PathVariable("file_id") UUID fileId,
            @PathVariable("sheet_num") int sheetNum,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        TableQueryResponse response = queryService.queryTables(
                orgId, 0, 100, "Sheet" + sheetNum, fileId.toString(), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns workflow steps for a file.
     * GET /api/query/workflows/{file_id}/steps
     */
    @GetMapping("/workflows/{file_id}/steps")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<ProcessingLogDto>> getWorkflowSteps(
            @PathVariable("file_id") String fileId,
            @RequestHeader(value = "X-Org-Id") String orgId) {

        List<ProcessingLogDto> logs = queryService.getProcessingLogs(orgId, fileId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Aggregation query endpoint.
     * POST /api/query/aggregate
     */
    @PostMapping("/aggregate")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> aggregate(
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestBody java.util.Map<String, Object> request) {

        String groupBy = (String) request.getOrDefault("group_by", null);
        String metric = (String) request.getOrDefault("metric", null);
        String agg = (String) request.getOrDefault("aggregation", "SUM");

        TableQueryResponse tables = queryService.queryTables(orgId, 0, 1000, null, null, null);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("group_by", groupBy);
        result.put("metric", metric);
        result.put("aggregation", agg);
        result.put("total_rows", tables.totalElements());
        result.put("data", tables.tables());

        return ResponseEntity.ok(result);
    }

    /**
     * Export parsed table data as an Excel (.xlsx) file.
     * Each table record becomes a separate sheet in the workbook.
     */
    @GetMapping("/tables/export/excel")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> exportTablesToExcel(
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestParam(value = "file_id", required = false) String fileId,
            @RequestParam(value = "source_sheet", required = false) String sourceSheet) throws IOException {

        byte[] excelBytes = excelExportService.exportTablesToExcel(orgId, fileId, sourceSheet);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "table_export.xlsx");
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}
