package com.reportplatform.lifecycle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.lifecycle.model.SubmissionChecklistEntity;
import com.reportplatform.lifecycle.repository.SubmissionChecklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionChecklistService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionChecklistService.class);
    private static final TypeReference<List<Map<String, Object>>> CHECKLIST_TYPE = new TypeReference<>() {};

    private final SubmissionChecklistRepository checklistRepository;
    private final ObjectMapper objectMapper;

    public SubmissionChecklistService(SubmissionChecklistRepository checklistRepository,
                                     ObjectMapper objectMapper) {
        this.checklistRepository = checklistRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<SubmissionChecklistEntity> getChecklist(UUID reportId) {
        return checklistRepository.findByReportId(reportId);
    }

    public SubmissionChecklistEntity createDefaultChecklist(UUID reportId) {
        List<Map<String, Object>> items = List.of(
                Map.of("key", "required_fields", "label", "All required form fields filled", "completed", false),
                Map.of("key", "required_sheets", "label", "All required sheets uploaded", "completed", false),
                Map.of("key", "validation_rules", "label", "Validation rules pass", "completed", false)
        );
        return saveChecklist(reportId, items);
    }

    public SubmissionChecklistEntity updateChecklistItem(UUID reportId, String key, boolean completed) {
        SubmissionChecklistEntity entity = checklistRepository.findByReportId(reportId)
                .orElseGet(() -> createDefaultChecklist(reportId));

        try {
            List<Map<String, Object>> items = objectMapper.readValue(entity.getChecklistJson(), CHECKLIST_TYPE);
            items.stream()
                    .filter(item -> key.equals(item.get("key")))
                    .findFirst()
                    .ifPresent(item -> item.put("completed", completed));

            int completedPct = calculateCompletionPct(items);
            entity.setChecklistJson(objectMapper.writeValueAsString(items));
            entity.setCompletedPct(completedPct);
            return checklistRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse checklist JSON for report {}", reportId, e);
            throw new RuntimeException("Failed to update checklist", e);
        }
    }

    public boolean isComplete(UUID reportId) {
        return checklistRepository.findByReportId(reportId)
                .map(c -> c.getCompletedPct() >= 100)
                .orElse(false);
    }

    private SubmissionChecklistEntity saveChecklist(UUID reportId, List<Map<String, Object>> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            int pct = calculateCompletionPct(items);
            var entity = new SubmissionChecklistEntity(reportId, json, pct);
            return checklistRepository.save(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize checklist", e);
        }
    }

    private int calculateCompletionPct(List<Map<String, Object>> items) {
        if (items.isEmpty()) return 100;
        long completed = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("completed")))
                .count();
        return (int) (completed * 100 / items.size());
    }
}
