package com.reportplatform.admin.service;

import com.reportplatform.admin.model.entity.PromotionCandidateEntity;
import com.reportplatform.admin.model.entity.PromotionCandidateEntity.PromotionStatus;
import com.reportplatform.admin.repository.PromotionCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Scheduled service that detects mapping templates eligible for promotion
 * from JSONB storage to dedicated database tables.
 *
 * Runs hourly, queries ms-tmpl for high-usage mappings, and creates
 * promotion candidates with auto-generated DDL proposals.
 */
@Service
public class PromotionDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionDetectionService.class);

    /** Active statuses that prevent duplicate candidate creation */
    private static final Set<PromotionStatus> ACTIVE_STATUSES = EnumSet.of(
            PromotionStatus.CANDIDATE,
            PromotionStatus.PENDING_REVIEW,
            PromotionStatus.APPROVED,
            PromotionStatus.CREATED,
            PromotionStatus.MIGRATING,
            PromotionStatus.ACTIVE
    );

    private final PromotionCandidateRepository candidateRepository;
    private final SchemaProposalGenerator schemaProposalGenerator;

    @Value("${smart-persistence.promotion-threshold:5}")
    private long promotionThreshold;

    public PromotionDetectionService(
            PromotionCandidateRepository candidateRepository,
            SchemaProposalGenerator schemaProposalGenerator) {
        this.candidateRepository = candidateRepository;
        this.schemaProposalGenerator = schemaProposalGenerator;
    }

    /**
     * Hourly scheduled job that detects new promotion candidates.
     *
     * Process:
     * 1. Query ms-tmpl via Dapr for mappings with usage >= threshold
     * 2. Filter out mappings that already have an active candidate
     * 3. Generate DDL proposal for each new candidate
     * 4. Persist as CANDIDATE status
     * 5. Publish event via Dapr Pub/Sub
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void detectCandidates() {
        logger.info("Starting promotion candidate detection (threshold={})", promotionThreshold);

        // TODO: Replace with actual Dapr service invocation to ms-tmpl
        // GET /api/v1/usage/high-usage?threshold={promotionThreshold}
        // Returns list of {mappingTemplateId, mappingName, usageCount}
        List<HighUsageMapping> highUsageMappings = fetchHighUsageMappingsFromTmpl();

        int created = 0;
        int skipped = 0;

        for (HighUsageMapping mapping : highUsageMappings) {
            // Check if already a candidate in any active status
            boolean exists = candidateRepository.existsByMappingTemplateIdAndStatusIn(
                    mapping.mappingTemplateId(), ACTIVE_STATUSES);

            if (exists) {
                skipped++;
                continue;
            }

            // Generate DDL proposal
            SchemaProposalGenerator.ProposalResult proposal = schemaProposalGenerator
                    .generateProposal(mapping.mappingTemplateId(), mapping.mappingName());

            // Create candidate entity
            PromotionCandidateEntity candidate = new PromotionCandidateEntity();
            candidate.setMappingTemplateId(mapping.mappingTemplateId());
            candidate.setStatus(PromotionStatus.CANDIDATE);
            candidate.setUsageCount(mapping.usageCount());
            candidate.setProposedTableName("promoted_" + deriveTableName(mapping.mappingName()));
            candidate.setProposedDdl(proposal.ddl());
            candidate.setProposedIndexes(proposal.indexes());
            candidate.setColumnTypeAnalysis(proposal.columnAnalysis());

            candidateRepository.save(candidate);
            created++;

            // TODO: Publish promotion.candidate.detected event via Dapr Pub/Sub
            // daprClient.publishEvent("pubsub", "promotion.candidate.detected",
            //     Map.of("candidateId", candidate.getId(),
            //            "mappingTemplateId", mapping.mappingTemplateId(),
            //            "usageCount", mapping.usageCount()));

            logger.info("Created promotion candidate for template={} table={} usage={}",
                    mapping.mappingTemplateId(), candidate.getProposedTableName(), mapping.usageCount());
        }

        logger.info("Promotion detection complete: created={} skipped={} total={}",
                created, skipped, highUsageMappings.size());
    }

    /**
     * Fetch high-usage mappings from ms-tmpl.
     * TODO: Replace with actual Dapr service invocation.
     */
    private List<HighUsageMapping> fetchHighUsageMappingsFromTmpl() {
        // TODO: Implement Dapr service invocation to ms-tmpl
        // Example:
        // String url = "http://ms-tmpl/api/v1/usage/high-usage?threshold=" + promotionThreshold;
        // Response from Dapr sidecar would contain list of high-usage mapping records
        logger.debug("TODO: Fetch high-usage mappings from ms-tmpl via Dapr (threshold={})", promotionThreshold);
        return Collections.emptyList();
    }

    private String deriveTableName(String mappingName) {
        return mappingName.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Represents a high-usage mapping template returned from ms-tmpl.
     */
    public record HighUsageMapping(UUID mappingTemplateId, String mappingName, long usageCount) {}
}
