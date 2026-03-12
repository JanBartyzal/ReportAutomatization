package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingRuleEntity;
import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MappingRuleEngine – rule evaluation logic.
 */
class MappingRuleEngineTest {

    private MappingRuleEngine ruleEngine;
    private MappingTemplateEntity template;

    @BeforeEach
    void setUp() {
        ruleEngine = new MappingRuleEngine();
        template = new MappingTemplateEntity();
    }

    @Test
    void exactMatchRuleMapsColumn() {
        List<MappingRuleEntity> rules = List.of(
                createRule("EXACT_MATCH", "Cost", "amount_czk", 1.0, 100));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("Cost"));

        assertEquals(1, result.size());
        assertEquals("amount_czk", result.getFirst().targetColumn());
        assertEquals("EXACT_MATCH", result.getFirst().ruleType());
        assertEquals(1.0, result.getFirst().confidence());
    }

    @Test
    void synonymRuleMapsNakladyToAmountCzk() {
        // AC: Column "Naklady" auto-mapped to "amount_czk" using synonym rule
        List<MappingRuleEntity> rules = List.of(
                createRule("SYNONYM", "Naklady,Cost,Cena,Naklady celkem", "amount_czk", 0.95, 90));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("Naklady"));

        assertEquals(1, result.size());
        assertEquals("amount_czk", result.getFirst().targetColumn());
        assertEquals("SYNONYM", result.getFirst().ruleType());
    }

    @Test
    void regexRuleMapsPattern() {
        List<MappingRuleEntity> rules = List.of(
                createRule("REGEX", "^IT.*cost$", "it_costs", 0.9, 80));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("IT infrastructure cost"));

        assertEquals(1, result.size());
        assertEquals("it_costs", result.getFirst().targetColumn());
        assertEquals("REGEX", result.getFirst().ruleType());
    }

    @Test
    void unmatchedColumnPassesThrough() {
        List<MappingRuleEntity> rules = List.of(
                createRule("EXACT_MATCH", "Cost", "amount_czk", 1.0, 100));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("UnknownColumn"));

        assertEquals(1, result.size());
        assertEquals("UnknownColumn", result.getFirst().targetColumn());
        assertEquals("UNMAPPED", result.getFirst().ruleType());
    }

    @Test
    void higherPriorityRuleWins() {
        List<MappingRuleEntity> rules = new ArrayList<>();
        // Lower priority synonym
        rules.add(createRule("SYNONYM", "Cost,Cena", "cost_generic", 0.8, 50));
        // Higher priority exact match
        rules.add(createRule("EXACT_MATCH", "Cost", "cost_exact", 1.0, 100));

        // Sort by priority DESC (as repository would)
        rules.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("Cost"));

        assertEquals("cost_exact", result.getFirst().targetColumn());
        assertEquals("EXACT_MATCH", result.getFirst().ruleType());
    }

    @Test
    void multipleHeadersMappedIndependently() {
        List<MappingRuleEntity> rules = List.of(
                createRule("EXACT_MATCH", "Cost", "amount_czk", 1.0, 100),
                createRule("EXACT_MATCH", "Date", "invoice_date", 1.0, 100));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("Cost", "Date", "Notes"));

        assertEquals(3, result.size());
        assertEquals("amount_czk", result.get(0).targetColumn());
        assertEquals("invoice_date", result.get(1).targetColumn());
        assertEquals("UNMAPPED", result.get(2).ruleType());
    }

    @Test
    void caseInsensitiveExactMatch() {
        List<MappingRuleEntity> rules = List.of(
                createRule("EXACT_MATCH", "cost", "amount_czk", 1.0, 100));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("COST"));

        assertEquals("amount_czk", result.getFirst().targetColumn());
    }

    @Test
    void invalidRegexDoesNotCrash() {
        List<MappingRuleEntity> rules = List.of(
                createRule("REGEX", "[invalid(", "target", 0.9, 80));

        List<MappingActionData> result = ruleEngine.applyRules(rules, List.of("test"));

        assertEquals("UNMAPPED", result.getFirst().ruleType());
    }

    private MappingRuleEntity createRule(String type, String pattern, String target,
                                          double confidence, int priority) {
        MappingRuleEntity rule = new MappingRuleEntity();
        rule.setTemplate(template);
        rule.setRuleType(type);
        rule.setSourcePattern(pattern);
        rule.setTargetColumn(target);
        rule.setConfidence(confidence);
        rule.setPriority(priority);
        return rule;
    }
}
