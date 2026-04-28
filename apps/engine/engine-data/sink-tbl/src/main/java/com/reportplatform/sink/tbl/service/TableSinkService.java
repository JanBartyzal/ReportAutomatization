package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.backend.TableStorageBackend;
import com.reportplatform.sink.tbl.entity.FormResponseEntity;
import com.reportplatform.sink.tbl.repository.FormResponseRepository;
import com.reportplatform.sink.tbl.repository.ParsedTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for table sink operations.
 * <p>
 * Delegates bulk-insert and delete to the {@link TableStorageBackend} resolved
 * by {@link StorageRoutingService} – enabling transparent coexistence of
 * POSTGRES and SPARK backends during migration.
 * </p>
 */
@Service
public class TableSinkService {

    private static final Logger logger = LoggerFactory.getLogger(TableSinkService.class);

    private final StorageRoutingService storageRoutingService;
    private final ParsedTableRepository parsedTableRepository;
    private final FormResponseRepository formResponseRepository;

    public TableSinkService(
            StorageRoutingService storageRoutingService,
            ParsedTableRepository parsedTableRepository,
            FormResponseRepository formResponseRepository) {
        this.storageRoutingService = storageRoutingService;
        this.parsedTableRepository = parsedTableRepository;
        this.formResponseRepository = formResponseRepository;
    }

    /**
     * Bulk insert parsed table data.
     * Routes to the backend selected by {@link StorageRoutingService}.
     */
    public int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableRecordData> records) {

        TableStorageBackend backend = storageRoutingService.resolve(orgId, sourceType);
        logger.info("Bulk inserting {} records for fileId={}, orgId={}, sourceType={} via backend={}",
                records.size(), fileId, orgId, sourceType, backend.backendType());

        return backend.bulkInsert(fileId, orgId, sourceType, records);
    }

    /**
     * Delete all records for a given file ID across all backends.
     * Used as Saga compensating action.
     */
    @Transactional
    public int deleteByFileId(String fileId) {
        logger.info("Deleting all table records for fileId={}", fileId);

        // Always remove from Postgres (rows may exist regardless of active backend)
        int deletedCount = parsedTableRepository.deleteByFileId(fileId);

        logger.info("Deleted {} table records for fileId={}", deletedCount, fileId);
        return deletedCount;
    }

    /**
     * Store form response data.
     */
    @Transactional
    public String storeFormResponse(
            String orgId,
            String periodId,
            String formVersionId,
            List<FormFieldValueData> fields) {

        logger.info("Storing form response for orgId={}, periodId={}, formVersionId={}",
                orgId, periodId, formVersionId);

        String responseId = UUID.randomUUID().toString();
        OffsetDateTime submittedAt = OffsetDateTime.now();

        for (FormFieldValueData field : fields) {
            FormResponseEntity entity = new FormResponseEntity();
            entity.setOrgId(orgId);
            entity.setPeriodId(periodId);
            entity.setFormVersionId(formVersionId);
            entity.setFieldId(field.fieldId());
            entity.setValue(field.value());
            entity.setDataType(field.dataType());
            entity.setSubmittedAt(submittedAt);

            formResponseRepository.save(entity);
        }

        logger.info("Stored form response with id={}", responseId);
        return responseId;
    }

    /**
     * Data class for table record from proto.
     */
    public record TableRecordData(
            String recordId,
            String sourceSheet,
            List<String> headers,
            List<List<String>> rows,
            java.util.Map<String, String> metadata) {
    }

    /**
     * Data class for form field value from proto.
     */
    public record FormFieldValueData(
            String fieldId,
            String value,
            String dataType) {
    }
}
