package com.reportplatform.template.tmpl.controller;

import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import com.reportplatform.template.tmpl.repository.MappingTemplateRepository;
import com.reportplatform.template.tmpl.service.TemplateMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST facade for schema mapping operations.
 * Exposes internal (Dapr gRPC) template mapping functionality via REST
 * for testing and external access.
 */
@RestController
@RequestMapping("/api/query/templates")
public class SchemaMappingController {

    private static final Logger log = LoggerFactory.getLogger(SchemaMappingController.class);

    private final MappingTemplateRepository templateRepository;
    private final TemplateMappingService templateMappingService;

    public SchemaMappingController(MappingTemplateRepository templateRepository,
                                    TemplateMappingService templateMappingService) {
        this.templateRepository = templateRepository;
        this.templateMappingService = templateMappingService;
    }

    @GetMapping("/mappings")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<MappingTemplateEntity>> listMappings(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        List<MappingTemplateEntity> templates;
        if (orgId != null && !orgId.isBlank()) {
            templates = templateRepository.findByOrgIdOrOrgIdIsNull(orgId);
        } else {
            templates = templateRepository.findAll();
        }
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/mappings")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<MappingTemplateEntity> createMapping(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> body) {

        MappingTemplateEntity template = new MappingTemplateEntity();
        template.setName((String) body.getOrDefault("name", "Unnamed"));
        template.setOrgId(orgId);
        template.setActive(true);

        MappingTemplateEntity saved = templateRepository.save(template);
        log.info("Created mapping template: {} (org={})", saved.getId(), orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/mappings/suggest")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> suggestMapping(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<String> sourceHeaders = (List<String>) body.getOrDefault("source_headers", List.of());
        String fileId = (String) body.getOrDefault("file_id", null);

        // Try to find the best matching template and apply mapping
        List<MappingTemplateEntity> templates = orgId != null
                ? templateRepository.findByIsActiveTrueAndOrgIdOrOrgIdIsNull(orgId)
                : templateRepository.findAll();

        if (templates.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "suggested_mappings", List.of(),
                    "confidence", 0.0,
                    "message", "No templates available for auto-suggestion"));
        }

        // Use first matching template
        MappingTemplateEntity template = templates.get(0);
        var result = templateMappingService.applyMapping(
                template.getId(), orgId, sourceHeaders, fileId);

        double confidence = result.actions().stream()
                .mapToDouble(a -> a.confidence())
                .average().orElse(0.0);

        return ResponseEntity.ok(Map.of(
                "template_id", template.getId(),
                "suggested_mappings", result.mappedHeaders(),
                "confidence", confidence,
                "actions_applied", result.actions().size()));
    }

    @PostMapping("/mappings/excel-to-form")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> excelToFormMapping(
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) body.getOrDefault("headers", List.of());

        // Generate form field suggestions from Excel headers
        List<Map<String, Object>> fields = headers.stream()
                .map(h -> Map.<String, Object>of(
                        "name", h.toLowerCase().replaceAll("[^a-z0-9_]", "_"),
                        "label", h,
                        "type", inferFieldType(h),
                        "required", false))
                .toList();

        return ResponseEntity.ok(Map.of("fields", fields));
    }

    @GetMapping("/slide-metadata")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSlideMetadata(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        // Slide metadata is persisted as part of MappingTemplateEntity rules.
        // Return empty list — no standalone slide-metadata store exists yet.
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/slide-metadata")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> createSlideMetadata(
            @RequestBody Map<String, Object> body) {
        // Slide metadata persistence requires a dedicated table (not yet implemented).
        // Returning 501 is preferable to creating a fake resource with a random ID that won't be found on subsequent requests.
        log.warn("createSlideMetadata called but slide-metadata persistence is not yet implemented");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "Slide metadata persistence is not yet implemented"));
    }

    @PostMapping("/slide-metadata/validate")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> validateSlideMetadata(
            @RequestBody Map<String, Object> body) {

        List<String> errors = new java.util.ArrayList<>();
        if (!body.containsKey("slide_index") && !body.containsKey("slideIndex")) {
            errors.add("slide_index is required");
        }
        if (!body.containsKey("template_id") && !body.containsKey("templateId")) {
            errors.add("template_id is required");
        }
        return ResponseEntity.ok(Map.of(
                "valid", errors.isEmpty(),
                "errors", errors));
    }

    @GetMapping("/slide-metadata/match")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> matchSlideMetadata(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(required = false) String fileId) {

        // Matching a file to slide metadata requires fetching the file's extracted headers
        // from the query service and then running them through the suggestion engine.
        // That cross-service call is not available in this module.
        // Clients should use POST /api/query/templates/mappings/suggest with explicit source_headers instead.
        if (fileId == null || fileId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "fileId parameter is required"));
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "error", "Automatic file-to-template matching is not yet implemented",
                        "suggestion", "Use POST /api/query/templates/mappings/suggest with explicit source_headers"));
    }

    private String inferFieldType(String header) {
        String lower = header.toLowerCase();
        if (lower.contains("cost") || lower.contains("budget") || lower.contains("amount")
                || lower.contains("price") || lower.contains("total")) {
            return "number";
        }
        if (lower.contains("date") || lower.contains("deadline")) {
            return "date";
        }
        return "text";
    }
}
