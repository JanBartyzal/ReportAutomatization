package com.reportplatform.enginedata.common.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.enginedata.common.service.PipelineStoreService;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import com.reportplatform.template.tmpl.service.TemplateMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TemplateMappingService templateMappingService;
    private final ObjectMapper objectMapper;

    public PipelineController(PipelineStoreService storeService,
                              TemplateMappingService templateMappingService,
                              ObjectMapper objectMapper) {
        this.storeService = storeService;
        this.templateMappingService = templateMappingService;
        this.objectMapper = objectMapper;
    }

    /**
     * MAP step: applies template mapping rules to normalize atomizer output column names.
     * Accepts the raw parsedData JSON from the atomizer (sheets or slides structure),
     * extracts source headers, runs them through the TemplateMappingService suggestion
     * engine (history + AI + template rules), and renames headers in the data structure.
     * The resulting mappedData is passed to the STORE step.
     */
    @PostMapping("/map")
    public ResponseEntity<Map<String, Object>> mapData(@RequestBody Map<String, Object> request) {
        String fileId = String.valueOf(request.getOrDefault("fileId", "unknown"));
        String orgId  = String.valueOf(request.getOrDefault("orgId", ""));
        String parsedDataJson = String.valueOf(request.getOrDefault("parsedData", "{}"));
        log.info("MAP step for file [{}] org [{}]", fileId, orgId);

        try {
            Map<String, Object> parsedData = objectMapper.readValue(
                    parsedDataJson, new TypeReference<>() {});

            String mappedDataJson = applyColumnMapping(orgId, parsedData);

            return ResponseEntity.ok(Map.of(
                    "fileId", fileId,
                    "status", "MAPPED",
                    "mappedData", mappedDataJson
            ));
        } catch (Exception e) {
            log.error("MAP step failed for file [{}]: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "fileId", fileId,
                    "status", "FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    /**
     * Applies template mapping suggestions to normalize column headers in the parsed data.
     * Supports both tabular (sheets) and document (slides) structures.
     */
    @SuppressWarnings("unchecked")
    private String applyColumnMapping(String orgId, Map<String, Object> parsedData) throws Exception {
        // Collect all source headers from sheets or slides
        List<String> allHeaders = new ArrayList<>();

        List<Map<String, Object>> sheets = (List<Map<String, Object>>) parsedData.get("sheets");
        List<Map<String, Object>> slides = (List<Map<String, Object>>) parsedData.get("slides");

        if (sheets != null) {
            for (Map<String, Object> sheet : sheets) {
                Object hdrs = sheet.get("headers");
                if (hdrs instanceof List<?> list) {
                    list.forEach(h -> allHeaders.add(String.valueOf(h)));
                }
            }
        }

        if (allHeaders.isEmpty()) {
            // No mappable headers — return parsedData unchanged
            return objectMapper.writeValueAsString(parsedData);
        }

        // Get mapping suggestions for all headers
        List<MappingActionData> suggestions = templateMappingService.suggestMapping(orgId, allHeaders);

        // Build header rename map: sourceColumn → targetColumn (for mapped columns only)
        Map<String, String> renameMap = new LinkedHashMap<>();
        for (MappingActionData action : suggestions) {
            if (!"UNMAPPED".equals(action.ruleType()) && action.targetColumn() != null) {
                renameMap.put(action.sourceColumn(), action.targetColumn());
            }
        }

        if (renameMap.isEmpty()) {
            log.info("MAP step: no column renames for file — using original headers");
            return objectMapper.writeValueAsString(parsedData);
        }

        // Apply renames to sheets
        Map<String, Object> mapped = new LinkedHashMap<>(parsedData);
        if (sheets != null) {
            List<Map<String, Object>> remappedSheets = new ArrayList<>();
            for (Map<String, Object> sheet : sheets) {
                Map<String, Object> remappedSheet = new LinkedHashMap<>(sheet);
                Object hdrs = sheet.get("headers");
                if (hdrs instanceof List<?> headerList) {
                    List<String> remappedHeaders = headerList.stream()
                            .map(h -> renameMap.getOrDefault(String.valueOf(h), String.valueOf(h)))
                            .toList();
                    remappedSheet.put("headers", remappedHeaders);
                }
                remappedSheets.add(remappedSheet);
            }
            mapped.put("sheets", remappedSheets);
        }

        int renamedCount = renameMap.size();
        log.info("MAP step: renamed {} column(s) for file using template mapping", renamedCount);
        return objectMapper.writeValueAsString(mapped);
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
