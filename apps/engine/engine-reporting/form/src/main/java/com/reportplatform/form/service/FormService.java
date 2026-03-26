package com.reportplatform.form.service;

import com.reportplatform.form.config.FormState;
import com.reportplatform.form.dto.FormCreateRequest;
import com.reportplatform.form.dto.FormDto;
import com.reportplatform.form.dto.FormFieldDefinition;
import com.reportplatform.form.dto.FormUpdateRequest;
import com.reportplatform.form.dto.FormVersionDto;
import com.reportplatform.form.exception.FormNotFoundException;
import com.reportplatform.form.exception.InvalidFormStateException;
import com.reportplatform.form.model.FormEntity;
import com.reportplatform.form.model.FormFieldEntity;
import com.reportplatform.form.model.FormVersionEntity;
import com.reportplatform.form.repository.FormFieldRepository;
import com.reportplatform.form.repository.FormRepository;
import com.reportplatform.form.repository.FormVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FormService {

    private static final Logger log = LoggerFactory.getLogger(FormService.class);

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormFieldRepository formFieldRepository;

    public FormService(FormRepository formRepository,
                       FormVersionRepository formVersionRepository,
                       FormFieldRepository formFieldRepository) {
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
        this.formFieldRepository = formFieldRepository;
    }

    private static final Set<String> VALID_SCOPES = Set.of("CENTRAL", "LOCAL", "SHARED_WITHIN_HOLDING");

    public Page<FormDto> listForms(String orgId, String scope, Pageable pageable) {
        Page<FormEntity> forms;
        if (scope != null && orgId != null) {
            forms = formRepository.findByScopeAndOwnerOrgId(scope, orgId, pageable);
        } else if (orgId != null) {
            forms = formRepository.findVisibleForms(orgId, pageable);
        } else if (scope != null) {
            forms = formRepository.findByScope(scope, pageable);
        } else {
            forms = formRepository.findAll(pageable);
        }
        return forms.map(FormDto::from);
    }

    public Page<FormDto> listReleasedForms(Pageable pageable) {
        return formRepository.findReleasedForms(pageable).map(FormDto::from);
    }

    @Transactional
    public FormDto createForm(FormCreateRequest request, String userId) {
        String scope = request.scope() != null ? request.scope() : "CENTRAL";
        if (!VALID_SCOPES.contains(scope)) {
            throw new InvalidFormStateException("Invalid scope: " + scope + ". Valid: " + VALID_SCOPES);
        }
        var form = new FormEntity(
                request.orgId(),
                request.title(),
                request.description(),
                scope,
                userId
        );
        form.setOwnerOrgId(request.ownerOrgId() != null ? request.ownerOrgId() : request.orgId());
        form = formRepository.save(form);

        // Create initial version (v1)
        var version = new FormVersionEntity(form.getId(), 1, Map.of(), userId);
        version = formVersionRepository.save(version);

        // Save fields if provided — normalize field definitions
        List<FormFieldDefinition> fieldDefs = request.fields() != null ? request.fields() : List.of();
        List<FormFieldDefinition> normalizedFields = normalizeFields(fieldDefs);
        saveFields(version.getId(), normalizedFields);

        log.info("Created form {} with version 1", form.getId());
        return FormDto.from(form, 1, normalizedFields);
    }

    public FormDto getForm(UUID formId) {
        var form = findFormOrThrow(formId);
        var latestVersion = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId);
        List<FormFieldDefinition> fields = latestVersion
                .map(v -> getFieldDefinitions(v.getId()))
                .orElse(List.of());
        int versionNum = latestVersion.map(FormVersionEntity::getVersionNumber).orElse(0);
        return FormDto.from(form, versionNum, fields);
    }

    @Transactional
    public FormDto updateForm(UUID formId, FormUpdateRequest request, String userId) {
        var form = findFormOrThrow(formId);

        if (form.getStatus() == FormState.CLOSED) {
            throw new InvalidFormStateException("Cannot edit a closed form");
        }

        // Update form metadata
        if (request.title() != null) form.setTitle(request.title());
        if (request.description() != null) form.setDescription(request.description());
        if (request.scope() != null) form.setScope(request.scope());
        if (request.ownerOrgId() != null) form.setOwnerOrgId(request.ownerOrgId());
        formRepository.save(form);

        // If form is PUBLISHED, create a new version
        FormVersionEntity version;
        if (form.getStatus() == FormState.PUBLISHED) {
            var latestVersion = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId)
                    .orElseThrow();
            int nextVersion = latestVersion.getVersionNumber() + 1;
            version = new FormVersionEntity(formId, nextVersion, Map.of(), userId);
            version = formVersionRepository.save(version);
            log.info("Created new version {} for published form {}", nextVersion, formId);
        } else {
            // DRAFT: update existing version
            version = formVersionRepository.findTopByFormIdOrderByVersionNumberDesc(formId)
                    .orElseThrow();
        }

        // Update fields if provided
        List<FormFieldDefinition> fieldDefs = request.fields();
        if (fieldDefs != null) {
            fieldDefs = normalizeFields(fieldDefs);
            if (form.getStatus() != FormState.PUBLISHED) {
                formFieldRepository.deleteByFormVersionId(version.getId());
            }
            saveFields(version.getId(), fieldDefs);
        } else {
            fieldDefs = getFieldDefinitions(version.getId());
        }

        return FormDto.from(form, version.getVersionNumber(), fieldDefs);
    }

    @Transactional
    public void deleteForm(UUID formId) {
        var form = findFormOrThrow(formId);
        if (form.getStatus() == FormState.PUBLISHED) {
            throw new InvalidFormStateException("Cannot delete a published form. Close it first.");
        }
        formRepository.delete(form);
        log.info("Deleted form {}", formId);
    }

    @Transactional
    public FormDto publishForm(UUID formId, String userId) {
        var form = findFormOrThrow(formId);
        if (form.getStatus() != FormState.DRAFT) {
            throw new InvalidFormStateException("Only DRAFT forms can be published. Current: " + form.getStatus());
        }
        form.setStatus(FormState.PUBLISHED);
        formRepository.save(form);
        log.info("Published form {}", formId);
        return getForm(formId);
    }

    @Transactional
    public FormDto closeForm(UUID formId, String userId) {
        var form = findFormOrThrow(formId);
        if (form.getStatus() != FormState.PUBLISHED) {
            throw new InvalidFormStateException("Only PUBLISHED forms can be closed. Current: " + form.getStatus());
        }
        form.setStatus(FormState.CLOSED);
        formRepository.save(form);
        log.info("Closed form {}", formId);
        return getForm(formId);
    }

    @Transactional
    public FormDto releaseForm(UUID formId, String userId) {
        var form = findFormOrThrow(formId);
        if (!"LOCAL".equals(form.getScope()) && !"SHARED_WITHIN_HOLDING".equals(form.getScope())) {
            throw new InvalidFormStateException("Only LOCAL or SHARED forms can be released");
        }
        if (form.getReleasedAt() != null) {
            throw new InvalidFormStateException("Form is already released");
        }
        form.setReleasedAt(Instant.now());
        form.setReleasedBy(userId);
        formRepository.save(form);
        log.info("Released form {} by user {}", formId, userId);
        return getForm(formId);
    }

    @Transactional
    public FormDto updateFormScope(UUID formId, String scope, String userId) {
        var form = findFormOrThrow(formId);
        form.setScope(scope);
        formRepository.save(form);
        log.info("Updated form {} scope to {} by user {}", formId, scope, userId);
        return getForm(formId);
    }

    public List<FormVersionDto> getVersions(UUID formId) {
        findFormOrThrow(formId);
        return formVersionRepository.findByFormIdOrderByVersionNumberDesc(formId).stream()
                .map(FormVersionDto::from)
                .toList();
    }

    public FormDto getPreview(UUID formId) {
        return getForm(formId);
    }

    private FormEntity findFormOrThrow(UUID formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
    }

    private void saveFields(UUID formVersionId, List<FormFieldDefinition> fieldDefs) {
        for (var fd : fieldDefs) {
            var field = new FormFieldEntity(
                    formVersionId,
                    fd.fieldKey(),
                    fd.fieldType(),
                    fd.label(),
                    fd.section(),
                    fd.sectionDescription(),
                    fd.sortOrder(),
                    fd.required(),
                    fd.properties() != null ? fd.properties() : Collections.emptyMap()
            );
            formFieldRepository.save(field);
        }
    }

    /**
     * Normalize field definitions — fill in defaults for missing fieldKey/fieldType/label.
     */
    private List<FormFieldDefinition> normalizeFields(List<FormFieldDefinition> fields) {
        List<FormFieldDefinition> normalized = new java.util.ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            var f = fields.get(i);
            String fieldKey = f.fieldKey() != null && !f.fieldKey().isBlank() ? f.fieldKey() : "field_" + (i + 1);
            String fieldType = f.fieldType() != null && !f.fieldType().isBlank() ? f.fieldType() : "text";
            String label = f.label() != null && !f.label().isBlank() ? f.label() : fieldKey;
            normalized.add(new FormFieldDefinition(
                    fieldKey, fieldType, label,
                    f.section(), f.sectionDescription(),
                    f.sortOrder() > 0 ? f.sortOrder() : i + 1,
                    f.required(),
                    f.properties()
            ));
        }
        return normalized;
    }

    private List<FormFieldDefinition> getFieldDefinitions(UUID formVersionId) {
        return formFieldRepository.findByFormVersionIdOrderBySortOrder(formVersionId).stream()
                .map(f -> new FormFieldDefinition(
                        f.getFieldKey(),
                        f.getFieldType(),
                        f.getLabel(),
                        f.getSection(),
                        f.getSectionDescription(),
                        f.getSortOrder(),
                        f.isRequired(),
                        f.getProperties()
                ))
                .toList();
    }
}
