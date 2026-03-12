package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.entity.PromotedTableRegistryEntity;
import com.reportplatform.sink.tbl.repository.PromotedTableRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Service for creating and managing promoted (dedicated) tables.
 * Handles DDL execution, registry tracking, and dynamic inserts
 * into promoted tables.
 */
@Service
public class DynamicTableService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicTableService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PromotedTableRegistryRepository registryRepository;

    public DynamicTableService(
            JdbcTemplate jdbcTemplate,
            PromotedTableRegistryRepository registryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.registryRepository = registryRepository;
    }

    /**
     * Create a promoted table by executing the provided DDL and registering it
     * in the promoted_tables_registry.
     *
     * @param ddl               the CREATE TABLE DDL statement to execute
     * @param mappingTemplateId the mapping template ID this table is promoted from
     * @param tableName         the name of the table to create
     * @param dualWriteDays     number of days to maintain dual-write mode
     * @return the created registry entity
     * @throws RuntimeException if DDL execution or registration fails
     */
    @Transactional
    public PromotedTableRegistryEntity createTable(
            String ddl,
            UUID mappingTemplateId,
            String tableName,
            int dualWriteDays) {

        logger.info("Creating promoted table '{}' for mapping template {}, dualWriteDays={}",
                tableName, mappingTemplateId, dualWriteDays);

        // Register with CREATING status first
        PromotedTableRegistryEntity registry = new PromotedTableRegistryEntity();
        registry.setMappingTemplateId(mappingTemplateId);
        registry.setTableName(tableName);
        registry.setDdlApplied(ddl);
        registry.setStatus("CREATING");
        registry.setDualWriteUntil(OffsetDateTime.now().plusDays(dualWriteDays));
        registryRepository.save(registry);

        try {
            // Execute the DDL
            jdbcTemplate.execute(ddl);
            logger.info("DDL executed successfully for table '{}'", tableName);

            // Update status to ACTIVE
            registry.setStatus("ACTIVE");
            registryRepository.save(registry);

            logger.info("Promoted table '{}' created and registered successfully", tableName);
            return registry;

        } catch (Exception e) {
            logger.error("Failed to create promoted table '{}': {}", tableName, e.getMessage(), e);
            registry.setStatus("DISABLED");
            registryRepository.save(registry);
            throw new RuntimeException("Failed to create promoted table: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a mapping template has a promoted table.
     *
     * @param mappingTemplateId the mapping template ID to check
     * @return the registry entity if a promoted table exists, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<PromotedTableRegistryEntity> isPromotedTable(UUID mappingTemplateId) {
        return registryRepository.findByMappingTemplateId(mappingTemplateId);
    }

    /**
     * Insert data into a promoted table dynamically.
     * Constructs an INSERT statement from the provided column-value map.
     *
     * @param tableName the name of the promoted table
     * @param data      map of column names to values
     * @return true if the insert was successful
     */
    @Transactional
    public boolean insertToPromotedTable(String tableName, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            logger.warn("No data to insert into promoted table '{}'", tableName);
            return false;
        }

        logger.debug("Inserting row into promoted table '{}': {} columns", tableName, data.size());

        try {
            StringJoiner columns = new StringJoiner(", ");
            StringJoiner placeholders = new StringJoiner(", ");
            Object[] values = new Object[data.size()];

            int index = 0;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                columns.add(entry.getKey());
                placeholders.add("?");
                values[index++] = entry.getValue();
            }

            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    tableName, columns, placeholders);

            jdbcTemplate.update(sql, values);

            logger.debug("Successfully inserted row into promoted table '{}'", tableName);
            return true;

        } catch (Exception e) {
            logger.error("Failed to insert into promoted table '{}': {}", tableName, e.getMessage(), e);
            return false;
        }
    }
}
