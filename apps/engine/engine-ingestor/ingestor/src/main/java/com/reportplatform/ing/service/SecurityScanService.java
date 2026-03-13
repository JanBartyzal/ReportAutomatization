package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.repository.FileRepository;
import com.reportplatform.scan.service.SecurityScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Performs virus scanning and file sanitization by directly calling
 * SecurityScannerService (in-process, since scanner is now consolidated
 * into the same deployment unit).
 * <p>
 * Previously called MS-SCAN via Dapr service invocation; now uses
 * local Spring injection for zero-latency in-process calls.
 */
@Service
public class SecurityScanService {

    private static final Logger log = LoggerFactory.getLogger(SecurityScanService.class);

    private final SecurityScannerService securityScannerService;
    private final FileRepository fileRepository;

    public SecurityScanService(SecurityScannerService securityScannerService,
                               FileRepository fileRepository) {
        this.securityScannerService = securityScannerService;
        this.fileRepository = fileRepository;
    }

    /**
     * Performs the full scan + sanitize workflow for the given file entity.
     * Updates the entity's scan status and blob URLs accordingly.
     *
     * @param fileEntity the persisted file entity with rawBlobUrl already set
     */
    public void scanAndSanitize(FileEntity fileEntity) {
        try {
            // Step 1: Scan for viruses via in-process SecurityScannerService
            var scanResult = securityScannerService.scanFile(
                    fileEntity.getId().toString(), fileEntity.getRawBlobUrl());

            if (scanResult.status() == SecurityScannerService.ScanStatus.SCAN_RESULT_INFECTED) {
                log.warn("File {} is infected: {}", fileEntity.getId(), scanResult.threatName());
                fileEntity.setScanStatus(ScanStatus.INFECTED);
                fileRepository.save(fileEntity);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "File infected: " + scanResult.threatName());
            }

            // Step 2: Sanitize clean file
            var sanitizeResult = securityScannerService.sanitizeFile(
                    fileEntity.getId().toString(), fileEntity.getRawBlobUrl(), fileEntity.getMimeType());

            fileEntity.setScanStatus(ScanStatus.CLEAN);
            fileEntity.setBlobUrl(sanitizeResult.sanitizedBlobUrl());
            fileRepository.save(fileEntity);

            log.info("File {} scanned and sanitized successfully", fileEntity.getId());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Security scan failed for file {}: {}", fileEntity.getId(), e.getMessage(), e);
            fileEntity.setScanStatus(ScanStatus.ERROR);
            fileRepository.save(fileEntity);
            throw new RuntimeException("Scan failed", e);
        }
    }
}
