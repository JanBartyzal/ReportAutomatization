package com.reportplatform.tmplpptx.service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    @Value("${azure.storage.blob.connection-string}")
    private String connectionString;

    @Value("${azure.storage.blob.container-name:templates}")
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
     * Upload a PPTX template file to blob storage.
     *
     * @param templateId Template UUID
     * @param version    Version number
     * @param fileBytes  File content
     * @param fileName   Original file name
     * @return Blob URL of the uploaded file
     */
    public String uploadTemplate(UUID templateId, int version, byte[] fileBytes, String fileName) {
        String blobPath = String.format("%s/v%d/%s", templateId, version, fileName);

        var blobClient = containerClient.getBlobClient(blobPath);
        blobClient.upload(new ByteArrayInputStream(fileBytes), fileBytes.length, true);

        String blobUrl = blobClient.getBlobUrl();
        log.info("Uploaded template blob: {} ({} bytes)", blobPath, fileBytes.length);
        return blobUrl;
    }

    /**
     * Download a template file from blob storage.
     *
     * @param blobUrl Blob URL or relative path
     * @return File bytes
     */
    public byte[] downloadTemplate(String blobUrl) {
        String blobPath = extractBlobPath(blobUrl);
        var blobClient = containerClient.getBlobClient(blobPath);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        blobClient.downloadStream(out);
        return out.toByteArray();
    }

    private String extractBlobPath(String blobUrl) {
        // If full URL, extract path after container name
        if (blobUrl.contains("/" + containerName + "/")) {
            return blobUrl.substring(blobUrl.indexOf("/" + containerName + "/") + containerName.length() + 2);
        }
        return blobUrl;
    }
}
