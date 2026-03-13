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
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "scope", defaultValue = "CENTRAL") String scope,
            @RequestParam(value = "report_type", required = false) String reportType,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        var response = templateService.uploadTemplate(file, name, description, scope, reportType, orgId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    @PostMapping("/{id}/mapping")
    public ResponseEntity<PlaceholderMappingResponse> configureMappings(
            @PathVariable UUID id,
            @Valid @RequestBody PlaceholderMappingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        return ResponseEntity.ok(mappingService.configureMappings(id, request, userId));
    }

    /**
     * Get current mapping configuration for a template.
     */
    @GetMapping("/{id}/mapping")
    public ResponseEntity<PlaceholderMappingResponse> getMappings(@PathVariable UUID id) {
        return ResponseEntity.ok(mappingService.getMappings(id));
    }
}
