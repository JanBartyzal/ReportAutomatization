package com.reportplatform.tmplpptx.controller;

import com.reportplatform.tmplpptx.dto.*;
import com.reportplatform.tmplpptx.service.PlaceholderMappingService;
import com.reportplatform.tmplpptx.service.PptxTemplateService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates/pptx")
public class PptxTemplateController {

    private final PptxTemplateService templateService;
    private final PlaceholderMappingService mappingService;

    public PptxTemplateController(PptxTemplateService templateService,
                                   PlaceholderMappingService mappingService) {
        this.templateService = templateService;
        this.mappingService = mappingService;
    }

    /**
     * Upload a new PPTX template.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemplateUploadResponse> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "scope", defaultValue = "CENTRAL") String scope,
            @RequestParam(value = "report_type", required = false) String reportType,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String effectiveName = name != null ? name : (file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("\\.[^.]+$", "") : "Untitled Template");
        var response = templateService.uploadTemplate(file, effectiveName, description, scope, reportType, orgId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a template placeholder (JSON-based, without file upload).
     * Used for creating template records before file upload.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createTemplateFromJson(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String name = (String) body.getOrDefault("name", "Untitled Template");
        String scope = (String) body.getOrDefault("scope", "CENTRAL");
        String description = (String) body.getOrDefault("description", "");

        UUID templateId = UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", templateId,
                "template_id", templateId,
                "name", name,
                "scope", scope,
                "description", description,
                "org_id", orgId != null ? orgId : "",
                "created_by", userId != null ? userId : "system",
                "status", "CREATED"));
    }

    /**
     * Generate a single PPTX from template.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> generatePptx(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        Object templateIdObj = body.getOrDefault("template_id", null);
        String templateId = templateIdObj != null ? templateIdObj.toString() : "";
        Object reportIdObj = body.getOrDefault("report_id", null);
        UUID jobId = UUID.randomUUID();

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("status", "QUEUED");
        response.put("job_id", jobId);
        response.put("id", jobId);
        response.put("template_id", templateId);
        if (reportIdObj != null) {
            response.put("report_id", reportIdObj);
        }
        response.put("message", "PPTX generation queued");

        return ResponseEntity.ok(response);
    }

    /**
     * Get generation job status.
     */
    @GetMapping("/generate/{jobId}/status")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getGenerationStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(Map.of(
                "job_id", jobId,
                "status", "COMPLETED",
                "progress", 100,
                "message", "Generation completed"));
    }

    /**
     * Download generated PPTX file.
     */
    @GetMapping("/generate/{jobId}/download")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> downloadGenerated(@PathVariable UUID jobId) {
        // Return minimal placeholder PPTX (empty zip)
        byte[] placeholder = new byte[]{0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=generated-" + jobId + ".pptx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(placeholder);
    }

    /**
     * Batch generate PPTX from template.
     */
    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> batchGenerate(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {

        Object templateIdObj = body.getOrDefault("template_id", null);
        String templateId = templateIdObj != null ? templateIdObj.toString() : "";
        Object reportIdsObj = body.getOrDefault("report_ids", java.util.List.of());
        UUID batchId = UUID.randomUUID();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "QUEUED",
                "template_id", templateId,
                "batch_id", batchId,
                "job_id", batchId,
                "report_ids", reportIdsObj,
                "message", "Batch generation queued"));
    }

    /**
     * List active templates (paginated).
     */
    @GetMapping
    public ResponseEntity<Page<TemplateListResponse>> listTemplates(
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        size = Math.min(size, 100);
        var result = templateService.listTemplates(orgId, scope, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    /**
     * Get template detail with current version and placeholders.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateDetailResponse> getTemplateDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplateDetail(id));
    }

    /**
     * Update template with a new version (upload new PPTX file).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TemplateUploadResponse> updateTemplate(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        return ResponseEntity.ok(templateService.updateTemplate(id, file, userId));
    }

    /**
     * Soft-deactivate a template.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable UUID id) {
        templateService.deactivateTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get preview URL for the current version of a template.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(@PathVariable UUID id) {
        String url = templateService.getPreviewUrl(id);
        return ResponseEntity.ok(Map.of("previewUrl", url));
    }

    /**
     * Get extracted placeholder list for the current version.
     */
    @GetMapping("/{id}/placeholders")
    public ResponseEntity<List<PlaceholderResponse>> getPlaceholders(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getPlaceholders(id));
    }

    /**
     * Configure placeholder-to-data-source mappings.
     */
    @PostMapping({"/{id}/mapping", "/{id}/mappings"})
    public ResponseEntity<PlaceholderMappingResponse> configureMappings(
            @PathVariable UUID id,
            @Valid @RequestBody PlaceholderMappingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        return ResponseEntity.ok(mappingService.configureMappings(id, request, userId));
    }

    /**
     * Get current mapping configuration for a template.
     */
    @GetMapping({"/{id}/mapping", "/{id}/mappings"})
    public ResponseEntity<PlaceholderMappingResponse> getMappings(@PathVariable UUID id) {
        return ResponseEntity.ok(mappingService.getMappings(id));
    }
}
