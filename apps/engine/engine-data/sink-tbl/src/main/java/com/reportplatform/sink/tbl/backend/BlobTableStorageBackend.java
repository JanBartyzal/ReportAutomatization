package com.reportplatform.sink.tbl.backend;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.tbl.service.TableSinkService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blob/ADLS Gen2 implementation of {@link TableStorageBackend}.
 * <p>
 * Serialises the full table batch (headers + rows) to a single JSON document
 * and uploads it to Azure Blob Storage under the path:
 * <pre>
 *   {blobSinkContainer}/{orgId}/{fileId}/{sourceSheet}.json
 * </pre>
 * This is the "raw JSON in Blob" path – lighter than the full Spark pipeline
 * and readable directly by the frontend via the
 * {@code GET /api/query/sinks/data-source-stats} and Blob proxy endpoints.
 * </p>
 * <p>
 * Unlike the {@link PostgresTableStorageBackend}, <em>no rows are written to
 * {@code parsed_tables}</em>.  The read-side integration calls a dedicated
 * {@code BlobQueryService} to list / read blobs and materialise the same DTO
 * contract.
 * </p>
 */
@Component
public class BlobTableStorageBackend implements TableStorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(BlobTableStorageBackend.class);
    public static final String BACKEND_TYPE = "BLOB";

    private static final String CONTENT_TYPE = "application/json";

    @Value("${azure.storage.blob.connection-string:UseDevelopmentStorage=true}")
    private String connectionString;

    @Value("${azure.storage.blob.sink-container:table-sink-blob}")
    private String sinkContainer;

    private final ObjectMapper objectMapper;

    private BlobContainerClient containerClient;

    public BlobTableStorageBackend(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            containerClient = serviceClient.getBlobContainerClient(sinkContainer);
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("[BLOB] Created sink container: {}", sinkContainer);
            }
        } catch (Exception e) {
            logger.warn("[BLOB] Could not initialise Azure Blob container '{}': {} – uploads will fail at runtime",
                    sinkContainer, e.getMessage());
        }
    }

    @Override
    public String backendType() {
        return BACKEND_TYPE;
    }

    /**
     * Serialises each sheet record to JSON and uploads as a separate blob.
     * Path: {@code {orgId}/{fileId}/{sourceSheet}_{timestamp}.json}
     *
     * @return number of blobs successfully uploaded
     */
    @Override
    public int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableSinkService.TableRecordData> records) {

        logger.info("[BLOB] Uploading {} record(s) for fileId={}, orgId={}, sourceType={}",
                records.size(), fileId, orgId, sourceType);

        int uploaded = 0;
        for (TableSinkService.TableRecordData rec : records) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("fileId", fileId);
                payload.put("orgId", orgId);
                payload.put("sourceType", sourceType);
                payload.put("sourceSheet", rec.sourceSheet());
                payload.put("headers", rec.headers());
                payload.put("rows", rec.rows());
                payload.put("metadata", rec.metadata());
                payload.put("storageBackend", BACKEND_TYPE);
                payload.put("createdAt", Instant.now().toString());

                byte[] json = objectMapper.writeValueAsBytes(payload);
                String blobName = "%s/%s/%s_%d.json".formatted(
                        orgId, fileId,
                        sanitise(rec.sourceSheet()),
                        System.nanoTime());

                BlobClient blobClient = containerClient.getBlobClient(blobName);
                blobClient.upload(new ByteArrayInputStream(json), json.length, true);

                // Attach metadata for server-side listing
                blobClient.setMetadata(Map.of(
                        "fileId", fileId,
                        "orgId", orgId,
                        "sourceType", sourceType,
                        "sourceSheet", rec.sourceSheet() != null ? rec.sourceSheet() : ""));

                logger.debug("[BLOB] Uploaded blob: {}", blobName);
                uploaded++;
            } catch (Exception e) {
                logger.error("[BLOB] Failed to upload record for sheet='{}', fileId={}: {}",
                        rec.sourceSheet(), fileId, e.getMessage(), e);
                throw new RuntimeException("Blob upload failed for fileId=" + fileId +
                        ", sheet=" + rec.sourceSheet(), e);
            }
        }

        logger.info("[BLOB] Uploaded {}/{} blobs for fileId={}", uploaded, records.size(), fileId);
        return uploaded;
    }

    /**
     * Deletes all blobs under the {@code {orgId}/{fileId}/} prefix.
     *
     * @return number of blobs deleted
     */
    @Override
    public int deleteByFileId(String fileId) {
        // We can't efficiently look up orgId here; use a flat fileId prefix instead.
        // The blob path is {orgId}/{fileId}/... but we list by fileId in the name.
        String prefix = fileId + "/";
        int deleted = 0;

        try {
            for (var item : containerClient.listBlobsByHierarchy(prefix)) {
                if (item.isPrefix() == null || !item.isPrefix()) {
                    containerClient.getBlobClient(item.getName()).deleteIfExists();
                    deleted++;
                }
            }
            // Also walk all orgs by listing everything (fallback for older data)
            if (deleted == 0) {
                for (var item : containerClient.listBlobs()) {
                    if (item.getName().contains("/" + fileId + "/")) {
                        containerClient.getBlobClient(item.getName()).deleteIfExists();
                        deleted++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[BLOB] Failed to delete blobs for fileId={}: {}", fileId, e.getMessage(), e);
        }

        logger.info("[BLOB] Deleted {} blob(s) for fileId={}", deleted, fileId);
        return deleted;
    }

    private static String sanitise(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
