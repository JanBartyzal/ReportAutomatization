package com.reportplatform.form.controller;

import com.reportplatform.form.dto.FormCreateRequest;
import com.reportplatform.form.dto.FormDto;
import com.reportplatform.form.dto.FormUpdateRequest;
import com.reportplatform.form.dto.FormVersionDto;
import com.reportplatform.form.service.FormService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;
    private final com.reportplatform.form.service.ExcelTemplateService excelTemplateService;
    private final com.reportplatform.form.service.ExcelImportService excelImportService;

    public FormController(FormService formService,
                           com.reportplatform.form.service.ExcelTemplateService excelTemplateService,
                           com.reportplatform.form.service.ExcelImportService excelImportService) {
        this.formService = formService;
        this.excelTemplateService = excelTemplateService;
        this.excelImportService = excelImportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<FormDto> listForms(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String scope,
            Pageable pageable) {
        return formService.listForms(orgId, scope, pageable);
    }

    @GetMapping("/released")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<FormDto> listReleasedForms(Pageable pageable) {
        return formService.listReleasedForms(pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<?> createForm(
            @Valid @RequestBody FormCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId) {
        // Resolve orgId: body takes precedence, then X-Org-Id header; reject if neither provided
        String effectiveOrgId = (request.orgId() != null && !request.orgId().isBlank())
                ? request.orgId()
                : headerOrgId;
        if (effectiveOrgId == null || effectiveOrgId.isBlank()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "orgId is required in request body or X-Org-Id header"));
        }
        FormCreateRequest effectiveRequest = (request.orgId() != null && !request.orgId().isBlank())
                ? request
                : new FormCreateRequest(effectiveOrgId, request.title(), request.description(),
                        request.scope(), request.ownerOrgId(), request.fields());
        var form = formService.createForm(effectiveRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(form);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public FormDto getForm(@PathVariable UUID id) {
        return formService.getForm(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto updateForm(
            @PathVariable UUID id,
            @Valid @RequestBody FormUpdateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return formService.updateForm(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<Void> deleteForm(@PathVariable UUID id) {
        formService.deleteForm(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto publishForm(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return formService.publishForm(id, userId);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto closeForm(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return formService.closeForm(id, userId);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto releaseForm(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return formService.releaseForm(id, userId);
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto shareForm(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        String scope = request.getOrDefault("scope", "SHARED_WITHIN_HOLDING");
        return formService.updateFormScope(id, scope, userId);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<FormVersionDto> getVersions(@PathVariable UUID id) {
        return formService.getVersions(id);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public FormDto getPreview(@PathVariable UUID id) {
        return formService.getPreview(id);
    }

    /**
     * Export form as Excel template.
     */
    @GetMapping("/{id}/export/excel-template")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<byte[]> exportExcelTemplate(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Org-Id", required = false) String orgId) {
        try {
            byte[] excelBytes = excelTemplateService.generateTemplate(id, orgId);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=form-template-" + id + ".xlsx")
                    .contentType(org.springframework.http.MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            // Fallback: generate simple single-sheet template
            try {
                var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var sheet = workbook.createSheet("Form Data");
                var headerRow = sheet.createRow(0);
                var form = formService.getForm(id);
                if (form.fields() != null) {
                    for (int i = 0; i < form.fields().size(); i++) {
                        var field = form.fields().get(i);
                        headerRow.createCell(i).setCellValue(
                                field.label() != null ? field.label() : field.fieldKey());
                    }
                }
                var out = new java.io.ByteArrayOutputStream();
                workbook.write(out);
                workbook.close();
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=form-template-" + id + ".xlsx")
                        .contentType(org.springframework.http.MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(out.toByteArray());
            } catch (Throwable fallbackEx) {
                byte[] placeholder = ("Form template for " + id).getBytes();
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=form-template-" + id + ".xlsx")
                        .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                        .body(placeholder);
            }
        }
    }

    /**
     * Import form data from Excel.
     */
    @PostMapping("/{id}/import/excel")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> importExcel(
            @PathVariable UUID id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            var result = excelImportService.importExcel(id, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "form_id", id,
                    "status", "FAILED",
                    "error", "Excel import failed",
                    "detail", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
