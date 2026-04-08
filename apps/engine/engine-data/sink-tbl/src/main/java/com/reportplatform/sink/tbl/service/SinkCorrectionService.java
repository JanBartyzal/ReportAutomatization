package com.reportplatform.sink.tbl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.tbl.entity.ExtractionLearningLogEntity;
import com.reportplatform.sink.tbl.entity.ParsedTableEntity;
import com.reportplatform.sink.tbl.entity.SinkCorrectionEntity;
import com.reportplatform.sink.tbl.repository.ExtractionLearningLogRepository;
import com.reportplatform.sink.tbl.repository.ParsedTableRepository;
import com.reportplatform.sink.tbl.repository.SinkCorrectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing sink corrections (FS25).
 * Corrections are stored as an overlay on immutable parsed_tables.
 */
@Service
public class SinkCorrectionService {

    private static final Logger logger = LoggerFactory.getLogger(SinkCorrectionService.class);

    private final SinkCorrectionRepository correctionRepository;
    private final ExtractionLearningLogRepository learningLogRepository;
    private final ParsedTableRepository parsedTableRepository;
    private final ObjectMapper objectMapper;

    public SinkCorrectionService(
            SinkCorrectionRepository correctionRepository,
            ExtractionLearningLogRepository learningLogRepository,
            ParsedTableRepository parsedTableRepository,
            ObjectMapper objectMapper) {
        this.correctionRepository = correctionRepository;
        this.learningLogRepository = learningLogRepository;
        this.parsedTableRepository = parsedTableRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SinkCorrectionEntity createCorrection(CorrectionData data) {
        logger.info("Creating correction for parsedTableId={}, type={}, row={}, col={}",
                data.parsedTableId(), data.correctionType(), data.rowIndex(), data.colIndex());

        SinkCorrectionEntity entity = new SinkCorrectionEntity();
        entity.setParsedTableId(data.parsedTableId());
        entity.setOrgId(data.orgId());
        entity.setRowIndex(data.rowIndex());
        entity.setColIndex(data.colIndex());
        entity.setOriginalValue(data.originalValue());
        entity.setCorrectedValue(data.correctedValue());
        entity.setCorrectionType(data.correctionType());
        entity.setCorrectedBy(data.correctedBy());
        entity.setCorrectedAt(OffsetDateTime.now());

        if (data.metadata() != null) {
            try {
                entity.setMetadata(objectMapper.writeValueAsString(data.metadata()));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize correction metadata: {}", e.getMessage());
                entity.setMetadata("{}");
            }
        }

        SinkCorrectionEntity saved = correctionRepository.save(entity);

        createLearningLogEntry(saved, data);

        logger.info("Correction created: id={}", saved.getId());
        return saved;
    }

    @Transactional
    public List<SinkCorrectionEntity> createBulkCorrections(List<CorrectionData> corrections) {
        logger.info("Creating {} bulk corrections", corrections.size());
        return corrections.stream()
                .map(this::createCorrection)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SinkCorrectionEntity> getCorrections(UUID parsedTableId) {
        return correctionRepository.findByParsedTableIdOrderByCorrectedAtAsc(parsedTableId);
    }

    @Transactional
    public void deleteCorrection(UUID correctionId) {
        logger.info("Deleting correction id={}", correctionId);
        correctionRepository.deleteById(correctionId);
    }

    @Transactional
    public int deleteAllCorrections(UUID parsedTableId) {
        logger.info("Deleting all corrections for parsedTableId={}", parsedTableId);
        return correctionRepository.deleteByParsedTableId(parsedTableId);
    }

    private void createLearningLogEntry(SinkCorrectionEntity correction, CorrectionData data) {
        ParsedTableEntity table = parsedTableRepository.findById(data.parsedTableId()).orElse(null);
        if (table == null) {
            return;
        }

        ExtractionLearningLogEntity log = new ExtractionLearningLogEntity();
        log.setFileId(table.getFileId());
        log.setParsedTableId(data.parsedTableId());
        log.setOrgId(data.orgId());
        log.setSourceType(extractSourceType(table.getMetadata()));
        log.setSlideIndex(extractSlideIndex(table.getSourceSheet()));
        log.setErrorCategory(data.errorCategory());

        try {
            Map<String, String> original = Map.of(
                    "row_index", String.valueOf(data.rowIndex()),
                    "col_index", String.valueOf(data.colIndex()),
                    "value", data.originalValue() != null ? data.originalValue() : "");
            log.setOriginalSnippet(objectMapper.writeValueAsString(original));

            Map<String, String> corrected = Map.of(
                    "row_index", String.valueOf(data.rowIndex()),
                    "col_index", String.valueOf(data.colIndex()),
                    "value", data.correctedValue() != null ? data.correctedValue() : "");
            log.setCorrectedSnippet(objectMapper.writeValueAsString(corrected));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize learning log snippets: {}", e.getMessage());
        }

        log.setConfidenceScore(extractConfidence(table.getMetadata()));
        log.setCreatedAt(OffsetDateTime.now());
        log.setApplied(false);

        learningLogRepository.save(log);
        logger.debug("Learning log entry created for correction id={}", correction.getId());
    }

    private String extractSourceType(String metadata) {
        if (metadata == null) return null;
        try {
            var node = objectMapper.readTree(metadata);
            if (node.has("source_type")) return node.get("source_type").asText();
            if (node.has("sourceType")) return node.get("sourceType").asText();
        } catch (JsonProcessingException e) {
            // ignore
        }
        return null;
    }

    private Integer extractSlideIndex(String sourceSheet) {
        if (sourceSheet == null) return null;
        try {
            if (sourceSheet.toLowerCase().startsWith("slide_")) {
                return Integer.parseInt(sourceSheet.substring(6));
            }
            return Integer.parseInt(sourceSheet);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float extractConfidence(String metadata) {
        if (metadata == null) return null;
        try {
            var node = objectMapper.readTree(metadata);
            if (node.has("confidence")) return (float) node.get("confidence").asDouble();
            if (node.has("confidence_score")) return (float) node.get("confidence_score").asDouble();
        } catch (JsonProcessingException e) {
            // ignore
        }
        return null;
    }

    public record CorrectionData(
            UUID parsedTableId,
            String orgId,
            Integer rowIndex,
            Integer colIndex,
            String originalValue,
            String correctedValue,
            String correctionType,
            String correctedBy,
            String errorCategory,
            Map<String, Object> metadata) {
    }
}
