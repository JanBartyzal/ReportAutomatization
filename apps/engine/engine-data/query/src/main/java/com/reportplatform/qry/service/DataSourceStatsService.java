package com.reportplatform.qry.service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.reportplatform.qry.repository.QryParsedTableRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

/**
 * Aggregates data-source statistics for the three storage backends:
 * <ul>
 *   <li><b>POSTGRES</b> – row counts from {@code parsed_tables}</li>
 *   <li><b>SPARK</b>   – external Delta Lake query endpoint (availability probe)</li>
 *   <li><b>BLOB</b>    – Azure Blob Storage blob count under the sink container</li>
 * </ul>
 *
 * <p>Used by the frontend DataSourceSwitcher to show per-backend record counts
 * and availability indicators.</p>
 */
@Service
public class DataSourceStatsService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceStatsService.class);

    private final QryParsedTableRepository parsedTableRepository;

    @Value("${azure.storage.blob.connection-string:UseDevelopmentStorage=true}")
    private String blobConnectionString;

    @Value("${azure.storage.blob.sink-container:table-sink-blob}")
    private String sinkContainer;

    @Value("${spark.query.endpoint:}")
    private String sparkQueryEndpoint;

    private BlobContainerClient blobContainerClient;
    private final RestClient restClient = RestClient.create();

    public DataSourceStatsService(QryParsedTableRepository parsedTableRepository) {
        this.parsedTableRepository = parsedTableRepository;
    }

    @PostConstruct
    void init() {
        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();
            blobContainerClient = serviceClient.getBlobContainerClient(sinkContainer);
        } catch (Exception e) {
            log.warn("[DataSourceStats] Could not connect to Blob Storage: {}", e.getMessage());
        }
    }

    /**
     * Returns stats for all three backends for the given org.
     *
     * @param orgId organisation UUID string
     * @return {@link DataSourceStats} record (never null)
     */
    @Transactional(readOnly = true)
    public DataSourceStats getStats(String orgId) {
        long postgresCount = countPostgres(orgId);
        BlobStats blobStats = countBlob(orgId);
        SparkAvailability sparkAvailability = probeSparkApi();

        return new DataSourceStats(
                postgresCount,
                sparkAvailability.available(),
                sparkAvailability.recordCount(),
                blobStats.available(),
                blobStats.blobCount());
    }

    // --- private helpers ---

    private long countPostgres(String orgId) {
        try {
            return parsedTableRepository.countByOrgIdAndStorageBackend(orgId, "POSTGRES")
                    + parsedTableRepository.countByOrgIdAndStorageBackend(orgId, "SPARK")
                    + parsedTableRepository.countByOrgIdAndStorageBackend(orgId, "BLOB");
            // All rows in parsed_tables regardless of backend (SPARK/BLOB rows are not written there,
            // so only POSTGRES rows exist – the sum always equals countByOrgId effectively)
        } catch (Exception e) {
            log.warn("[DataSourceStats] Error counting Postgres rows: {}", e.getMessage());
            return -1;
        }
    }

    private BlobStats countBlob(String orgId) {
        if (blobContainerClient == null) return new BlobStats(false, 0);
        try {
            if (!blobContainerClient.exists()) return new BlobStats(true, 0);
            String prefix = orgId + "/";
            long count = 0;
            for (var ignored : blobContainerClient.listBlobsByHierarchy(prefix)) {
                count++;
            }
            return new BlobStats(true, count);
        } catch (Exception e) {
            log.warn("[DataSourceStats] Error counting Blob blobs for org {}: {}", orgId, e.getMessage());
            return new BlobStats(false, 0);
        }
    }

    private SparkAvailability probeSparkApi() {
        if (sparkQueryEndpoint == null || sparkQueryEndpoint.isBlank()) {
            return new SparkAvailability(false, 0);
        }
        try {
            // Lightweight health probe – just check the endpoint is reachable
            URI healthUri = URI.create(sparkQueryEndpoint.stripTrailing() + "/health");
            restClient.get().uri(healthUri).retrieve().toBodilessEntity();
            return new SparkAvailability(true, -1); // count unknown (external)
        } catch (Exception e) {
            log.debug("[DataSourceStats] Spark API probe failed: {}", e.getMessage());
            return new SparkAvailability(false, 0);
        }
    }

    // --- Result types ---

    public record DataSourceStats(
            long postgresRowCount,
            boolean sparkAvailable,
            long sparkRecordCount,   // -1 = external / unknown
            boolean blobAvailable,
            long blobCount) {
    }

    private record BlobStats(boolean available, long blobCount) {}
    private record SparkAvailability(boolean available, long recordCount) {}
}
