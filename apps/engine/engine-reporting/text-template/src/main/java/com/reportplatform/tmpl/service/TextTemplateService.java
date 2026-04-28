package com.reportplatform.tmpl.service;

import com.reportplatform.tmpl.dto.*;
import com.reportplatform.tmpl.entity.TextTemplateEntity;
import com.reportplatform.tmpl.entity.TextTemplateVersionEntity;
import com.reportplatform.tmpl.repository.TextTemplateRepository;
import com.reportplatform.tmpl.repository.TextTemplateVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD and versioning for text templates.
 * Every update creates a version snapshot for audit and rollback support.
 */
@Service
public class TextTemplateService {

    private static final Logger log = LoggerFactory.getLogger(TextTemplateService.class);

    private final TextTemplateRepository templateRepo;
    private final TextTemplateVersionRepository versionRepo;

    public TextTemplateService(TextTemplateRepository templateRepo,
                                TextTemplateVersionRepository versionRepo) {
        this.templateRepo = templateRepo;
        this.versionRepo = versionRepo;
    }

    @Transactional(readOnly = true)
    public List<TextTemplateDto> listAccessible(UUID orgId) {
        return templateRepo.findAccessibleByOrgId(orgId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<TextTemplateDto> listAccessiblePageable(UUID orgId, int page, int size) {
        return templateRepo.findAccessibleByOrgIdPageable(orgId, PageRequest.of(page, Math.min(size, 50)))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<TextTemplateDto> findById(UUID orgId, UUID templateId) {
        return templateRepo.findByIdAndOrgAccess(templateId, orgId).map(this::toDto);
    }

    @Transactional
    public TextTemplateDto create(UUID orgId, String createdBy, CreateTextTemplateRequest req) {
        TextTemplateEntity entity = new TextTemplateEntity(
                orgId, req.name(), req.description(),
                req.templateType(), req.content(),
                req.outputFormats(), req.dataBindings(),
                req.scope(), false, createdBy);
        TextTemplateEntity saved = templateRepo.save(entity);
        // Snapshot initial version
        versionRepo.save(new TextTemplateVersionEntity(
                saved.getId(), 1, saved.getContent(), saved.getDataBindings(), createdBy));
        log.info("Text template created: {} (id={})", saved.getName(), saved.getId());
        return toDto(saved);
    }

    @Transactional
    public TextTemplateDto update(UUID orgId, UUID templateId, String updatedBy, UpdateTextTemplateRequest req) {
        TextTemplateEntity entity = templateRepo.findByIdAndOrgAccess(templateId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Text template not found: " + templateId));
        if (entity.isSystem()) {
            throw new IllegalStateException("System templates cannot be modified.");
        }

        if (req.name() != null) entity.setName(req.name());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.templateType() != null) entity.setTemplateType(req.templateType());
        if (req.content() != null) entity.setContent(req.content());
        if (req.outputFormats() != null) entity.setOutputFormats(req.outputFormats());
        if (req.dataBindings() != null) entity.setDataBindings(req.dataBindings());
        if (req.scope() != null) entity.setScope(req.scope());
        if (req.active() != null) entity.setActive(req.active());

        entity.setVersion(entity.getVersion() + 1);
        entity.setUpdatedAt(Instant.now());

        TextTemplateEntity saved = templateRepo.save(entity);
        // Snapshot new version
        versionRepo.save(new TextTemplateVersionEntity(
                saved.getId(), saved.getVersion(),
                saved.getContent(), saved.getDataBindings(), updatedBy));

        return toDto(saved);
    }

    @Transactional
    public void delete(UUID orgId, UUID templateId) {
        TextTemplateEntity entity = templateRepo.findByIdAndOrgAccess(templateId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Text template not found: " + templateId));
        if (entity.isSystem()) {
            throw new IllegalStateException("System templates cannot be deleted.");
        }
        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        templateRepo.save(entity);
    }

    @Transactional(readOnly = true)
    public List<TextTemplateVersionEntity> getVersions(UUID orgId, UUID templateId) {
        templateRepo.findByIdAndOrgAccess(templateId, orgId)
                .orElseThrow(() -> new NoSuchElementException("Text template not found: " + templateId));
        return versionRepo.findByTemplateIdOrderByVersionDesc(templateId);
    }

    // ---- Mapping ----

    private TextTemplateDto toDto(TextTemplateEntity e) {
        return new TextTemplateDto(
                e.getId(), e.getOrgId(), e.getName(), e.getDescription(),
                e.getTemplateType(), e.getContent(), e.getOutputFormats(),
                e.getDataBindings(), e.getScope(), e.isSystem(), e.isActive(),
                e.getVersion(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
