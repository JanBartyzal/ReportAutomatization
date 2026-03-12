package com.reportplatform.tmplpptx.service;

import com.reportplatform.tmplpptx.dto.*;
import com.reportplatform.tmplpptx.entity.*;
import com.reportplatform.tmplpptx.exception.TemplateNotFoundException;
import com.reportplatform.tmplpptx.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PptxTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PptxTemplateService.class);
    private static final Set<String> VALID_SCOPES = Set.of("CENTRAL", "LOCAL", "SHARED_WITHIN_HOLDING");

    private final PptxTemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplatePlaceholderRepository placeholderRepository;
    private final BlobStorageService blobStorageService;
    private final PlaceholderExtractorService extractorService;

    public PptxTemplateService(PptxTemplateRepository templateRepository,
                                TemplateVersionRepository versionRepository,
                                TemplatePlaceholderRepository placeholderRepository,
                                BlobStorageService blobStorageService,
                                PlaceholderExtractorService extractorService) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.placeholderRepository = placeholderRepository;
        this.blobStorageService = blobStorageService;
        this.extractorService = extractorService;
    }

    /**
     * Upload a new PPTX template.
     */
    public TemplateUploadResponse uploadTemplate(MultipartFile file, String name, String description,
                                                   String scope, String reportType,
                                                   String orgId, String userId) {
        if (scope != null && !VALID_SCOPES.contains(scope)) {
            throw new IllegalArgumentException("Invalid scope: " + scope + ". Valid: " + VALID_SCOPES);
        }
        byte[] fileBytes = readFileBytes(file);

        // Create template entity
        var template = new PptxTemplateEntity(name, orgId, scope != null ? scope : "CENTRAL", reportType, userId);
        template.setDescription(description);
        template.setOwnerOrgId(orgId);
        template = templateRepository.save(template);

        // Upload to blob storage
        String blobUrl = blobStorageService.uploadTemplate(template.getId(), 1, fileBytes, file.getOriginalFilename());

        // Create version
        var version = new TemplateVersionEntity(template, 1, blobUrl, (long) fileBytes.length, userId);
        version = versionRepository.save(version);

        // Extract and save placeholders
        List<PlaceholderResponse> placeholders = extractorService.extractPlaceholders(fileBytes);
        savePlaceholders(version, placeholders);

        log.info("Uploaded template '{}' (id={}, version=1, {} placeholders)", name, template.getId(), placeholders.size());

        return new TemplateUploadResponse(
                template.getId(), name, scope, 1, blobUrl, placeholders, template.getCreatedAt());
    }

    /**
     * Update an existing template with a new version.
     */
    public TemplateUploadResponse updateTemplate(UUID templateId, MultipartFile file, String userId) {
        var template = templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        byte[] fileBytes = readFileBytes(file);

        // Mark previous version as non-current
        versionRepository.clearCurrentVersion(templateId);

        // Create new version
        int nextVersion = versionRepository.findMaxVersionByTemplateId(templateId) + 1;
        String blobUrl = blobStorageService.uploadTemplate(templateId, nextVersion, fileBytes, file.getOriginalFilename());

        var version = new TemplateVersionEntity(template, nextVersion, blobUrl, (long) fileBytes.length, userId);
        version = versionRepository.save(version);

        // Extract placeholders for new version
        List<PlaceholderResponse> placeholders = extractorService.extractPlaceholders(fileBytes);
        savePlaceholders(version, placeholders);

        log.info("Updated template id={} to version={}", templateId, nextVersion);

        return new TemplateUploadResponse(
                templateId, template.getName(), template.getScope(), nextVersion, blobUrl,
                placeholders, template.getCreatedAt());
    }

    /**
     * List active templates, optionally filtered by orgId.
     */
    @Transactional(readOnly = true)
    public Page<TemplateListResponse> listTemplates(String orgId, String scope, Pageable pageable) {
        Page<PptxTemplateEntity> page;
        if (scope != null && !scope.isBlank()) {
            page = templateRepository.findByScopeAndActiveTrueOrderByCreatedAtDesc(scope, pageable);
        } else if (orgId != null && !orgId.isBlank()) {
            page = templateRepository.findVisibleTemplates(orgId, pageable);
        } else {
            page = templateRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
        }

        return page.map(t -> {
            int currentVersion = versionRepository.findMaxVersionByTemplateId(t.getId());
            return new TemplateListResponse(
                    t.getId(), t.getName(), t.getDescription(), t.getScope(),
                    t.getReportType(), currentVersion, t.getCreatedAt(), t.getUpdatedAt());
        });
    }

    /**
     * Get template detail with current version and placeholders.
     */
    @Transactional(readOnly = true)
    public TemplateDetailResponse getTemplateDetail(UUID templateId) {
        var template = templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        var currentVersion = versionRepository.findByTemplateIdAndCurrentTrue(templateId).orElse(null);

        List<PlaceholderResponse> placeholders = List.of();
        TemplateDetailResponse.VersionInfo versionInfo = null;

        if (currentVersion != null) {
            placeholders = placeholderRepository.findByVersionId(currentVersion.getId()).stream()
                    .map(p -> new PlaceholderResponse(p.getPlaceholderKey(), p.getPlaceholderType(),
                            p.getSlideIndex(), p.getShapeName()))
                    .toList();
            versionInfo = new TemplateDetailResponse.VersionInfo(
                    currentVersion.getId(), currentVersion.getVersion(), currentVersion.getBlobUrl(),
                    currentVersion.getFileSizeBytes(), currentVersion.getUploadedBy(),
                    currentVersion.getUploadedAt());
        }

        return new TemplateDetailResponse(
                template.getId(), template.getName(), template.getDescription(),
                template.getScope(), template.getReportType(), template.isActive(),
                template.getCreatedBy(), template.getCreatedAt(), template.getUpdatedAt(),
                versionInfo, placeholders);
    }

    /**
     * Soft-deactivate a template.
     */
    public void deactivateTemplate(UUID templateId) {
        var template = templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        template.setActive(false);
        templateRepository.save(template);
        log.info("Deactivated template id={}", templateId);
    }

    /**
     * Get preview URL for the current version of a template.
     */
    @Transactional(readOnly = true)
    public String getPreviewUrl(UUID templateId) {
        templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        var version = versionRepository.findByTemplateIdAndCurrentTrue(templateId)
                .orElseThrow(() -> new IllegalStateException("No current version for template: " + templateId));
        return version.getBlobUrl();
    }

    /**
     * Get placeholders for the current version of a template.
     */
    @Transactional(readOnly = true)
    public List<PlaceholderResponse> getPlaceholders(UUID templateId) {
        templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        var version = versionRepository.findByTemplateIdAndCurrentTrue(templateId).orElse(null);
        if (version == null) return List.of();

        return placeholderRepository.findByVersionId(version.getId()).stream()
                .map(p -> new PlaceholderResponse(p.getPlaceholderKey(), p.getPlaceholderType(),
                        p.getSlideIndex(), p.getShapeName()))
                .toList();
    }

    private void savePlaceholders(TemplateVersionEntity version, List<PlaceholderResponse> placeholders) {
        for (var ph : placeholders) {
            placeholderRepository.save(new TemplatePlaceholderEntity(
                    version, ph.key(), ph.type(), ph.slideIndex(), ph.shapeName()));
        }
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }
}
