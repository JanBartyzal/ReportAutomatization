package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.model.UploadPurpose;
import com.reportplatform.ing.model.dto.UploadResponse;
import com.reportplatform.ing.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Orchestrates the file upload pipeline:
 * 1. MIME validation (extension, content type, magic bytes)
 * 2. Stream upload to Azure Blob Storage (raw path)
 * 3. Persist file metadata to database
 * 4. Trigger security scan + sanitization
 * 5. Publish file-uploaded event to orchestrator
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final MimeValidationService mimeValidationService;
    private final BlobStorageService blobStorageService;
    private final SecurityScanService securityScanService;
    private final OrchestratorTriggerService orchestratorTriggerService;
    private final FileRepository fileRepository;

    public UploadService(MimeValidationService mimeValidationService,
                         BlobStorageService blobStorageService,
                         SecurityScanService securityScanService,
                         OrchestratorTriggerService orchestratorTriggerService,
                         FileRepository fileRepository) {
        this.mimeValidationService = mimeValidationService;
        this.blobStorageService = blobStorageService;
        this.securityScanService = securityScanService;
        this.orchestratorTriggerService = orchestratorTriggerService;
        this.fileRepository = fileRepository;
    }

    /**
     * Processes a multipart file upload. The file is never fully loaded into memory:
     * it is streamed from the multipart input through validation directly to blob storage.
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

        // Generate a file ID upfront for blob path construction
        UUID fileId = UUID.randomUUID();

        try {
            // Step 1: Validate MIME type, extension, magic bytes, and size
            BufferedInputStream validatedStream = mimeValidationService.validate(
                    originalFilename, contentType, file.getInputStream(), fileSize);

            // Step 2: Upload raw file to blob storage (streaming, never full in memory)
            String rawBlobPath = blobStorageService.buildRawBlobPath(orgId, fileId, originalFilename);
            String rawBlobUrl = blobStorageService.uploadStream(rawBlobPath, validatedStream, fileSize, contentType);

            // Standard blob path (for sanitized version)
            String standardBlobPath = blobStorageService.buildBlobPath(orgId, fileId, originalFilename);

            // Step 3: Persist file metadata
            FileEntity entity = new FileEntity();
            entity.setId(fileId);
            entity.setOrgId(orgId);
            entity.setUserId(userId);
            entity.setFilename(originalFilename);
            entity.setSizeBytes(fileSize);
            entity.setMimeType(contentType);
            entity.setRawBlobUrl(rawBlobPath);
            entity.setBlobUrl(standardBlobPath);
            entity.setScanStatus(ScanStatus.PENDING);
            entity.setUploadPurpose(uploadPurpose);
            entity = fileRepository.save(entity);

            // Step 4: Security scan and sanitization (async-capable, but called synchronously here)
            securityScanService.scanAndSanitize(entity);

            // Step 5: Publish event to orchestrator
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
        } catch (IOException e) {
            log.error("Failed to read upload stream for file '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Failed to process file upload: " + e.getMessage(), e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }
        // Remove path separators to prevent directory traversal
        return filename.replaceAll("[/\\\\]", "_").strip();
    }
}
