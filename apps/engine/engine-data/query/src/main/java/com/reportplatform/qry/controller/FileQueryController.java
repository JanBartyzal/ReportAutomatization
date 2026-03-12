package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.DocumentDto;
import com.reportplatform.qry.model.dto.FileDataResponse;
import com.reportplatform.qry.model.dto.ProcessingLogDto;
import com.reportplatform.qry.model.dto.SlideDataResponse;
import com.reportplatform.qry.model.dto.TableQueryResponse;
import com.reportplatform.qry.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/query")
public class FileQueryController {

    private final QueryService queryService;

    public FileQueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Returns all parsed data (tables + documents) for a file.
     */
    @GetMapping("/files/{file_id}/data")
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
     * Returns a single document by ID.
     */
    @GetMapping("/documents/{document_id}")
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
    public ResponseEntity<List<ProcessingLogDto>> getProcessingLogs(
            @PathVariable("file_id") String fileId,
            @RequestHeader(value = "X-Org-Id") String orgId,
            @RequestHeader(value = "X-User-Id") String userId) {

        List<ProcessingLogDto> logs = queryService.getProcessingLogs(orgId, fileId);
        return ResponseEntity.ok(logs);
    }
}
