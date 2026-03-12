package com.reportplatform.template.tmpl.service;

import com.reportplatform.template.tmpl.entity.MappingHistoryEntity;
import com.reportplatform.template.tmpl.repository.MappingHistoryRepository;
import com.reportplatform.template.tmpl.service.MappingRuleEngine.MappingActionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for MappingHistoryService – learning and suggestion logic.
 */
@ExtendWith(MockitoExtension.class)
class MappingHistoryServiceTest {

    @Mock
    private MappingHistoryRepository historyRepository;

    private MappingHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new MappingHistoryService(historyRepository);
    }

    @Test
    void recordSuccessCreatesNewEntry() {
        when(historyRepository.findByOrgIdAndSourceColumnAndTargetColumn("org-1", "Naklady", "amount_czk"))
                .thenReturn(Optional.empty());

        historyService.recordSuccess("org-1", "Naklady", "amount_czk", "SYNONYM", 0.95, "file-123");

        ArgumentCaptor<MappingHistoryEntity> captor = ArgumentCaptor.forClass(MappingHistoryEntity.class);
        verify(historyRepository).save(captor.capture());

        MappingHistoryEntity saved = captor.getValue();
        assertEquals("org-1", saved.getOrgId());
        assertEquals("Naklady", saved.getSourceColumn());
        assertEquals("amount_czk", saved.getTargetColumn());
        assertEquals(1, saved.getUsedCount());
    }

    @Test
    void recordSuccessIncrementsExistingCount() {
        MappingHistoryEntity existing = new MappingHistoryEntity();
        existing.setOrgId("org-1");
        existing.setSourceColumn("Naklady");
        existing.setTargetColumn("amount_czk");
        existing.setUsedCount(3);
        existing.setConfidence(0.9);

        when(historyRepository.findByOrgIdAndSourceColumnAndTargetColumn("org-1", "Naklady", "amount_czk"))
                .thenReturn(Optional.of(existing));

        historyService.recordSuccess("org-1", "Naklady", "amount_czk", "SYNONYM", 0.95, "file-456");

        verify(historyRepository).save(existing);
        assertEquals(4, existing.getUsedCount());
        assertEquals(0.95, existing.getConfidence()); // updated to higher confidence
    }

    @Test
    void suggestFromHistoryReturnsMostUsedMapping() {
        // AC: Second upload from same org → mapping auto-suggested from history
        MappingHistoryEntity entry = new MappingHistoryEntity();
        entry.setOrgId("org-1");
        entry.setSourceColumn("Naklady");
        entry.setTargetColumn("amount_czk");
        entry.setUsedCount(5);
        entry.setConfidence(0.95);

        when(historyRepository.findByOrgIdAndSourceColumnIgnoreCaseOrderByUsedCountDesc("org-1", "Naklady"))
                .thenReturn(List.of(entry));

        List<MappingActionData> suggestions = historyService.suggestFromHistory("org-1", List.of("Naklady"));

        assertEquals(1, suggestions.size());
        assertEquals("amount_czk", suggestions.getFirst().targetColumn());
        assertEquals("HISTORY", suggestions.getFirst().ruleType());
    }

    @Test
    void suggestFromHistoryReturnsEmptyForUnknownHeader() {
        when(historyRepository.findByOrgIdAndSourceColumnIgnoreCaseOrderByUsedCountDesc("org-1", "Unknown"))
                .thenReturn(List.of());

        List<MappingActionData> suggestions = historyService.suggestFromHistory("org-1", List.of("Unknown"));

        assertTrue(suggestions.isEmpty());
    }
}
