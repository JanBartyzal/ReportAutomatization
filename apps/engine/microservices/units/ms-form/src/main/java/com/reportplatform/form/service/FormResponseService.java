package com.reportplatform.form.service;

import com.reportplatform.form.config.FormState;
import com.reportplatform.form.dto.AutoSaveRequest;
import com.reportplatform.form.dto.FieldCommentDto;
import com.reportplatform.form.dto.FormResponseCreateRequest;
import com.reportplatform.form.dto.FormResponseDto;
import com.reportplatform.form.dto.FormResponseUpdateRequest;
import com.reportplatform.form.dto.ValidationResult;
import com.reportplatform.form.exception.FormClosedException;
import com.reportplatform.form.exception.FormNotFoundException;
import com.reportplatform.form.exception.FormNotPublishedException;
import com.reportplatform.form.exception.InvalidFormStateException;
import com.reportplatform.form.model.FormFieldCommentEntity;
import com.reportplatform.form.model.FormFieldValueEntity;
import com.reportplatform.form.model.FormResponseEntity;
import com.reportplatform.form.repository.FormFieldCommentRepository;
import com.reportplatform.form.repository.FormFieldValueRepository;
import com.reportplatform.form.repository.FormRepository;
import com.reportplatform.form.repository.FormResponseRepository;
import com.reportplatform.form.repository.FormVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FormResponseService {

    private static final Logger log = LoggerFactory.getLogger(FormResponseService.class);

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormResponseRepository formResponseRepository;
    private final FormFieldValueRepository formFieldValueRepository;
    private final FormFieldCommentRepository formFieldCommentRepository;
    private final ValidationService validationService;
    private final DaprEventPublisher daprEventPublisher;

    public FormResponseService(FormRepository formRepository,
                               FormVersionRepository formVersionRepository,
                               FormResponseRepository formResponseRepository,
                               FormFieldValueRepository formFieldValueRepository,
                               FormFieldCommentRepository formFieldCommentRepository,
                               ValidationService validationService,
                               DaprEventPublisher daprEventPublisher) {
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
        this.formResponseRepository = formResponseRepository;
        this.formFieldValueRepository = formFieldValueRepository;
        this.formFieldCommentRepository = formFieldCommentRepository;
        this.validationService = validationService;
        this.daprEventPublisher = daprEventPublisher;
    }

    public Page<FormResponseDto> listResponses(UUID formId, Pageable pageable) {
        return formResponseRepository.findByFormId(formId, pageable)
                .map(FormResponseDto::from);
    }

    @Transactional
    public FormResponseDto createResponse(UUID formId, FormResponseCreateRequest request, String userId) {
        var form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        if (form.getStatus() != FormState.PUBLISHED) {
            throw new FormNotPublishedException(formId);
        }

        var latestVersion = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId)
                .orElseThrow(() -> new InvalidFormStateException("No version found for form " + formId));

        var response = new FormResponseEntity(
                formId,
                latestVersion.getId(),
                request.orgId(),
                request.periodId(),
                userId,
                request.data() != null ? request.data() : Collections.emptyMap()
        );
        response = formResponseRepository.save(response);

        // Save individual field values
        if (request.data() != null) {
            saveFieldValues(response.getId(), request.data());
        }

        log.info("Created response {} for form {} version {}", response.getId(), formId, latestVersion.getVersionNumber());
        return FormResponseDto.from(response);
    }

    public FormResponseDto getResponse(UUID formId, UUID responseId) {
        var response = findResponseOrThrow(responseId);
        var comments = formFieldCommentRepository.findByResponseId(responseId).stream()
                .map(FieldCommentDto::from)
                .toList();
        return FormResponseDto.from(response, comments);
    }

    @Transactional
    public FormResponseDto updateResponse(UUID formId, UUID responseId,
                                          FormResponseUpdateRequest request, String userId) {
        var response = findResponseOrThrow(responseId);

        if ("SUBMITTED".equals(response.getStatus())) {
            throw new InvalidFormStateException("Cannot update a submitted response");
        }

        var form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        if (form.getStatus() == FormState.CLOSED) {
            throw new FormClosedException(formId);
        }

        // Merge data
        if (request.data() != null) {
            var merged = new HashMap<>(response.getData());
            merged.putAll(request.data());
            response.setData(merged);
            saveFieldValues(response.getId(), request.data());
        }

        // Submit if requested
        if (request.submit()) {
            var validation = validationService.validate(response.getFormVersionId(), response.getData(), true);
            if (!validation.valid()) {
                throw new InvalidFormStateException("Validation failed: " + validation.errors());
            }
            response.setStatus("SUBMITTED");
            response.setSubmittedAt(Instant.now());
            log.info("Submitted response {} for form {}", responseId, formId);

            // Publish event for MS-LIFECYCLE
            daprEventPublisher.publishFormResponseSubmitted(response);
        }

        formResponseRepository.save(response);
        return FormResponseDto.from(response);
    }

    @Transactional
    public FormResponseDto autoSave(UUID formId, UUID responseId, AutoSaveRequest request, String userId) {
        var response = findResponseOrThrow(responseId);

        if ("SUBMITTED".equals(response.getStatus())) {
            throw new InvalidFormStateException("Cannot auto-save a submitted response");
        }

        // Merge incoming data with existing
        var merged = new HashMap<>(response.getData());
        if (request.data() != null) {
            merged.putAll(request.data());
        }
        response.setData(merged);
        formResponseRepository.save(response);

        // Save individual field values
        if (request.data() != null) {
            saveFieldValues(response.getId(), request.data());
        }

        // Validate with warnings (non-blocking)
        var validation = validationService.validate(response.getFormVersionId(), merged, false);
        log.debug("Auto-saved response {} with {} warnings", responseId, validation.warnings().size());

        return FormResponseDto.from(response);
    }

    @Transactional
    public FieldCommentDto addComment(UUID responseId, String fieldKey, String comment, String userId) {
        findResponseOrThrow(responseId);
        var entity = new FormFieldCommentEntity(responseId, fieldKey, comment, userId);
        entity = formFieldCommentRepository.save(entity);
        return FieldCommentDto.from(entity);
    }

    private FormResponseEntity findResponseOrThrow(UUID responseId) {
        return formResponseRepository.findById(responseId)
                .orElseThrow(() -> new FormNotFoundException(responseId));
    }

    private void saveFieldValues(UUID responseId, Map<String, Object> data) {
        for (var entry : data.entrySet()) {
            var existing = formFieldValueRepository.findByResponseIdAndFieldKey(responseId, entry.getKey());
            if (existing.isPresent()) {
                var fv = existing.get();
                fv.setValue(entry.getValue() != null ? String.valueOf(entry.getValue()) : null);
                formFieldValueRepository.save(fv);
            } else {
                var fv = new FormFieldValueEntity(
                        responseId,
                        entry.getKey(),
                        entry.getValue() != null ? String.valueOf(entry.getValue()) : null
                );
                formFieldValueRepository.save(fv);
            }
        }
    }
}
