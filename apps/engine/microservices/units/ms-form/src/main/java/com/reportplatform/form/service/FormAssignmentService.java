package com.reportplatform.form.service;

import com.reportplatform.form.dto.FormAssignmentDto;
import com.reportplatform.form.dto.FormAssignmentRequest;
import com.reportplatform.form.exception.FormNotFoundException;
import com.reportplatform.form.model.FormAssignmentEntity;
import com.reportplatform.form.repository.FormAssignmentRepository;
import com.reportplatform.form.repository.FormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FormAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(FormAssignmentService.class);

    private final FormRepository formRepository;
    private final FormAssignmentRepository formAssignmentRepository;

    public FormAssignmentService(FormRepository formRepository,
                                 FormAssignmentRepository formAssignmentRepository) {
        this.formRepository = formRepository;
        this.formAssignmentRepository = formAssignmentRepository;
    }

    public List<FormAssignmentDto> getAssignments(UUID formId) {
        formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
        return formAssignmentRepository.findByFormId(formId).stream()
                .map(FormAssignmentDto::from)
                .toList();
    }

    @Transactional
    public List<FormAssignmentDto> assignForm(UUID formId, FormAssignmentRequest request, String userId) {
        formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        var results = new ArrayList<FormAssignmentDto>();
        for (String orgId : request.orgIds()) {
            var existing = formAssignmentRepository.findByFormIdAndOrgId(formId, orgId);
            if (existing.isPresent()) {
                results.add(FormAssignmentDto.from(existing.get()));
                continue;
            }

            var assignment = new FormAssignmentEntity(formId, orgId, userId);
            assignment = formAssignmentRepository.save(assignment);
            results.add(FormAssignmentDto.from(assignment));
            log.info("Assigned form {} to org {}", formId, orgId);
        }

        return results;
    }
}
