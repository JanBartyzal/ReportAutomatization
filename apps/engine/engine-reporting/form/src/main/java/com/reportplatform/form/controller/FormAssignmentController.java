package com.reportplatform.form.controller;

import com.reportplatform.form.dto.FormAssignmentDto;
import com.reportplatform.form.dto.FormAssignmentRequest;
import com.reportplatform.form.service.FormAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forms/{formId}/assignments")
public class FormAssignmentController {

    private final FormAssignmentService formAssignmentService;

    public FormAssignmentController(FormAssignmentService formAssignmentService) {
        this.formAssignmentService = formAssignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','COMPANY_ADMIN','HOLDING_ADMIN')")
    public List<FormAssignmentDto> getAssignments(@PathVariable UUID formId) {
        return formAssignmentService.getAssignments(formId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")
    public ResponseEntity<List<FormAssignmentDto>> assignForm(
            @PathVariable UUID formId,
            @Valid @RequestBody FormAssignmentRequest request,
            @RequestHeader("X-User-Id") String userId) {
        var assignments = formAssignmentService.assignForm(formId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignments);
    }
}
