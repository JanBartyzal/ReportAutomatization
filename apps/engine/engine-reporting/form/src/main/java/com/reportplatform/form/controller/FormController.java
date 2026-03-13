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

    public FormController(FormService formService) {
        this.formService = formService;
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
    public ResponseEntity<FormDto> createForm(
            @Valid @RequestBody FormCreateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var form = formService.createForm(request, userId);
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
            @RequestHeader("X-User-Id") String userId) {
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
            @RequestHeader("X-User-Id") String userId) {
        return formService.publishForm(id, userId);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto closeForm(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return formService.closeForm(id, userId);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public FormDto releaseForm(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return formService.releaseForm(id, userId);
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
}
