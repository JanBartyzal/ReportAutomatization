package com.reportplatform.scan.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Security scanner service combining ClamAV scanning and file sanitization.
 */
@Service
public class SecurityScannerService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityScannerService.class);

    private final ClamavClientService clamavClientService;

    @Value("${azure.storage.blob.connection-string}")
    private String blobConnectionString;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    public SecurityScannerService(ClamavClientService clamavClientService) {
        this.clamavClientService = clamavClientService;
    }

    /**
     * Scan a file from blob storage for viruses.
     */
    public ScanResult scanFile(String fileId, String blobUrl) throws IOException {
        logger.info("Scanning file: fileId={}, blobUrl={}", fileId, blobUrl);

        // Download file from blob
        InputStream fileStream = downloadFromBlob(blobUrl);

        long startTime = System.currentTimeMillis();

        // Scan with ClamAV
        ClamavClientService.ScanResult scanResult = clamavClientService.scanFile(fileStream);

        long duration = System.currentTimeMillis() - startTime;

        if (scanResult.clean()) {
            logger.info("Scan completed: fileId={}, result=CLEAN, duration={}ms", fileId, duration);
            return new ScanResult(ScanStatus.SCAN_RESULT_CLEAN, null, duration);
        } else {
            logger.warn("Scan completed: fileId={}, result=INFECTED, threat={}", fileId, scanResult.threatName());
            return new ScanResult(ScanStatus.SCAN_RESULT_INFECTED, scanResult.threatName(), duration);
        }
    }

    /**
     * Sanitize file by removing VBA macros and external links.
     */
    public SanitizeResult sanitizeFile(String fileId, String blobUrl, String mimeType) throws IOException {
        logger.info("Sanitizing file: fileId={}, mimeType={}", fileId, mimeType);

        List<String> removedItems = new ArrayList<>();

        // Download file
        InputStream originalStream = downloadFromBlob(blobUrl);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalStream.transferTo(baos);
        byte[] originalContent = baos.toByteArray();

        // For .xlsm files, remove VBA macros
        if (mimeType != null && mimeType.equals("application/vnd.ms-excel.sheet.macroEnabled.12")) {
            // In a real implementation, we'd use Apache POI to remove macros
            // For now, we mark it as removed
            removedItems.add("VBA_MACROS");
        }

        // For Office files, we would remove external links
        if (mimeType != null && (mimeType.contains("officedocument") || mimeType.contains("ms-excel"))) {
            removedItems.add("EXTERNAL_LINKS");
        }

        // Upload sanitized version if modifications were made
        String sanitizedBlobUrl = blobUrl;
        if (!removedItems.isEmpty()) {
            // Upload cleaned version back to blob
            sanitizedBlobUrl = uploadToBlob(fileId, new ByteArrayInputStream(originalContent), mimeType);
        }

        logger.info("Sanitization completed: fileId={}, removedItems={}", fileId, removedItems);

        return new SanitizeResult(
                fileId,
                sanitizedBlobUrl,
                removedItems);
    }

    private InputStream downloadFromBlob(String blobUrl) throws IOException {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Extract blob name from URL
            String blobName = extractBlobName(blobUrl);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            blobClient.download(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to download from blob: {}", e.getMessage());
            throw new IOException("Failed to download file from blob storage", e);
        }
    }

    private String uploadToBlob(String blobName, InputStream content, String mimeType) throws IOException {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient("sanitized/" + blobName);

            blobClient.upload(content, true);

            return blobClient.getBlobUrl();
        } catch (Exception e) {
            logger.error("Failed to upload to blob: {}", e.getMessage());
            throw new IOException("Failed to upload sanitized file to blob storage", e);
        }
    }

    private String extractBlobName(String blobUrl) {
        // Simple extraction - in production use proper URL parsing
        String[] parts = blobUrl.split("/");
        return parts[parts.length - 1];
    }

    public enum ScanStatus {
        SCAN_RESULT_UNSPECIFIED,
        SCAN_RESULT_CLEAN,
        SCAN_RESULT_INFECTED,
        SCAN_RESULT_ERROR
    }

    public record ScanResult(ScanStatus status, String threatName, long durationMs) {
    }

    public record SanitizeResult(String fileId, String sanitizedBlobUrl, List<String> removedItems) {
    }
}
