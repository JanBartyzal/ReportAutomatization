package com.reportplatform.scan.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
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

    private static final String VBA_PROJECT_REL_TYPE =
            "http://schemas.microsoft.com/office/2006/relationships/vbaProject";

    private static final String XLSM_MIME = "application/vnd.ms-excel.sheet.macroEnabled.12";
    private static final String PPTM_MIME = "application/vnd.ms-powerpoint.presentation.macroEnabled.12";

    private final ClamavClientService clamavClientService;

    @Value("${azure.storage.blob.connection-string}")
    private String blobConnectionString;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    public SecurityScannerService(ClamavClientService clamavClientService) {
        this.clamavClientService = clamavClientService;
    }

    /**
     * Scan raw file bytes for viruses using ClamAV.
     * Called BEFORE blob upload to prevent infected files from reaching storage.
     */
    public ScanResult scanStream(byte[] fileContent) throws IOException {
        logger.info("Scanning file stream: size={}B", fileContent.length);
        long startTime = System.currentTimeMillis();

        ClamavClientService.ScanResult scanResult =
                clamavClientService.scanFile(new ByteArrayInputStream(fileContent));

        long duration = System.currentTimeMillis() - startTime;

        if (scanResult.clean()) {
            logger.info("Stream scan completed: result=CLEAN, duration={}ms", duration);
            return new ScanResult(ScanStatus.SCAN_RESULT_CLEAN, null, duration);
        } else {
            logger.warn("Stream scan completed: result=INFECTED, threat={}", scanResult.threatName());
            return new ScanResult(ScanStatus.SCAN_RESULT_INFECTED, scanResult.threatName(), duration);
        }
    }

    /**
     * Scan a file from blob storage for viruses.
     */
    public ScanResult scanFile(String fileId, String blobUrl) throws IOException {
        logger.info("Scanning file: fileId={}, blobUrl={}", fileId, blobUrl);

        InputStream fileStream = downloadFromBlob(blobUrl);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileStream.transferTo(baos);

        return scanStream(baos.toByteArray());
    }

    /**
     * Remove VBA macros from macro-enabled Office files (.xlsm, .pptm).
     * Uses Apache POI OPCPackage to strip the VBA project parts.
     *
     * @param fileContent raw file bytes
     * @param mimeType    the MIME type of the file
     * @return sanitized bytes and metadata, or original bytes if no macros found
     */
    public SanitizeResult removeVbaMacros(byte[] fileContent, String mimeType) {
        List<String> removedItems = new ArrayList<>();

        if (mimeType == null) {
            return new SanitizeResult(null, fileContent, removedItems, false);
        }

        boolean isMacroEnabled = XLSM_MIME.equals(mimeType) || PPTM_MIME.equals(mimeType);
        if (!isMacroEnabled) {
            return new SanitizeResult(null, fileContent, removedItems, false);
        }

        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(fileContent))) {
            List<PackagePart> vbaParts = new ArrayList<>(
                    pkg.getPartsByRelationshipType(VBA_PROJECT_REL_TYPE));

            if (vbaParts.isEmpty()) {
                logger.info("No VBA project found in macro-enabled file");
                return new SanitizeResult(null, fileContent, removedItems, false);
            }

            for (PackagePart part : vbaParts) {
                logger.info("Removing VBA part: {}", part.getPartName());
                pkg.removePart(part);
            }
            removedItems.add("VBA_MACROS");

            ByteArrayOutputStream sanitizedStream = new ByteArrayOutputStream();
            pkg.save(sanitizedStream);
            byte[] sanitizedContent = sanitizedStream.toByteArray();

            logger.info("VBA macros removed: originalSize={}B, sanitizedSize={}B",
                    fileContent.length, sanitizedContent.length);

            return new SanitizeResult(null, sanitizedContent, removedItems, true);

        } catch (Exception e) {
            logger.error("Failed to remove VBA macros: {}", e.getMessage(), e);
            return new SanitizeResult(null, fileContent, removedItems, false);
        }
    }

    /**
     * Sanitize file by removing VBA macros and external links.
     * Legacy method that works with blob storage URLs.
     */
    public SanitizeResult sanitizeFile(String fileId, String blobUrl, String mimeType) throws IOException {
        logger.info("Sanitizing file: fileId={}, mimeType={}", fileId, mimeType);

        InputStream originalStream = downloadFromBlob(blobUrl);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalStream.transferTo(baos);
        byte[] originalContent = baos.toByteArray();

        SanitizeResult macroResult = removeVbaMacros(originalContent, mimeType);
        byte[] sanitizedContent = macroResult.sanitizedContent();
        List<String> removedItems = new ArrayList<>(macroResult.removedItems());

        // For Office files, mark external links as removed
        if (mimeType != null && (mimeType.contains("officedocument") || mimeType.contains("ms-excel"))) {
            removedItems.add("EXTERNAL_LINKS");
        }

        String sanitizedBlobUrl = blobUrl;
        if (!removedItems.isEmpty()) {
            sanitizedBlobUrl = uploadToBlob(fileId, new ByteArrayInputStream(sanitizedContent), mimeType);
        }

        logger.info("Sanitization completed: fileId={}, removedItems={}", fileId, removedItems);

        return new SanitizeResult(sanitizedBlobUrl, sanitizedContent, removedItems, macroResult.macroRemoved());
    }

    private InputStream downloadFromBlob(String blobUrl) throws IOException {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

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

    public record SanitizeResult(String sanitizedBlobUrl, byte[] sanitizedContent,
                                 List<String> removedItems, boolean macroRemoved) {
    }
}
