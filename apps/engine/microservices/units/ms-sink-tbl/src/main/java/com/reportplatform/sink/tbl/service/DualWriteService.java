package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.entity.PromotedTableRegistryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for dual-write logic during the promotion transition period.
 *
 * When a mapping template has been promoted to a dedicated table, this service
 * decides where to route writes:
 * <ul>
 *   <li>No promoted table: write to JSONB store only (existing behavior)</li>
 *   <li>Promoted table within dual_write_until: write to BOTH JSONB and promoted table</li>
 *   <li>Promoted table past dual_write_until: write to promoted table ONLY</li>
 * </ul>
 */
@Service
public class DualWriteService {

    private static final Logger logger = LoggerFactory.getLogger(DualWriteService.class);

    private final DynamicTableService dynamicTableService;
    private final TableSinkService tableSinkService;

    public DualWriteService(
            DynamicTableService dynamicTableService,
            TableSinkService tableSinkService) {
        this.dynamicTableService = dynamicTableService;
        this.tableSinkService = tableSinkService;
    }

    /**
     * Write records with dual-write awareness.
     * Routes data to the appropriate store(s) based on the mapping template's
     * promotion status and dual-write window.
     *
     * @param mappingTemplateId the mapping template ID
     * @param fileId            the file ID being processed
     * @param orgId             the organization ID
     * @param records           the data records to write
     * @return the number of records successfully written
     */
    @Transactional
    public int write(
            UUID mappingTemplateId,
            String fileId,
            String orgId,
            List<Map<String, Object>> records) {

        logger.info("DualWriteService.write: mappingTemplateId={}, fileId={}, orgId={}, recordCount={}",
                mappingTemplateId, fileId, orgId, records.size());

        Optional<PromotedTableRegistryEntity> promotedOpt = dynamicTableService.isPromotedTable(mappingTemplateId);

        if (promotedOpt.isEmpty()) {
            // No promoted table - write to JSONB store only (existing behavior)
            logger.info("No promoted table for mapping template {} - writing to JSONB store only",
                    mappingTemplateId);
            return writeToJsonbStore(fileId, orgId, records);
        }

        PromotedTableRegistryEntity promoted = promotedOpt.get();
        boolean withinDualWriteWindow = promoted.getDualWriteUntil() != null
                && OffsetDateTime.now().isBefore(promoted.getDualWriteUntil());

        if (withinDualWriteWindow) {
            // Within dual-write window - write to BOTH stores
            logger.info("Within dual-write window for table '{}' (until {}) - writing to BOTH stores",
                    promoted.getTableName(), promoted.getDualWriteUntil());

            int jsonbCount = writeToJsonbStore(fileId, orgId, records);
            int promotedCount = writeToPromotedTable(promoted.getTableName(), records);

            logger.info("Dual-write complete: JSONB={}, promoted={} records", jsonbCount, promotedCount);
            return jsonbCount;
        } else {
            // Past dual-write window - write to promoted table ONLY
            logger.info("Past dual-write window for table '{}' - writing to promoted table only",
                    promoted.getTableName());

            int promotedCount = writeToPromotedTable(promoted.getTableName(), records);
            logger.info("Promoted-only write complete: {} records", promotedCount);
            return promotedCount;
        }
    }

    /**
     * Write records to the existing JSONB store via TableSinkService.
     */
    private int writeToJsonbStore(String fileId, String orgId, List<Map<String, Object>> records) {
        logger.debug("Writing {} records to JSONB store for fileId={}", records.size(), fileId);

        List<TableSinkService.TableRecordData> tableRecords = records.stream()
                .map(record -> new TableSinkService.TableRecordData(
                        UUID.randomUUID().toString(),
                        (String) record.getOrDefault("source_sheet", "default"),
                        record.containsKey("headers") ? (List<String>) record.get("headers") : List.of(),
                        record.containsKey("rows") ? (List<List<String>>) record.get("rows") : List.of(),
                        Map.of()))
                .toList();

        return tableSinkService.bulkInsert(fileId, orgId, "PROMOTED_DUAL_WRITE", tableRecords);
    }

    /**
     * Write records to the promoted dedicated table.
     */
    private int writeToPromotedTable(String tableName, List<Map<String, Object>> records) {
        logger.debug("Writing {} records to promoted table '{}'", records.size(), tableName);

        int successCount = 0;
        for (Map<String, Object> record : records) {
            boolean success = dynamicTableService.insertToPromotedTable(tableName, record);
            if (success) {
                successCount++;
            }
        }

        if (successCount < records.size()) {
            logger.warn("Only {} of {} records written to promoted table '{}'",
                    successCount, records.size(), tableName);
        }

        return successCount;
    }
}
