package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingRuleEntity;
import com.reportplatform.template.tmpl.entity.MappingTemplateEntity;
import com.reportplatform.template.tmpl.repository.MappingRuleRepository;
import com.reportplatform.template.tmpl.repository.MappingTemplateRepository;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TemplateMappingService – orchestration of mapping operations.
 */
@ExtendWith(MockitoExtension.class)
class TemplateMappingServiceTest {

    @Mock private MappingTemplateRepository templateRepository;
    @Mock private MappingRuleRepository ruleRepository;
    @Mock private MappingHistoryService historyService;
    @Mock private AiMappingClient aiMappingClient;

    private TemplateMappingService service;
    private MappingRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new MappingRuleEngine();
        service = new TemplateMappingService(
                templateRepository, ruleRepository, ruleEngine, historyService, aiMappingClient);
    }

    @Test
    void applyMappingUsesTemplateRules() {
        UUID templateId = UUID.randomUUID();
        MappingTemplateEntity template = new MappingTemplateEntity();
        template.setId(templateId);

        MappingRuleEntity synonymRule = new MappingRuleEntity();
        synonymRule.setTemplate(template);
        synonymRule.setRuleType("SYNONYM");
        synonymRule.setSourcePattern("Naklady,Cost,Cena");
        synonymRule.setTargetColumn("amount_czk");
        synonymRule.setConfidence(0.95);
        synonymRule.setPriority(90);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ruleRepository.findByTemplateIdOrderByPriorityDesc(templateId)).thenReturn(List.of(synonymRule));

        TemplateMappingService.ApplyMappingResult result =
                service.applyMapping(templateId, "org-1", List.of("Naklady"), null);

        assertEquals(1, result.mappedHeaders().size());
        assertEquals("amount_czk", result.mappedHeaders().getFirst());

        // Verify history was recorded
        verify(historyService).recordSuccess("org-1", "Naklady", "amount_czk", "SYNONYM", 0.95, null);
    }

    @Test
    void suggestMappingCombinesHistoryAndAi() {
        when(historyService.suggestFromHistory("org-1", List.of("Naklady", "Datum")))
                .thenReturn(List.of(
                        new MappingActionData("Naklady", "amount_czk", "HISTORY", 0.95)));

        when(aiMappingClient.suggestMappingsViaAi(List.of("Naklady", "Datum"), "org-1"))
                .thenReturn(List.of(
                        new MappingActionData("Datum", "invoice_date", "AI_SUGGESTED", 0.85)));

        when(templateRepository.findByOrgIdOrOrgIdIsNull("org-1")).thenReturn(List.of());

        List<MappingActionData> suggestions =
                service.suggestMapping("org-1", List.of("Naklady", "Datum"));

        assertEquals(2, suggestions.size());
        // History suggestion for Naklady
        MappingActionData naklady = suggestions.stream()
                .filter(s -> s.sourceColumn().equalsIgnoreCase("Naklady"))
                .findFirst().orElseThrow();
        assertEquals("amount_czk", naklady.targetColumn());

        // AI suggestion for Datum
        MappingActionData datum = suggestions.stream()
                .filter(s -> s.sourceColumn().equalsIgnoreCase("Datum"))
                .findFirst().orElseThrow();
        assertEquals("invoice_date", datum.targetColumn());
    }

    @Test
    void applyMappingThrowsForMissingTemplate() {
        UUID templateId = UUID.randomUUID();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () ->
                service.applyMapping(templateId, "org-1", List.of("Col"), null));
    }
}
