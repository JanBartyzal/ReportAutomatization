package com.reportplatform.sink.tbl.backend;

import com.reportplatform.sink.tbl.service.TableSinkService;
import io.dapr.client.DaprClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark/Delta-Lake stub implementation of {@link TableStorageBackend}.
 * <p>
 * Instead of writing data directly, this backend publishes a
 * {@code spark-ingest-requested} Dapr Pub/Sub event that carries the
 * file reference and source metadata.  An external Spark / Azure Data
 * Factory pipeline subscribes to that topic and reads the raw file from
 * Azure Blob Storage, converting it into Parquet/Delta format on
 * ADLS Gen2.
 * </p>
 * <p>
 * This backend intentionally contains <em>no</em> JPA/SQL logic – all
 * persistence is delegated to the external Spark tooling.  The
 * {@code deleteByFileId} method publishes a corresponding
 * {@code spark-delete-requested} event for Saga compensation.
 * </p>
 */
@Component
public class SparkTableStorageBackend implements TableStorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(SparkTableStorageBackend.class);
    public static final String BACKEND_TYPE = "SPARK";

    private static final String PUBSUB_NAME = "reportplatform-pubsub";
    private static final String TOPIC_INGEST = "spark-ingest-requested";
    private static final String TOPIC_DELETE = "spark-delete-requested";

    private final DaprClient daprClient;

    public SparkTableStorageBackend(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Override
    public String backendType() {
        return BACKEND_TYPE;
    }

    /**
     * Publishes a {@code spark-ingest-requested} Pub/Sub event carrying file
     * reference and source metadata. The external Spark/ADF pipeline picks up
     * the event and performs the actual Delta-Lake write asynchronously.
     *
     * @return {@code records.size()} — number of records <em>submitted</em> for
     *         async ingestion. Actual persistence is confirmed only after the
     *         Spark job completes. See {@link TableStorageBackend#bulkInsert}
     *         contract for details.
     */
    @Override
    public int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableSinkService.TableRecordData> records) {

        logger.info("[SPARK] Publishing spark-ingest-requested for fileId={}, orgId={}, sourceType={}, records={}",
                fileId, orgId, sourceType, records.size());

        Map<String, Object> event = new HashMap<>();
        event.put("fileId", fileId);
        event.put("orgId", orgId);
        event.put("sourceType", sourceType);
        event.put("recordCount", records.size());
        event.put("requestedAt", Instant.now().toString());

        // Include sheet names so the Spark job knows which sheets to process
        List<String> sheets = records.stream()
                .map(TableSinkService.TableRecordData::sourceSheet)
                .distinct()
                .toList();
        event.put("sheets", sheets);

        try {
            daprClient.publishEvent(PUBSUB_NAME, TOPIC_INGEST, event).block();
            logger.info("[SPARK] Published {} for fileId={}", TOPIC_INGEST, fileId);
        } catch (Exception e) {
            logger.error("[SPARK] Failed to publish {}: {}", TOPIC_INGEST, e.getMessage(), e);
            throw new RuntimeException("Spark ingest event publish failed for fileId=" + fileId, e);
        }

        return records.size();
    }

    /**
     * Publishes a {@code spark-delete-requested} event – Saga compensation.
     *
     * @return 0 (deletion is asynchronous; the Spark pipeline handles it)
     */
    @Override
    public int deleteByFileId(String fileId) {
        logger.info("[SPARK] Publishing spark-delete-requested for fileId={}", fileId);

        Map<String, Object> event = new HashMap<>();
        event.put("fileId", fileId);
        event.put("requestedAt", Instant.now().toString());

        try {
            daprClient.publishEvent(PUBSUB_NAME, TOPIC_DELETE, event).block();
            logger.info("[SPARK] Published {} for fileId={}", TOPIC_DELETE, fileId);
        } catch (Exception e) {
            logger.error("[SPARK] Failed to publish {}: {}", TOPIC_DELETE, e.getMessage(), e);
        }

        return 0;
    }
}
