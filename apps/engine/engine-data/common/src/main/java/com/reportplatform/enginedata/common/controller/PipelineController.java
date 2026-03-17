package com.reportplatform.enginedata.common.controller;

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
 */
@RestController
@RequestMapping("/api/v1")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    @PostMapping("/map")
    public ResponseEntity<Map<String, Object>> mapData(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("MAP step for file [{}] — pass-through (template mapping not yet implemented)", fileId);
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
        log.info("STORE (table) step for file [{}] org [{}]", fileId, orgId);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "status", "STORED"
        ));
    }

    @PostMapping("/store-doc")
    public ResponseEntity<Map<String, Object>> storeDoc(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("STORE (document) step for file [{}]", fileId);
        return ResponseEntity.ok(Map.of(
                "fileId", fileId,
                "status", "STORED"
        ));
    }

    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> rollback(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("ROLLBACK (table) for file [{}]", fileId);
        return ResponseEntity.ok(Map.of("fileId", fileId, "status", "ROLLED_BACK"));
    }

    @PostMapping("/rollback-doc")
    public ResponseEntity<Map<String, Object>> rollbackDoc(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        log.info("ROLLBACK (document) for file [{}]", fileId);
        return ResponseEntity.ok(Map.of("fileId", fileId, "status", "ROLLED_BACK"));
    }
}
