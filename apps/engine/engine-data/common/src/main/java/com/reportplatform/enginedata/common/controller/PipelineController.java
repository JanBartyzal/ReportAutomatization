package com.reportplatform.enginedata.common.controller;

import com.reportplatform.enginedata.common.service.PipelineStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Pipeline endpoints called by the orchestrator during file processing.
 * <p>
 * These REST endpoints provide a direct HTTP alternative to Dapr service
 * invocation for the MAP and STORE workflow steps.
 * </p>
 * <p>
 * The orchestrator sends these payloads:
 * <ul>
 *   <li>{@code /store} and {@code /store-doc}: {@code {"fileId":"...","mappedData":"...","orgId":"..."}}</li>
 *   <li>{@code /rollback} and {@code /rollback-doc}: {@code {"fileId":"...","orgId":"..."}}</li>
 * </ul>
 * The {@code mappedData} field is the stringified JSON from the MAP step,
 * which in turn wraps the actual parsed data from the atomizer.
 * </p>
 */
@RestController
@RequestMapping("/api/v1")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    private final PipelineStoreService storeService;

    public PipelineController(PipelineStoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping("/map")
    public ResponseEntity<Map<String, Object>> mapData(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("MAP step for file [{}] -- pass-through (template mapping not yet implemented)", fileId);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "status", "MAPPED",
                "mappedData", request.getOrDefault("parsedData", "{}")
        ));
    }

    @PostMapping("/store")
    public ResponseEntity<Map<String, Object>> storeData(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        String orgId = String.valueOf(request.getOrDefault("orgId", "unknown"));
        String mappedData = String.valueOf(request.getOrDefault("mappedData", "{}"));
        log.info("STORE (table) step for file [{}] org [{}]", fileId, orgId);

        try {
            int count = storeService.storeTableData(fileId, orgId, mappedData);
            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "STORED",
                    "tablesStored", count
            ));
        } catch (Exception e) {
            log.error("STORE (table) failed for file [{}]: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "fileId", fileId,
                    "status", "FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @PostMapping("/store-doc")
    public ResponseEntity<Map<String, Object>> storeDoc(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        String orgId = String.valueOf(request.getOrDefault("orgId", "unknown"));
        String mappedData = String.valueOf(request.getOrDefault("mappedData", "{}"));
        log.info("STORE (document) step for file [{}] org [{}]", fileId, orgId);

        try {
            int count = storeService.storeDocumentData(fileId, orgId, mappedData);
            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "STORED",
                    "documentsStored", count
            ));
        } catch (Exception e) {
            log.error("STORE (document) failed for file [{}]: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "fileId", fileId,
                    "status", "FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> rollback(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("ROLLBACK (table) for file [{}]", fileId);

        try {
            int deleted = storeService.rollbackTableData(fileId);
            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "ROLLED_BACK",
                    "tablesDeleted", deleted
            ));
        } catch (Exception e) {
            log.error("ROLLBACK (table) failed for file [{}]: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "fileId", fileId,
                    "status", "ROLLBACK_FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @PostMapping("/rollback-doc")
    public ResponseEntity<Map<String, Object>> rollbackDoc(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("ROLLBACK (document) for file [{}]", fileId);

        try {
            int deleted = storeService.rollbackDocumentData(fileId);
            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "ROLLED_BACK",
                    "documentsDeleted", deleted
            ));
        } catch (Exception e) {
            log.error("ROLLBACK (document) failed for file [{}]: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "fileId", fileId,
                    "status", "ROLLBACK_FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }
}
