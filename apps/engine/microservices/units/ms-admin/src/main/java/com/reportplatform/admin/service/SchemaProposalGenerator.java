package com.reportplatform.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes JSONB data samples and generates DDL proposals for dedicated tables.
 * Used during the smart persistence promotion process to automatically propose
 * table schemas based on observed data patterns.
 */
@Service
public class SchemaProposalGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SchemaProposalGenerator.class);

    private static final int MAX_VARCHAR_LENGTH = 4000;
    private static final double VARCHAR_LENGTH_MULTIPLIER = 1.5;

    /**
     * Generate a full DDL proposal for promoting a mapping template to a dedicated table.
     *
     * @param mappingTemplateId the mapping template being promoted
     * @param mappingName       human-readable name used to derive the table name
     * @return proposal result containing DDL, indexes, and column analysis
     */
    public ProposalResult generateProposal(UUID mappingTemplateId, String mappingName) {
        // Derive a safe table name from the mapping name
        String tableName = deriveTableName(mappingName);

        // TODO: Fetch sample data from ms-tmpl via Dapr service invocation
        // For now, return an empty-column proposal as a placeholder
        List<Map<String, Object>> sampleData = Collections.emptyList();

        Map<String, ColumnTypeInfo> columns = analyzeColumnTypes(sampleData);
        String ddl = generateDDL(tableName, columns);
        String indexes = generateIndexes(tableName, columns);

        // Serialize column analysis to JSON-like string for storage
        String columnAnalysis = serializeColumnAnalysis(columns);

        logger.info("Generated DDL proposal for template={} table={} columns={}",
                mappingTemplateId, tableName, columns.size());

        return new ProposalResult(ddl, indexes, columnAnalysis);
    }

    /**
     * Analyze sample data to determine PostgreSQL column types for each key.
     *
     * @param sampleData list of row maps from JSONB data samples
     * @return map of column name to detected type info
     */
    public Map<String, ColumnTypeInfo> analyzeColumnTypes(List<Map<String, Object>> sampleData) {
        if (sampleData == null || sampleData.isEmpty()) {
            return Collections.emptyMap();
        }

        // Collect all keys and their values across samples
        Map<String, List<Object>> columnValues = new LinkedHashMap<>();
        for (Map<String, Object> row : sampleData) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                columnValues.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        Map<String, ColumnTypeInfo> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Object>> entry : columnValues.entrySet()) {
            String columnName = entry.getKey();
            List<Object> values = entry.getValue();
            result.put(columnName, inferColumnType(values, sampleData.size()));
        }

        return result;
    }

    /**
     * Generate CREATE TABLE DDL for the promoted table.
     *
     * @param tableName the target table name (will be prefixed with "promoted_")
     * @param columns   analyzed column types
     * @return DDL string
     */
    public String generateDDL(String tableName, Map<String, ColumnTypeInfo> columns) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE promoted_").append(tableName).append(" (\n");
        ddl.append("    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n");
        ddl.append("    org_id UUID NOT NULL,\n");

        for (Map.Entry<String, ColumnTypeInfo> entry : columns.entrySet()) {
            String colName = sanitizeColumnName(entry.getKey());
            ColumnTypeInfo typeInfo = entry.getValue();
            ddl.append("    ").append(colName).append(" ").append(typeInfo.pgType());
            if (!typeInfo.nullable()) {
                ddl.append(" NOT NULL");
            }
            ddl.append(",\n");
        }

        ddl.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),\n");
        ddl.append("    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()\n");
        ddl.append(");\n\n");

        // RLS policy
        ddl.append("ALTER TABLE promoted_").append(tableName).append(" ENABLE ROW LEVEL SECURITY;\n");
        ddl.append("CREATE POLICY org_isolation ON promoted_").append(tableName).append("\n");
        ddl.append("    USING (org_id = current_setting('app.current_org_id')::UUID);\n");

        return ddl.toString();
    }

    private String generateIndexes(String tableName, Map<String, ColumnTypeInfo> columns) {
        StringBuilder indexes = new StringBuilder();
        String fullTableName = "promoted_" + tableName;

        // Always index org_id
        indexes.append("CREATE INDEX idx_").append(tableName).append("_org ON ")
                .append(fullTableName).append("(org_id);\n");

        // Index columns that look like foreign keys or identifiers
        for (Map.Entry<String, ColumnTypeInfo> entry : columns.entrySet()) {
            String colName = sanitizeColumnName(entry.getKey());
            if (colName.endsWith("_id") || colName.equals("code") || colName.equals("status")) {
                indexes.append("CREATE INDEX idx_").append(tableName).append("_").append(colName)
                        .append(" ON ").append(fullTableName).append("(").append(colName).append(");\n");
            }
        }

        return indexes.toString();
    }

    private ColumnTypeInfo inferColumnType(List<Object> values, int totalRows) {
        boolean nullable = values.size() < totalRows || values.stream().anyMatch(Objects::isNull);
        List<Object> nonNullValues = values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (nonNullValues.isEmpty()) {
            return new ColumnTypeInfo("TEXT", 0, true);
        }

        // Check if all values are boolean
        if (nonNullValues.stream().allMatch(v -> isBoolean(v))) {
            return new ColumnTypeInfo("BOOLEAN", 0, nullable);
        }

        // Check if all values are numeric (integer)
        if (nonNullValues.stream().allMatch(v -> isInteger(v))) {
            return new ColumnTypeInfo("INTEGER", 0, nullable);
        }

        // Check if all values are numeric (decimal)
        if (nonNullValues.stream().allMatch(v -> isNumeric(v))) {
            return new ColumnTypeInfo("NUMERIC", 0, nullable);
        }

        // Check if all values are ISO 8601 timestamps
        if (nonNullValues.stream().allMatch(v -> isTimestamp(v))) {
            return new ColumnTypeInfo("TIMESTAMP WITH TIME ZONE", 0, nullable);
        }

        // Default: VARCHAR with length based on observed max
        int maxLength = nonNullValues.stream()
                .map(Object::toString)
                .mapToInt(String::length)
                .max()
                .orElse(255);

        int proposedLength = Math.min((int) (maxLength * VARCHAR_LENGTH_MULTIPLIER), MAX_VARCHAR_LENGTH);
        return new ColumnTypeInfo("VARCHAR(" + proposedLength + ")", proposedLength, nullable);
    }

    private boolean isBoolean(Object value) {
        if (value instanceof Boolean) {
            return true;
        }
        String str = value.toString().toLowerCase().trim();
        return "true".equals(str) || "false".equals(str);
    }

    private boolean isInteger(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return true;
        }
        try {
            Long.parseLong(value.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(value.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isTimestamp(Object value) {
        String str = value.toString().trim();
        try {
            OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            // Try ISO_DATE_TIME (without offset)
            try {
                OffsetDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
                return true;
            } catch (DateTimeParseException e2) {
                return false;
            }
        }
    }

    private String deriveTableName(String mappingName) {
        return mappingName.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String sanitizeColumnName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String serializeColumnAnalysis(Map<String, ColumnTypeInfo> columns) {
        if (columns.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ColumnTypeInfo> entry : columns.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            ColumnTypeInfo info = entry.getValue();
            sb.append("\"").append(entry.getKey()).append("\":{")
                    .append("\"pgType\":\"").append(info.pgType()).append("\",")
                    .append("\"maxLength\":").append(info.maxLength()).append(",")
                    .append("\"nullable\":").append(info.nullable())
                    .append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Result of a DDL proposal generation.
     */
    public record ProposalResult(String ddl, String indexes, String columnAnalysis) {}

    /**
     * Detected column type information.
     */
    public record ColumnTypeInfo(String pgType, int maxLength, boolean nullable) {}
}
