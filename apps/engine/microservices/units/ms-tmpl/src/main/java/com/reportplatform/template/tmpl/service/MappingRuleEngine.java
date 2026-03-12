package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingRuleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Core mapping rule engine that evaluates rules against source headers.
 *
 * Rule priority chain: EXACT_MATCH -> SYNONYM -> REGEX -> (history/AI handled externally).
 * Rules within the same type are ordered by the priority field (higher first).
 */
@Service
public class MappingRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(MappingRuleEngine.class);

    /**
     * Apply mapping rules to a list of source headers.
     *
     * @param rules         Ordered list of rules (by priority DESC)
     * @param sourceHeaders Column headers from the source data
     * @return List of applied mapping actions
     */
    public List<MappingActionData> applyRules(List<MappingRuleEntity> rules, List<String> sourceHeaders) {
        List<MappingActionData> actions = new ArrayList<>();

        for (String header : sourceHeaders) {
            Optional<MappingActionData> match = matchHeader(rules, header);
            if (match.isPresent()) {
                actions.add(match.get());
            } else {
                // No rule matched - column passes through unmapped
                actions.add(new MappingActionData(header, header, "UNMAPPED", 0.0));
            }
        }

        return actions;
    }

    private Optional<MappingActionData> matchHeader(List<MappingRuleEntity> rules, String header) {
        // Try each rule in priority order
        for (MappingRuleEntity rule : rules) {
            Optional<MappingActionData> result = switch (rule.getRuleType()) {
                case "EXACT_MATCH" -> tryExactMatch(rule, header);
                case "SYNONYM" -> trySynonymMatch(rule, header);
                case "REGEX" -> tryRegexMatch(rule, header);
                default -> Optional.empty();
            };
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private Optional<MappingActionData> tryExactMatch(MappingRuleEntity rule, String header) {
        if (rule.getSourcePattern().equalsIgnoreCase(header)) {
            return Optional.of(new MappingActionData(
                    header, rule.getTargetColumn(), "EXACT_MATCH", rule.getConfidence()));
        }
        return Optional.empty();
    }

    private Optional<MappingActionData> trySynonymMatch(MappingRuleEntity rule, String header) {
        // Synonym rules store comma-separated synonyms in source_pattern
        String[] synonyms = rule.getSourcePattern().split(",");
        for (String synonym : synonyms) {
            if (synonym.trim().equalsIgnoreCase(header)) {
                return Optional.of(new MappingActionData(
                        header, rule.getTargetColumn(), "SYNONYM", rule.getConfidence()));
            }
        }
        return Optional.empty();
    }

    private Optional<MappingActionData> tryRegexMatch(MappingRuleEntity rule, String header) {
        try {
            Pattern pattern = Pattern.compile(rule.getSourcePattern(), Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(header).matches()) {
                return Optional.of(new MappingActionData(
                        header, rule.getTargetColumn(), "REGEX", rule.getConfidence()));
            }
        } catch (PatternSyntaxException e) {
            logger.warn("Invalid regex pattern in rule {}: {}", rule.getId(), rule.getSourcePattern());
        }
        return Optional.empty();
    }

    /**
     * Data class representing a mapping action result.
     */
    public record MappingActionData(
            String sourceColumn,
            String targetColumn,
            String ruleType,
            double confidence
    ) {}
}
