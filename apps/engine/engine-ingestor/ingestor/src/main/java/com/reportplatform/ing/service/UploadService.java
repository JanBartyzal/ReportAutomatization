package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.model.UploadPurpose;
import com.reportplatform.ing.model.dto.UploadResponse;
import com.reportplatform.ing.repository.FileRepository;
import com.reportplatform.scan.service.SecurityScannerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Orchestrates the file upload pipeline:
 * 1. MIME validation (extension, content type, magic bytes)
 * 2. ClamAV antivirus scan (BEFORE blob upload)
 * 3. VBA macro removal for .xlsm/.pptm files
 * 4. Stream upload to Azure Blob Storage
 * 5. Persist file metadata to database
 * 6. Publish file-uploaded event to orchestrator
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final MimeValidationService mimeValidationService;
    private final BlobStorageService blobStorageService;
    private final SecurityScannerService securityScannerService;
    private final OrchestratorTriggerService orchestratorTriggerService;
    private final FileRepository fileRepository;

    public UploadService(MimeValidationService mimeValidationService,
                         BlobStorageService blobStorageService,
                         SecurityScannerService securityScannerService,
                         OrchestratorTriggerService orchestratorTriggerService,
                         FileRepository fileRepository) {
        this.mimeValidationService = mimeValidationService;
        this.blobStorageService = blobStorageService;
        this.securityScannerService = securityScannerService;
        this.orchestratorTriggerService = orchestratorTriggerService;
        this.fileRepository = fileRepository;
    }

    /**
     * Processes a multipart file upload with security-first pipeline:
     * validates, scans, sanitizes, then uploads to blob storage.
     *
     * @param file          the multipart file
     * @param uploadPurpose the intended purpose (PARSE or FORM_IMPORT)
     * @param orgId         organization ID from the authenticated context
     * @param userId        user ID from the authenticated context
     * @return upload response with file metadata
     */
    @Transactional
    public UploadResponse handleUpload(MultipartFile file, UploadPurpose uploadPurpose, UUID orgId, UUID userId) {
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        log.info("Processing upload: filename={}, contentType={}, size={}, purpose={}, org={}",
                originalFilename, contentType, fileSize, uploadPurpose, orgId);

        UUID fileId = UUID.randomUUID();

        try {
            // Step 1: Validate MIME type, extension, magic bytes, and size
            BufferedInputStream validatedStream = mimeValidationService.validate(
                    originalFilename, contentType, file.getInputStream(), fileSize);

            // Buffer the validated stream for scanning (required before blob upload)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            validatedStream.transferTo(buffer);
            byte[] fileContent = buffer.toByteArray();

            // Step 2: ClamAV antivirus scan BEFORE blob upload
            SecurityScannerService.ScanResult scanResult = securityScannerService.scanStream(fileContent);
            if (scanResult.status() == SecurityScannerService.ScanStatus.SCAN_RESULT_INFECTED) {
                log.warn("Upload rejected - infected file: filename={}, threat={}",
                        originalFilename, scanResult.threatName());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "File infected: " + scanResult.threatName());
            }

            // Step 3: VBA macro removal for .xlsm/.pptm files
            boolean macroRemoved = false;
            SecurityScannerService.SanitizeResult sanitizeResult =
                    securityScannerService.removeVbaMacros(fileContent, contentType);
            if (sanitizeResult.macroRemoved()) {
                fileContent = sanitizeResult.sanitizedContent();
                macroRemoved = true;
                log.info("VBA macros removed from file: filename={}", originalFilename);
            }

            // Step 4: Upload sanitized file to blob storage
            String blobPath = blobStorageService.buildBlobPath(orgId, fileId, originalFilename);
            String blobUrl = blobStorageService.uploadStream(
                    blobPath, new ByteArrayInputStream(fileContent), fileContent.length, contentType);

            // Step 5: Set RLS org context so the INSERT passes the row-level security policy,
            // then persist file metadata. The 'true' flag makes the setting transaction-local.
            setRlsContext(orgId);
            FileEntity entity = new FileEntity();
            entity.setId(fileId);
            entity.setOrgId(orgId);
            entity.setUserId(userId);
            entity.setFilename(originalFilename);
            entity.setSizeBytes(fileSize);
            entity.setMimeType(contentType);
            entity.setRawBlobUrl(blobPath);
            entity.setBlobUrl(blobPath);
            entity.setScanStatus(ScanStatus.CLEAN);
            entity.setUploadPurpose(uploadPurpose);
            entity.setMacroRemoved(macroRemoved);
            entity = fileRepository.save(entity);

            // Step 6: Publish event to orchestrator
            orchestratorTriggerService.publishFileUploaded(entity);

            return new UploadResponse(
                    entity.getId(),
                    entity.getFilename(),
                    entity.getSizeBytes(),
                    entity.getMimeType(),
                    entity.getScanStatus(),
                    entity.getUploadPurpose(),
                    entity.getCreatedAt()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to read upload stream for file '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Failed to process file upload: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the PostgreSQL session variable used by RLS policies on the files table.
     * Must be called within an active @Transactional context so the setting applies
     * to the same connection used by subsequent JPA operations.
     * The {@code true} flag makes it transaction-local (auto-reset on commit/rollback).
     */
    private void setRlsContext(UUID orgId) {
        entityManager.createNativeQuery("SELECT set_config('app.current_org_id', :orgId, true)")
                .setParameter("orgId", orgId.toString())
                .getSingleResult();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }
        return filename.replaceAll("[/\\\\]", "_").strip();
    }
}
