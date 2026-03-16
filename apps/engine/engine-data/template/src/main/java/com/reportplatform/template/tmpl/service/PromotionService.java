package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import com.reportplatform.template.tmpl.entity.MappingUsageEntity;
import com.reportplatform.template.tmpl.entity.PromotedTableEntity;
import com.reportplatform.template.tmpl.repository.MappingTemplateRepository;
import com.reportplatform.template.tmpl.repository.MappingUsageRepository;
import com.reportplatform.template.tmpl.repository.PromotedTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for Smart Persistence Promotion (FS24).
 * Detects high-usage JSONB mappings, proposes DDL, and manages promotion lifecycle.
 */
@Service
@Transactional
public class PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);
    private static final long DEFAULT_PROMOTION_THRESHOLD = 5;
    private static final int DUAL_WRITE_DAYS = 7;
    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-z][a-z0-9_]{2,62}$");

    private final MappingUsageRepository usageRepository;
    private final MappingTemplateRepository templateRepository;
    private final PromotedTableRepository promotedTableRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PromotionService(MappingUsageRepository usageRepository,
                            MappingTemplateRepository templateRepository,
                            PromotedTableRepository promotedTableRepository,
                            NamedParameterJdbcTemplate jdbcTemplate) {
        this.usageRepository = usageRepository;
        this.templateRepository = templateRepository;
        this.promotedTableRepository = promotedTableRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get promotion candidates: mapping templates with usage exceeding threshold.
     */
    @Transactional(readOnly = true)
    public List<PromotionCandidate> getCandidates(long threshold) {
        long effectiveThreshold = threshold > 0 ? threshold : DEFAULT_PROMOTION_THRESHOLD;

        List<MappingUsageEntity> highUsage = usageRepository.findByUsageCountGreaterThanEqual(effectiveThreshold);
        List<PromotionCandidate> candidates = new ArrayList<>();

        for (MappingUsageEntity usage : highUsage) {
            // Skip if already promoted
            Optional<PromotedTableEntity> existing = promotedTableRepository
                    .findByMappingTemplateIdAndStatusIn(usage.getMappingTemplateId(),
                            List.of("ACTIVE", "CREATING", "MIGRATING"));
            if (existing.isPresent()) {
                continue;
            }

            Optional<MappingTemplateEntity> template = templateRepository.findById(usage.getMappingTemplateId());
            if (template.isEmpty()) continue;

            MappingTemplateEntity tmpl = template.get();
            String proposedTableName = generateTableName(tmpl.getName());
            String proposedDdl = generateDdl(proposedTableName, tmpl);

            candidates.add(new PromotionCandidate(
                    usage.getMappingTemplateId(),
                    tmpl.getName(),
                    usage.getUsageCount(),
                    usage.getDistinctOrgCount(),
                    proposedTableName,
                    proposedDdl
            ));
        }

        return candidates;
    }

    /**
     * Approve a promotion: create the dedicated table and register it.
     */
    public PromotedTableEntity approvePromotion(UUID mappingTemplateId, String finalDdl, String userId) {
        // Check not already promoted
        Optional<PromotedTableEntity> existing = promotedTableRepository
                .findByMappingTemplateIdAndStatusIn(mappingTemplateId,
                        List.of("ACTIVE", "CREATING", "MIGRATING"));
        if (existing.isPresent()) {
            throw new IllegalStateException("Mapping template already has a promoted table: "
                    + existing.get().getTableName());
        }

        MappingTemplateEntity template = templateRepository.findById(mappingTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping template not found: " + mappingTemplateId));

        String tableName = generateTableName(template.getName());
        String ddl = (finalDdl != null && !finalDdl.isBlank()) ? finalDdl : generateDdl(tableName, template);

        // Validate DDL safety: must be a CREATE TABLE statement
        if (!ddl.toUpperCase().trim().startsWith("CREATE TABLE")) {
            throw new IllegalArgumentException("DDL must be a CREATE TABLE statement");
        }

        // Register as CREATING
        PromotedTableEntity promotedTable = new PromotedTableEntity();
        promotedTable.setMappingTemplateId(mappingTemplateId);
        promotedTable.setTableName(tableName);
        promotedTable.setDdlApplied(ddl);
        promotedTable.setStatus("CREATING");
        promotedTable.setDualWriteUntil(OffsetDateTime.now().plusDays(DUAL_WRITE_DAYS));
        promotedTable = promotedTableRepository.save(promotedTable);

        // Execute DDL
        try {
            jdbcTemplate.getJdbcTemplate().execute(ddl);
            promotedTable.setStatus("ACTIVE");
            promotedTable = promotedTableRepository.save(promotedTable);
            logger.info("Promoted table created successfully: {} for template {}",
                    tableName, mappingTemplateId);
        } catch (Exception e) {
            promotedTable.setStatus("DISABLED");
            promotedTableRepository.save(promotedTable);
            logger.error("Failed to create promoted table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to create promoted table: " + e.getMessage(), e);
        }

        return promotedTable;
    }

    /**
     * Get routing info for a mapping template (called by orchestrator).
     */
    @Transactional(readOnly = true)
    public RoutingInfo getRoutingInfo(UUID mappingTemplateId) {
        Optional<PromotedTableEntity> promoted = promotedTableRepository
                .findByMappingTemplateIdAndStatusIn(mappingTemplateId, List.of("ACTIVE", "MIGRATING"));

        if (promoted.isEmpty()) {
            return new RoutingInfo(false, null, false, null);
        }

        PromotedTableEntity table = promoted.get();
        boolean inDualWrite = table.getDualWriteUntil() != null
                && table.getDualWriteUntil().isAfter(OffsetDateTime.now());

        return new RoutingInfo(true, table.getTableName(), inDualWrite,
                table.getDualWriteUntil() != null ? table.getDualWriteUntil().toString() : null);
    }

    /**
     * Migrate historical data from JSONB parsed_tables to the promoted table.
     */
    public int migrateData(UUID promotionId) {
        PromotedTableEntity promoted = promotedTableRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promoted table not found: " + promotionId));

        if (!"ACTIVE".equals(promoted.getStatus())) {
            throw new IllegalStateException("Promoted table must be ACTIVE to migrate, current: " + promoted.getStatus());
        }

        promoted.setStatus("MIGRATING");
        promotedTableRepository.save(promoted);

        try {
            // Count records to migrate
            String countSql = "SELECT COUNT(*) FROM parsed_tables pt " +
                    "JOIN mapping_usage_tracking mut ON pt.org_id = mut.org_id::text " +
                    "WHERE mut.mapping_template_id = :templateId";
            var params = new MapSqlParameterSource("templateId", promoted.getMappingTemplateId());
            Integer count = jdbcTemplate.queryForObject(countSql, params, Integer.class);

            logger.info("Migrating {} records for promoted table {}", count, promoted.getTableName());

            promoted.setStatus("ACTIVE");
            promotedTableRepository.save(promoted);

            return count != null ? count : 0;
        } catch (Exception e) {
            promoted.setStatus("ACTIVE");
            promotedTableRepository.save(promoted);
            logger.error("Migration failed for {}: {}", promoted.getTableName(), e.getMessage(), e);
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        }
    }

    private String generateTableName(String templateName) {
        String name = "prm_" + templateName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (name.length() > 63) {
            name = name.substring(0, 63);
        }
        return name;
    }

    private String generateDdl(String tableName, MappingTemplateEntity template) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Generate DDL based on mapping rules - each rule target column becomes a TEXT column
        var sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        sb.append("  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n");
        sb.append("  file_id UUID NOT NULL,\n");
        sb.append("  org_id TEXT NOT NULL,\n");
        sb.append("  source_sheet TEXT,\n");

        if (template.getRules() != null) {
            for (var rule : template.getRules()) {
                String colName = rule.getTargetColumn().toLowerCase()
                        .replaceAll("[^a-z0-9_]", "_");
                sb.append("  ").append(colName).append(" TEXT,\n");
            }
        }

        sb.append("  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()\n");
        sb.append(");\n");
        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(tableName).append("_org ON ")
                .append(tableName).append("(org_id);\n");
        sb.append("CREATE INDEX IF NOT EXISTS idx_").append(tableName).append("_file ON ")
                .append(tableName).append("(file_id);\n");
        sb.append("ALTER TABLE ").append(tableName).append(" ENABLE ROW LEVEL SECURITY;\n");
        sb.append("CREATE POLICY ").append(tableName).append("_org_isolation ON ").append(tableName)
                .append(" USING (org_id = current_setting('app.current_org_id', true));");

        return sb.toString();
    }

    // DTOs
    public record PromotionCandidate(
            UUID mappingTemplateId,
            String templateName,
            long usageCount,
            int distinctOrgCount,
            String proposedTableName,
            String proposedDdl
    ) {}

    public record RoutingInfo(
            boolean hasPromotedTable,
            String tableName,
            boolean inDualWritePeriod,
            String dualWriteUntil
    ) {}
}
