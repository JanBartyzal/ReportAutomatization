package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingHistoryEntity;
import com.reportplatform.template.tmpl.repository.MappingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for learning from successful mappings and suggesting based on history.
 */
@Service
public class MappingHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(MappingHistoryService.class);

    private final MappingHistoryRepository historyRepository;

    public MappingHistoryService(MappingHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Record a successful mapping. Upserts by (org_id, source_column, target_column).
     * If the mapping already exists, increments the used_count.
     */
    @Transactional
    public void recordSuccess(String orgId, String sourceColumn, String targetColumn,
                              String ruleType, double confidence, String fileId) {
        Optional<MappingHistoryEntity> existing = historyRepository
                .findByOrgIdAndSourceColumnAndTargetColumn(orgId, sourceColumn, targetColumn);

        if (existing.isPresent()) {
            MappingHistoryEntity entity = existing.get();
            entity.setUsedCount(entity.getUsedCount() + 1);
            entity.setLastUsedAt(OffsetDateTime.now());
            entity.setConfidence(Math.max(entity.getConfidence(), confidence));
            historyRepository.save(entity);
            logger.debug("Updated mapping history: {}→{} (count={})", sourceColumn, targetColumn, entity.getUsedCount());
        } else {
            MappingHistoryEntity entity = new MappingHistoryEntity();
            entity.setOrgId(orgId);
            entity.setSourceColumn(sourceColumn);
            entity.setTargetColumn(targetColumn);
            entity.setRuleType(ruleType);
            entity.setConfidence(confidence);
            entity.setFileId(fileId);
            historyRepository.save(entity);
            logger.debug("Created mapping history: {}→{}", sourceColumn, targetColumn);
        }
    }

    /**
     * Suggest mappings based on historical usage for the given source headers.
     * Returns the most frequently used mapping for each header, ranked by used_count.
     */
    public List<MappingRuleEngine.MappingActionData> suggestFromHistory(String orgId, List<String> sourceHeaders) {
        List<MappingRuleEngine.MappingActionData> suggestions = new ArrayList<>();

        for (String header : sourceHeaders) {
            List<MappingHistoryEntity> history = historyRepository
                    .findByOrgIdAndSourceColumnIgnoreCaseOrderByUsedCountDesc(orgId, header);

            if (!history.isEmpty()) {
                MappingHistoryEntity best = history.getFirst();
                suggestions.add(new MappingRuleEngine.MappingActionData(
                        header,
                        best.getTargetColumn(),
                        "HISTORY",
                        best.getConfidence()));
            }
        }

        return suggestions;
    }
}
