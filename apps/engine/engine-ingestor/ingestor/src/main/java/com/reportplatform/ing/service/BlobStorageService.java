package com.reportplatform.ing.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");

    @Value("${azure.storage.blob.connection-string}")
    private String connectionString;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("Created blob container: {}", containerName);
        }
    }

    /**
     * Builds the standard blob path: {org_id}/{yyyy}/{MM}/{file_id}/{original_filename}
     */
    public String buildBlobPath(UUID orgId, UUID fileId, String originalFilename) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return "%s/%s/%s/%s/%s".formatted(
                orgId,
                now.format(YEAR_FORMATTER),
                now.format(MONTH_FORMATTER),
                fileId,
                originalFilename
        );
    }

    /**
     * Builds the raw blob path: {org_id}/{yyyy}/{MM}/{file_id}/_raw/{original_filename}
     */
    public String buildRawBlobPath(UUID orgId, UUID fileId, String originalFilename) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return "%s/%s/%s/%s/_raw/%s".formatted(
                orgId,
                now.format(YEAR_FORMATTER),
                now.format(MONTH_FORMATTER),
                fileId,
                originalFilename
        );
    }

    /**
     * Uploads an input stream to blob storage without loading the entire file into memory.
     *
     * @param blobPath    the target blob path
     * @param inputStream the data stream
     * @param length      content length in bytes
     * @param contentType MIME type of the content
     * @return the blob URL
     */
    public String uploadStream(String blobPath, InputStream inputStream, long length, String contentType) {
        BlobClient blobClient = containerClient.getBlobClient(blobPath);

        var headers = new BlobHttpHeaders().setContentType(contentType);
        var options = new BlobParallelUploadOptions(inputStream)
                .setHeaders(headers);

        if (length > 0) {
            options.setParallelTransferOptions(
                    new com.azure.storage.blob.models.ParallelTransferOptions()
                            .setBlockSizeLong(4L * 1024 * 1024)   // 4 MB blocks
                            .setMaxConcurrency(4)
            );
        }

        blobClient.uploadWithResponse(options, null, null);
        log.info("Uploaded blob: {}", blobPath);
        return blobClient.getBlobUrl();
    }

    /**
     * Downloads the blob content to the given output stream.
     */
    public void downloadToStream(String blobPath, OutputStream outputStream) {
        BlobClient blobClient = containerClient.getBlobClient(blobPath);
        blobClient.downloadStream(outputStream);
    }

    /**
     * Generates a time-limited SAS URL for blob access.
     */
    public String generateSasUrl(String blobPath, int expiryMinutes) {
        BlobClient blobClient = containerClient.getBlobClient(blobPath);
        var expiryTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expiryMinutes);
        var permissions = new BlobSasPermission().setReadPermission(true);
        var sasValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues);
    }
}
