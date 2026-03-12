package com.reportplatform.ing.service;

import com.reportplatform.ing.model.FileEntity;
import com.reportplatform.ing.model.ScanStatus;
import com.reportplatform.ing.repository.FileRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Calls MS-SCAN via Dapr service invocation for virus scanning and file sanitization.
 * <p>
 * Flow:
 * 1. Invoke ScannerService.ScanFile on the raw blob path
 * 2. If infected -> reject with 422
 * 3. If clean -> invoke ScannerService.SanitizeFile
 * 4. Store the original in _raw/ path, sanitized at standard path
 */
@Service
public class SecurityScanService {

    private static final Logger log = LoggerFactory.getLogger(SecurityScanService.class);

    private final DaprClient daprClient;
    private final FileRepository fileRepository;
    private final BlobStorageService blobStorageService;

    @Value("${dapr.scan-service.app-id}")
    private String scanServiceAppId;

    public SecurityScanService(DaprClient daprClient,
                               FileRepository fileRepository,
                               BlobStorageService blobStorageService) {
        this.daprClient = daprClient;
        this.fileRepository = fileRepository;
        this.blobStorageService = blobStorageService;
    }

    /**
     * Performs the full scan + sanitize workflow for the given file entity.
     * Updates the entity's scan status and blob URLs accordingly.
     *
     * @param fileEntity the persisted file entity with rawBlobUrl already set
     */
    public void scanAndSanitize(FileEntity fileEntity) {
        UUID fileId = fileEntity.getId();
        String rawBlobPath = fileEntity.getRawBlobUrl();

        try {
            // Step 1: Invoke scan
            ScanResponse scanResult = invokeScan(rawBlobPath, fileId);

            if (scanResult.infected()) {
                log.warn("File {} is infected: {}", fileId, scanResult.details());
                fileEntity.setScanStatus(ScanStatus.INFECTED);
                fileRepository.save(fileEntity);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "File is infected and cannot be processed: " + scanResult.details());
            }

            // Step 2: Sanitize clean file
            String sanitizedBlobPath = invokeSanitize(rawBlobPath, fileEntity.getBlobUrl(), fileId);

            fileEntity.setScanStatus(ScanStatus.CLEAN);
            fileEntity.setBlobUrl(sanitizedBlobPath);
            fileRepository.save(fileEntity);

            log.info("File {} scanned and sanitized successfully", fileId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Security scan failed for file {}: {}", fileId, e.getMessage(), e);
            fileEntity.setScanStatus(ScanStatus.ERROR);
            fileRepository.save(fileEntity);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Security scan failed: " + e.getMessage());
        }
    }

    private ScanResponse invokeScan(String blobPath, UUID fileId) {
        var request = Map.of(
                "blob_path", blobPath,
                "file_id", fileId.toString()
        );

        return daprClient.invokeMethod(
                scanServiceAppId,
                "api/scan",
                request,
                HttpExtension.POST,
                ScanResponse.class
        ).block();
    }

    private String invokeSanitize(String rawBlobPath, String targetBlobPath, UUID fileId) {
        var request = Map.of(
                "source_blob_path", rawBlobPath,
                "target_blob_path", targetBlobPath,
                "file_id", fileId.toString()
        );

        SanitizeResponse response = daprClient.invokeMethod(
                scanServiceAppId,
                "api/sanitize",
                request,
                HttpExtension.POST,
                SanitizeResponse.class
        ).block();

        return response != null ? response.sanitizedBlobPath() : targetBlobPath;
    }

    private record ScanResponse(boolean infected, String details) {
    }

    private record SanitizeResponse(String sanitizedBlobPath) {
    }
}
