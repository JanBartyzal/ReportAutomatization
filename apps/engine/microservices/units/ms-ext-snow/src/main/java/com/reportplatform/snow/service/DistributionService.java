package com.reportplatform.snow.service;

import com.reportplatform.snow.model.dto.CreateDistributionRuleRequest;
import com.reportplatform.snow.model.dto.DistributionHistoryDTO;
import com.reportplatform.snow.model.dto.DistributionRuleDTO;
import com.reportplatform.snow.model.entity.DistributionHistoryEntity;
import com.reportplatform.snow.model.entity.DistributionHistoryEntity.DistributionStatus;
import com.reportplatform.snow.model.entity.DistributionRuleEntity;
import com.reportplatform.snow.repository.DistributionHistoryRepository;
import com.reportplatform.snow.repository.DistributionRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DistributionService {

    private static final Logger logger = LoggerFactory.getLogger(DistributionService.class);

    private final DistributionRuleRepository distributionRuleRepository;
    private final DistributionHistoryRepository distributionHistoryRepository;

    public DistributionService(DistributionRuleRepository distributionRuleRepository,
                               DistributionHistoryRepository distributionHistoryRepository) {
        this.distributionRuleRepository = distributionRuleRepository;
        this.distributionHistoryRepository = distributionHistoryRepository;
    }

    // ==================== CRUD for distribution rules ====================

    @Transactional
    public DistributionRuleDTO createRule(UUID orgId, CreateDistributionRuleRequest request) {
        logger.info("Creating distribution rule for org: {}, schedule: {}", orgId, request.getScheduleId());

        DistributionRuleEntity entity = new DistributionRuleEntity();
        entity.setOrgId(orgId);
        entity.setScheduleId(request.getScheduleId());
        entity.setReportTemplateId(request.getReportTemplateId());
        entity.setRecipients(request.getRecipients());
        entity.setFormat(request.getFormat());
        entity.setEnabled(request.isEnabled());

        DistributionRuleEntity saved = distributionRuleRepository.save(entity);
        logger.info("Created distribution rule: {}", saved.getId());
        return toRuleDTO(saved);
    }

    public List<DistributionRuleDTO> getRulesBySchedule(UUID scheduleId) {
        return distributionRuleRepository.findByScheduleId(scheduleId).stream()
                .map(this::toRuleDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DistributionRuleDTO updateRule(UUID ruleId, CreateDistributionRuleRequest request) {
        DistributionRuleEntity entity = distributionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution rule not found: " + ruleId));

        if (request.getReportTemplateId() != null) {
            entity.setReportTemplateId(request.getReportTemplateId());
        }
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            entity.setRecipients(request.getRecipients());
        }
        if (request.getFormat() != null) {
            entity.setFormat(request.getFormat());
        }
        entity.setEnabled(request.isEnabled());

        DistributionRuleEntity saved = distributionRuleRepository.save(entity);
        logger.info("Updated distribution rule: {}", saved.getId());
        return toRuleDTO(saved);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        DistributionRuleEntity entity = distributionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Distribution rule not found: " + ruleId));
        distributionRuleRepository.delete(entity);
        logger.info("Deleted distribution rule: {}", ruleId);
    }

    public Page<DistributionHistoryDTO> getHistory(UUID orgId, Pageable pageable) {
        return distributionHistoryRepository.findByOrgIdOrderByCreatedAtDesc(orgId, pageable)
                .map(this::toHistoryDTO);
    }

    // ==================== Distribution pipeline ====================

    /**
     * Triggered after a sync completes. Finds all enabled distribution rules
     * for the given schedule and executes the distribution pipeline for each.
     */
    @Transactional
    public void onSyncCompleted(UUID scheduleId) {
        logger.info("Processing distribution rules for completed sync schedule: {}", scheduleId);

        List<DistributionRuleEntity> rules = distributionRuleRepository
                .findByScheduleIdAndEnabledTrue(scheduleId);

        if (rules.isEmpty()) {
            logger.debug("No enabled distribution rules found for schedule: {}", scheduleId);
            return;
        }

        logger.info("Found {} enabled distribution rule(s) for schedule: {}", rules.size(), scheduleId);

        for (DistributionRuleEntity rule : rules) {
            processDistributionRule(rule);
        }
    }

    private void processDistributionRule(DistributionRuleEntity rule) {
        logger.info("Processing distribution rule: {} (template: {}, recipients: {})",
                rule.getId(), rule.getReportTemplateId(), rule.getRecipients().size());

        // Create a PENDING history record
        DistributionHistoryEntity history = new DistributionHistoryEntity();
        history.setOrgId(rule.getOrgId());
        history.setRuleId(rule.getId());
        history.setRecipients(rule.getRecipients());
        history.setStatus(DistributionStatus.PENDING);
        history = distributionHistoryRepository.save(history);

        try {
            // TODO: Step 1 - Call MS-GEN-XLS via Dapr gRPC to generate the report
            // DaprClient daprClient = ...;
            // GenerateReportRequest grpcRequest = GenerateReportRequest.newBuilder()
            //     .setTemplateId(rule.getReportTemplateId().toString())
            //     .setFormat(rule.getFormat())
            //     .build();
            // GenerateReportResponse grpcResponse = daprClient.invokeMethod(
            //     "ms-gen-xls", "generateReport", grpcRequest, HttpExtension.POST).block();
            // String blobUrl = grpcResponse.getBlobUrl();
            String blobUrl = null; // Placeholder until MS-GEN-XLS integration is implemented

            // TODO: Step 2 - Publish email notification event via Dapr Pub/Sub to MS-NOTIF
            // Map<String, Object> notificationEvent = new HashMap<>();
            // notificationEvent.put("recipients", rule.getRecipients());
            // notificationEvent.put("reportBlobUrl", blobUrl);
            // notificationEvent.put("templateId", rule.getReportTemplateId().toString());
            // notificationEvent.put("format", rule.getFormat());
            // daprClient.publishEvent("pubsub", "distribution.email.send", notificationEvent).block();

            // Step 3 - Update history record as SENT
            history.setReportBlobUrl(blobUrl);
            history.setStatus(DistributionStatus.SENT);
            history.setSentAt(Instant.now());
            distributionHistoryRepository.save(history);

            logger.info("Distribution completed for rule: {}. History: {}", rule.getId(), history.getId());

        } catch (Exception ex) {
            logger.error("Distribution failed for rule: {}", rule.getId(), ex);

            history.setStatus(DistributionStatus.FAILED);
            history.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
            distributionHistoryRepository.save(history);
        }
    }

    // ==================== Mapping Helpers ====================

    private DistributionRuleDTO toRuleDTO(DistributionRuleEntity entity) {
        DistributionRuleDTO dto = new DistributionRuleDTO();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setScheduleId(entity.getScheduleId());
        dto.setReportTemplateId(entity.getReportTemplateId());
        dto.setRecipients(entity.getRecipients());
        dto.setFormat(entity.getFormat());
        dto.setEnabled(entity.isEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private DistributionHistoryDTO toHistoryDTO(DistributionHistoryEntity entity) {
        DistributionHistoryDTO dto = new DistributionHistoryDTO();
        dto.setId(entity.getId());
        dto.setOrgId(entity.getOrgId());
        dto.setRuleId(entity.getRuleId());
        dto.setRecipients(entity.getRecipients());
        dto.setReportBlobUrl(entity.getReportBlobUrl());
        dto.setStatus(entity.getStatus().name());
        dto.setSentAt(entity.getSentAt());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
