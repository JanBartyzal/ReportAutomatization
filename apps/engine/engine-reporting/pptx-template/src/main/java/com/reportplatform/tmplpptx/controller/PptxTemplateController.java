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
     * JSON-only template creation is not supported — a PPTX file is required to extract placeholders.
     * Use the multipart/form-data POST endpoint with an attached .pptx file instead.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createTemplateFromJson(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                "error", "Template creation requires a PPTX file upload",
                "hint", "Use Content-Type: multipart/form-data and include 'file' field with the .pptx file"));
    }

    /**
     * Trigger async PPTX generation for a report via processor-generators (Dapr gRPC).
     * Generation is handled asynchronously — poll GET /generate/{jobId}/status for progress.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> generatePptx(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        // PPTX generation is delegated to processor-generators via Dapr gRPC.
        // This requires the processor-generators service to be running and registered in Dapr.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "error", "PPTX generation requires the processor-generators service running via Dapr",
                "detail", "Invoke processor-generators Dapr app directly or wait for orchestrator integration"));
    }

    /**
     * Get async generation job status.
     * Jobs are tracked by the processor-generators service.
     */
    @GetMapping("/generate/{jobId}/status")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> getGenerationStatus(@PathVariable UUID jobId) {
        // No local job store exists — generation jobs are managed by processor-generators.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Generation job not found",
                "job_id", jobId.toString(),
                "detail", "Job tracking requires the processor-generators Dapr service"));
    }

    /**
     * Download a generated PPTX file produced by processor-generators.
     */
    @GetMapping("/generate/{jobId}/download")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> downloadGenerated(@PathVariable UUID jobId) {
        // No generated file exists — downloads are served from blob storage after processor-generators completes.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Generated file not found",
                "job_id", jobId.toString(),
                "detail", "File becomes available after successful generation by processor-generators"));
    }

    /**
     * Trigger batch PPTX generation for multiple reports via processor-generators (Dapr gRPC).
     */
    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> batchGenerate(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "error", "Batch PPTX generation requires the processor-generators service running via Dapr",
                "detail", "Submit individual generation requests or wait for orchestrator batch integration"));
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
