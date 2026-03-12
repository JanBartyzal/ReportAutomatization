package com.reportplatform.sink.tbl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.tbl.entity.FormResponseEntity;
import com.reportplatform.sink.tbl.entity.ParsedTableEntity;
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
 * Handles bulk inserts, form response storage, and deletion for Saga
 * compensation.
 */
@Service
public class TableSinkService {

    private static final Logger logger = LoggerFactory.getLogger(TableSinkService.class);

    private final ParsedTableRepository parsedTableRepository;
    private final FormResponseRepository formResponseRepository;
    private final ObjectMapper objectMapper;

    public TableSinkService(
            ParsedTableRepository parsedTableRepository,
            FormResponseRepository formResponseRepository,
            ObjectMapper objectMapper) {
        this.parsedTableRepository = parsedTableRepository;
        this.formResponseRepository = formResponseRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Bulk insert parsed table data.
     * Optimized for batch inserts with JDBC batching.
     */
    @Transactional
    public int bulkInsert(
            String fileId,
            String orgId,
            String sourceType,
            List<TableRecordData> records) {

        logger.info("Bulk inserting {} table records for fileId={}, orgId={}, sourceType={}",
                records.size(), fileId, orgId, sourceType);

        int insertedCount = 0;

        for (TableRecordData record : records) {
            try {
                ParsedTableEntity entity = new ParsedTableEntity();
                entity.setFileId(fileId);
                entity.setOrgId(orgId);
                entity.setSourceSheet(record.sourceSheet());
                entity.setHeaders(objectMapper.writeValueAsString(record.headers()));
                entity.setRows(objectMapper.writeValueAsString(record.rows()));
                entity.setMetadata(objectMapper.writeValueAsString(record.metadata()));
                entity.setCreatedAt(OffsetDateTime.now());

                parsedTableRepository.save(entity);
                insertedCount++;
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize table record: {}", e.getMessage());
                throw new RuntimeException("Failed to serialize table data", e);
            }
        }

        logger.info("Successfully inserted {} table records", insertedCount);
        return insertedCount;
    }

    /**
     * Delete all records for a given file ID.
     * Used as Saga compensating action.
     */
    @Transactional
    public int deleteByFileId(String fileId) {
        logger.info("Deleting all table records for fileId={}", fileId);

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
