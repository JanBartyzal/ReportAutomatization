package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingRuleEntity;
import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import com.reportplatform.template.tmpl.repository.MappingRuleRepository;
import com.reportplatform.template.tmpl.repository.MappingTemplateRepository;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main orchestrating service for template mapping operations.
 *
 * Coordinates the rule engine, history service, and AI client to provide
 * mapping capabilities for the three gRPC RPCs.
 */
@Service
public class TemplateMappingService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateMappingService.class);

    private final MappingTemplateRepository templateRepository;
    private final MappingRuleRepository ruleRepository;
    private final MappingRuleEngine ruleEngine;
    private final MappingHistoryService historyService;
    private final AiMappingClient aiMappingClient;

    public TemplateMappingService(
            MappingTemplateRepository templateRepository,
            MappingRuleRepository ruleRepository,
            MappingRuleEngine ruleEngine,
            MappingHistoryService historyService,
            AiMappingClient aiMappingClient) {
        this.templateRepository = templateRepository;
        this.ruleRepository = ruleRepository;
        this.ruleEngine = ruleEngine;
        this.historyService = historyService;
        this.aiMappingClient = aiMappingClient;
    }

    /**
     * Apply mapping rules from a template to the given source data.
     * Records successful mappings to history for future learning.
     *
     * @param templateId    UUID of the mapping template to apply
     * @param orgId         Organization ID for history tracking
     * @param sourceHeaders Column headers from the source data
     * @param fileId        File ID for history tracking (nullable)
     * @return Result containing mapped headers and applied actions
     */
    @Transactional
    public ApplyMappingResult applyMapping(UUID templateId, String orgId,
                                            List<String> sourceHeaders, String fileId) {
        MappingTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        List<MappingRuleEntity> rules = ruleRepository.findByTemplateIdOrderByPriorityDesc(templateId);
        List<MappingActionData> actions = ruleEngine.applyRules(rules, sourceHeaders);

        // Build mapped headers list
        List<String> mappedHeaders = actions.stream()
                .map(MappingActionData::targetColumn)
                .collect(Collectors.toList());

        // Record successful mappings to history
        for (MappingActionData action : actions) {
            if (!"UNMAPPED".equals(action.ruleType())) {
                historyService.recordSuccess(
                        orgId, action.sourceColumn(), action.targetColumn(),
                        action.ruleType(), action.confidence(), fileId);
            }
        }

        logger.info("Applied mapping template={} to {} headers, {} mapped, {} unmapped",
                templateId, sourceHeaders.size(),
                actions.stream().filter(a -> !"UNMAPPED".equals(a.ruleType())).count(),
                actions.stream().filter(a -> "UNMAPPED".equals(a.ruleType())).count());

        return new ApplyMappingResult(mappedHeaders, actions);
    }

    /**
     * Suggest mappings for the given source headers, combining multiple sources:
     * 1. History-based suggestions (ranked by frequency)
     * 2. AI-suggested mappings (filtered by confidence threshold)
     *
     * @param orgId         Organization ID
     * @param sourceHeaders Column headers to suggest mappings for
     * @return Combined and deduplicated list of suggestions
     */
    public List<MappingActionData> suggestMapping(String orgId, List<String> sourceHeaders) {
        // Collect suggestions from history
        List<MappingActionData> historySuggestions = historyService.suggestFromHistory(orgId, sourceHeaders);

        // Collect suggestions from AI
        List<MappingActionData> aiSuggestions = aiMappingClient.suggestMappingsViaAi(sourceHeaders, orgId);

        // Also try template-based rules for any available templates
        List<MappingActionData> templateSuggestions = suggestFromTemplates(orgId, sourceHeaders);

        // Merge: template rules first, then history, then AI (dedup by source column)
        Map<String, MappingActionData> merged = new LinkedHashMap<>();
        for (MappingActionData action : templateSuggestions) {
            merged.putIfAbsent(action.sourceColumn().toLowerCase(), action);
        }
        for (MappingActionData action : historySuggestions) {
            merged.putIfAbsent(action.sourceColumn().toLowerCase(), action);
        }
        for (MappingActionData action : aiSuggestions) {
            merged.putIfAbsent(action.sourceColumn().toLowerCase(), action);
        }

        logger.info("Suggested mappings for org={}: template={}, history={}, ai={}, total={}",
                orgId, templateSuggestions.size(), historySuggestions.size(),
                aiSuggestions.size(), merged.size());

        return new ArrayList<>(merged.values());
    }

    /**
     * Map Excel columns to form fields based on header name matching.
     *
     * @param orgId         Organization ID
     * @param sourceHeaders Excel column headers
     * @return Suggestions combining all available sources
     */
    public List<MappingActionData> mapExcelToForm(String orgId, List<String> sourceHeaders) {
        // Reuse the same suggestion logic for form field mapping
        return suggestMapping(orgId, sourceHeaders);
    }

    private List<MappingActionData> suggestFromTemplates(String orgId, List<String> sourceHeaders) {
        List<MappingTemplateEntity> templates = templateRepository.findByOrgIdOrOrgIdIsNull(orgId);
        List<MappingActionData> allActions = new ArrayList<>();

        for (MappingTemplateEntity template : templates) {
            if (!template.isActive()) continue;
            List<MappingRuleEntity> rules = ruleRepository.findByTemplateIdOrderByPriorityDesc(template.getId());
            List<MappingActionData> actions = ruleEngine.applyRules(rules, sourceHeaders);
            // Only include successfully mapped columns
            for (MappingActionData action : actions) {
                if (!"UNMAPPED".equals(action.ruleType())) {
                    allActions.add(action);
                }
            }
        }

        return allActions;
    }

    /**
     * Result of applying a mapping template.
     */
    public record ApplyMappingResult(
            List<String> mappedHeaders,
            List<MappingActionData> actions
    ) {}
}
