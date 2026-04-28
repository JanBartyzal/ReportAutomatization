package com.reportplatform.tmpl.controller;

import com.reportplatform.tmpl.dto.*;
import com.reportplatform.tmpl.entity.TextTemplateVersionEntity;
import com.reportplatform.tmpl.service.TemplateRenderService;
import com.reportplatform.tmpl.service.TextTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the Text Template Engine.
 * <p>
 * Org identity is propagated via {@code X-Org-Id} and {@code X-User-Id} headers
 * injected by the Nginx API Gateway after JWT validation.
 */
@RestController
@RequestMapping("/api/v1/reporting/text-templates")
public class TextTemplateController {

    private final TextTemplateService templateService;
    private final TemplateRenderService renderService;

    public TextTemplateController(TextTemplateService templateService,
                                   TemplateRenderService renderService) {
        this.templateService = templateService;
        this.renderService = renderService;
    }

    /** List all accessible text templates (org + system). */
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<TextTemplateDto> list(@RequestHeader("X-Org-Id") String orgId) {
        return templateService.listAccessible(UUID.fromString(orgId));
    }

    /** Get single template by ID (including full content and bindings). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<TextTemplateDto> getById(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        return templateService.findById(UUID.fromString(orgId), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** List version history for a template. */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<TextTemplateVersionEntity> getVersions(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        return templateService.getVersions(UUID.fromString(orgId), id);
    }

    /** Create a new text template. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public TextTemplateDto create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateTextTemplateRequest req) {
        return templateService.create(UUID.fromString(orgId), userId, req);
    }

    /** Partially update a text template (creates a new version snapshot). */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public TextTemplateDto update(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTextTemplateRequest req) {
        return templateService.update(UUID.fromString(orgId), id, userId, req);
    }

    /** Soft-delete a text template. System templates cannot be deleted. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public void delete(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id) {
        templateService.delete(UUID.fromString(orgId), id);
    }

    /**
     * Render a text template with the provided runtime parameters.
     * Resolves all data bindings via Named Queries and calls the appropriate
     * generator (PPTX / Excel). Returns a download URL.
     */
    @PostMapping("/{id}/render")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public RenderResponse render(
            @RequestHeader("X-Org-Id") String orgId,
            @PathVariable UUID id,
            @Valid @RequestBody RenderRequest req) {
        return renderService.render(UUID.fromString(orgId), id, req);
    }
}
