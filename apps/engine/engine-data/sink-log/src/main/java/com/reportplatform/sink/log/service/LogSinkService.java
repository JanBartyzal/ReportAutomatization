package com.reportplatform.sink.log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.log.entity.ProcessingLogEntity;
import com.reportplatform.sink.log.repository.ProcessingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LogSinkService {
    private static final Logger logger = LoggerFactory.getLogger(LogSinkService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ProcessingLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public LogSinkService(ProcessingLogRepository logRepository, ObjectMapper objectMapper) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AppendLogResult appendLog(String fileId, String workflowId, String stepName,
            String status, Long durationMs, String errorDetail, Map<String, String> metadata) {

        ProcessingLogEntity entity = new ProcessingLogEntity();
        entity.setFileId(fileId);
        entity.setWorkflowId(workflowId);
        entity.setStepName(stepName);
        entity.setStatus(status);
        entity.setDurationMs(durationMs);
        entity.setErrorDetail(errorDetail);
        entity.setCreatedAt(OffsetDateTime.now());

        try {
            entity.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            entity.setMetadata("{}");
        }

        entity = logRepository.save(entity);
        logger.info("Appended log: id={}, fileId={}, step={}", entity.getId(), fileId, stepName);

        return new AppendLogResult(entity.getId().toString(), entity.getCreatedAt().format(ISO_FORMATTER));
    }

    @Transactional
    public List<AppendLogResult> batchAppendLog(List<AppendLogRequest> entries) {
        return entries.stream()
                .map(e -> appendLog(e.fileId(), e.workflowId(), e.stepName(),
                        e.status(), e.durationMs(), e.errorDetail(), e.metadata()))
                .toList();
    }

    public record AppendLogRequest(String fileId, String workflowId, String stepName,
            String status, Long durationMs, String errorDetail, Map<String, String> metadata) {
    }

    public record AppendLogResult(String logId, String recordedAt) {
    }
}
