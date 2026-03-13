package com.reportplatform.ing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@Service
public class MimeValidationService {

    private static final Logger log = LoggerFactory.getLogger(MimeValidationService.class);

    /**
     * Allowed MIME types mapped to their file extensions.
     */
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx",
            "application/pdf", ".pdf",
            "text/csv", ".csv"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pptx", ".xlsx", ".pdf", ".csv");

    /**
     * Magic bytes signatures for binary verification.
     * - ZIP-based (PPTX, XLSX): PK header 50 4B 03 04
     * - PDF: %PDF (25 50 44 46)
     * - CSV: no magic bytes (text-based), validated by extension + MIME only
     */
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    @Value("${upload.max-size.pptx:52428800}")
    private long maxSizePptx;

    @Value("${upload.max-size.xlsx:52428800}")
    private long maxSizeXlsx;

    @Value("${upload.max-size.csv:52428800}")
    private long maxSizeCsv;

    @Value("${upload.max-size.pdf:104857600}")
    private long maxSizePdf;

    /**
     * Validates the file by checking extension, MIME type, and magic bytes.
     * The returned InputStream is guaranteed to still contain the full content
     * (uses mark/reset on a BufferedInputStream).
     *
     * @param filename    original filename
     * @param contentType declared MIME type
     * @param inputStream file data stream (must support mark/reset or will be wrapped)
     * @param fileSize    declared file size in bytes
     * @return a BufferedInputStream positioned at byte 0
     */
    public BufferedInputStream validate(String filename, String contentType, InputStream inputStream, long fileSize) {
        String extension = extractExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file extension: %s. Allowed: %s".formatted(extension, ALLOWED_EXTENSIONS));
        }

        validateMimeType(contentType, extension);
        validateFileSize(extension, fileSize);

        BufferedInputStream buffered = new BufferedInputStream(inputStream, 8192);
        validateMagicBytes(buffered, extension, filename);

        return buffered;
    }

    private void validateMimeType(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Content type is required");
        }

        // For CSV, allow common MIME variations
        if (".csv".equalsIgnoreCase(extension)) {
            Set<String> csvMimes = Set.of("text/csv", "application/csv", "text/plain", "application/octet-stream");
            if (!csvMimes.contains(contentType.toLowerCase())) {
                throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                        "Invalid MIME type '%s' for CSV file".formatted(contentType));
            }
            return;
        }

        boolean mimeMatchesExtension = MIME_TO_EXTENSION.entrySet().stream()
                .anyMatch(e -> e.getKey().equalsIgnoreCase(contentType) && e.getValue().equalsIgnoreCase(extension));

        // Also allow application/octet-stream as a fallback
        if (!mimeMatchesExtension && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                    "MIME type '%s' does not match extension '%s'".formatted(contentType, extension));
        }
    }

    private void validateFileSize(String extension, long fileSize) {
        long maxSize = switch (extension.toLowerCase()) {
            case ".pptx" -> maxSizePptx;
            case ".xlsx" -> maxSizeXlsx;
            case ".csv" -> maxSizeCsv;
            case ".pdf" -> maxSizePdf;
            default -> throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Unsupported extension: " + extension);
        };

        if (fileSize > maxSize) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                    "File size %d bytes exceeds maximum %d bytes for %s files".formatted(fileSize, maxSize, extension));
        }
    }

    private void validateMagicBytes(BufferedInputStream bis, String extension, String filename) {
        // CSV is text-based; no magic bytes to verify
        if (".csv".equalsIgnoreCase(extension)) {
            return;
        }

        try {
            bis.mark(16);
            byte[] header = new byte[4];
            int bytesRead = bis.read(header);
            bis.reset();

            if (bytesRead < 4) {
                throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                        "File '%s' is too small to be a valid %s file".formatted(filename, extension));
            }

            byte[] expectedMagic = switch (extension.toLowerCase()) {
                case ".pptx", ".xlsx" -> ZIP_MAGIC;
                case ".pdf" -> PDF_MAGIC;
                default -> null;
            };

            if (expectedMagic != null && !Arrays.equals(header, 0, expectedMagic.length, expectedMagic, 0, expectedMagic.length)) {
                log.warn("Magic bytes mismatch for file '{}': expected {}, got {}",
                        filename, bytesToHex(expectedMagic), bytesToHex(header));
                throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                        "File '%s' does not match expected binary signature for %s".formatted(filename, extension));
            }
        } catch (IOException e) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE,
                    "Failed to read file header for validation: " + e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Filename must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private static String bytesToHex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append("%02X ".formatted(b));
        }
        return sb.toString().trim();
    }
}
