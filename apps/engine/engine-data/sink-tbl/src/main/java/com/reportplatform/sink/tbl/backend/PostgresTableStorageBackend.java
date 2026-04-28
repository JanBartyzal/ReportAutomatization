package com.reportplatform.sink.tbl.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.tbl.entity.ParsedTableEntity;
import com.reportplatform.sink.tbl.repository.ParsedTableRepository;
import com.reportplatform.sink.tbl.service.TableSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * PostgreSQL implementation of {@link TableStorageBackend}.
 * <p>
 * Persists structured table data as JSONB rows in the {@code parsed_tables}
 * table – the current default behaviour. Retains full Saga compensation support
 * via {@link #deleteByFileId}.
 * </p>
 */
@Component
public class PostgresTableStorageBackend implements TableStorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTableStorageBackend.class);
    public static final String BACKEND_TYPE = "POSTGRES";

    private final ParsedTableRepository parsedTableRepository;
    private final ObjectMapper objectMapper;

    public PostgresTableStorageBackend(
            ParsedTableRepository parsedTableRepository,
            ObjectMapper objectMapper) {
        this.parsedTableRepository = parsedTableRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String backendType() {
        return BACKEND_TYPE;
    }

    @Override
    @Transactional
    public int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableSinkService.TableRecordData> records) {

        logger.info("[POSTGRES] Bulk inserting {} records for fileId={}, orgId={}, sourceType={}",
                records.size(), fileId, orgId, sourceType);

        int insertedCount = 0;
        for (TableSinkService.TableRecordData record : records) {
            try {
                ParsedTableEntity entity = new ParsedTableEntity();
                entity.setFileId(fileId);
                entity.setOrgId(orgId);
                entity.setSourceSheet(record.sourceSheet());
                entity.setHeaders(objectMapper.writeValueAsString(record.headers()));
                entity.setRows(objectMapper.writeValueAsString(record.rows()));
                entity.setMetadata(objectMapper.writeValueAsString(record.metadata()));
                entity.setStorageBackend(BACKEND_TYPE);
                entity.setCreatedAt(OffsetDateTime.now());

                parsedTableRepository.save(entity);
                insertedCount++;
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize table record for fileId={}: {}", fileId, e.getMessage());
                throw new RuntimeException("Failed to serialize table data", e);
            }
        }

        logger.info("[POSTGRES] Successfully inserted {} table records for fileId={}", insertedCount, fileId);
        return insertedCount;
    }

    @Override
    @Transactional
    public int deleteByFileId(String fileId) {
        logger.info("[POSTGRES] Deleting all table records for fileId={}", fileId);
        int deleted = parsedTableRepository.deleteByFileId(fileId);
        logger.info("[POSTGRES] Deleted {} table records for fileId={}", deleted, fileId);
        return deleted;
    }
}
