package com.reportplatform.form.controller;

import com.reportplatform.form.dto.AutoSaveRequest;
import com.reportplatform.form.dto.FieldCommentDto;
import com.reportplatform.form.dto.FormResponseCreateRequest;
import com.reportplatform.form.dto.FormResponseDto;
import com.reportplatform.form.dto.FormResponseUpdateRequest;
import com.reportplatform.form.service.FormResponseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/forms/{formId}/responses")
public class FormResponseController {

    private final FormResponseService formResponseService;

    public FormResponseController(FormResponseService formResponseService) {
        this.formResponseService = formResponseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public Page<FormResponseDto> listResponses(
            @PathVariable UUID formId,
            Pageable pageable) {
        return formResponseService.listResponses(formId, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<FormResponseDto> createResponse(
            @PathVariable UUID formId,
            @Valid @RequestBody FormResponseCreateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var response = formResponseService.createResponse(formId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{respId}")
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public FormResponseDto getResponse(
            @PathVariable UUID formId,
            @PathVariable UUID respId) {
        return formResponseService.getResponse(formId, respId);
    }

    @PutMapping("/{respId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public FormResponseDto updateResponse(
            @PathVariable UUID formId,
            @PathVariable UUID respId,
            @Valid @RequestBody FormResponseUpdateRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return formResponseService.updateResponse(formId, respId, request, userId);
    }

    @PutMapping("/{respId}/auto-save")
    public FormResponseDto autoSave(
            @PathVariable UUID formId,
            @PathVariable UUID respId,
            @RequestBody AutoSaveRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return formResponseService.autoSave(formId, respId, request, userId);
    }

    @PostMapping("/{respId}/comments")
    public ResponseEntity<FieldCommentDto> addComment(
            @PathVariable UUID formId,
            @PathVariable UUID respId,
            @RequestBody FieldCommentDto comment,
            @RequestHeader("X-User-Id") String userId) {
        var saved = formResponseService.addComment(respId, comment.fieldKey(), comment.comment(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
