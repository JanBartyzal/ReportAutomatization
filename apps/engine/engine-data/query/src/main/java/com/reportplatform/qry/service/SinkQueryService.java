package com.reportplatform.qry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.qry.model.ParsedTableEntity;
import com.reportplatform.qry.model.SinkCorrectionEntity;
import com.reportplatform.qry.model.SinkSelectionEntity;
import com.reportplatform.qry.model.dto.*;
import com.reportplatform.qry.repository.QryParsedTableRepository;
import com.reportplatform.qry.repository.QrySinkCorrectionRepository;
import com.reportplatform.qry.repository.QrySinkSelectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for Sink Browser (FS25).
 * Provides read-only access to sinks with correction overlay and selection state.
 */
@Service
public class SinkQueryService {

    private static final Logger log = LoggerFactory.getLogger(SinkQueryService.class);

    private final QryParsedTableRepository parsedTableRepository;
    private final QrySinkCorrectionRepository correctionRepository;
    private final QrySinkSelectionRepository selectionRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public SinkQueryService(
            QryParsedTableRepository parsedTableRepository,
            QrySinkCorrectionRepository correctionRepository,
            QrySinkSelectionRepository selectionRepository,
            ObjectMapper objectMapper) {
        this.parsedTableRepository = parsedTableRepository;
        this.correctionRepository = correctionRepository;
        this.selectionRepository = selectionRepository;
        this.objectMapper = objectMapper;
    }

    private void setRlsContext(String orgId) {
        if (orgId != null && !orgId.isBlank()) {
            UUID.fromString(orgId);
            entityManager.createNativeQuery("SET LOCAL app.current_org_id = '" + orgId + "'")
                    .executeUpdate();
        }
    }

    /**
     * List all sinks with summary info, paginated.
     * @param search searches source_sheet name (case-insensitive contains)
     */
    @Transactional(readOnly = true)
    public SinkListResponse listSinks(String orgId, int page, int size,
                                       String fileId, String search) {
        setRlsContext(orgId);

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ParsedTableEntity> tablePage;

        if (fileId != null && search != null) {
            tablePage = parsedTableRepository.findByOrgIdAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                    orgId, fileId, search, pageRequest);
        } else if (fileId != null) {
            tablePage = parsedTableRepository.findByOrgIdAndFileIdOrderByCreatedAtDesc(orgId, fileId, pageRequest);
        } else if (search != null) {
            tablePage = parsedTableRepository.findByOrgIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                    orgId, search, pageRequest);
        } else {
            tablePage = parsedTableRepository.findByOrgIdOrderByCreatedAtDesc(orgId, pageRequest);
        }

        // Bulk-fetch real filenames from ingestor.files (cross-schema, single query)
        List<String> rawFileIds = tablePage.getContent().stream()
                .map(ParsedTableEntity::getFileId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> filenameMap = fetchFilenames(rawFileIds);

        List<SinkListItemDto> items = tablePage.getContent().stream()
                .map(t -> toListItem(t, filenameMap))
                .toList();

        UUID nextCursor = tablePage.hasNext() && !items.isEmpty()
                ? items.get(items.size() - 1).id() : null;

        return new SinkListResponse(items, page, size,
                tablePage.getTotalElements(), tablePage.getTotalPages(), nextCursor);
    }

    /**
     * Get full detail of a sink with corrections applied.
     */
    @Transactional(readOnly = true)
    public SinkDetailDto getSinkDetail(String orgId, UUID parsedTableId) {
        setRlsContext(orgId);

        ParsedTableEntity table = parsedTableRepository.findById(parsedTableId).orElse(null);
        if (table == null) {
            return null;
        }

        List<SinkCorrectionEntity> corrections = correctionRepository
                .findByParsedTableIdOrderByCorrectedAtAsc(parsedTableId);
        List<SinkSelectionEntity> selections = selectionRepository
                .findByParsedTableId(parsedTableId);

        List<SinkCorrectionDto> correctionDtos = corrections.stream()
                .map(this::toCorrectionDto)
                .toList();
        List<SinkSelectionDto> selectionDtos = selections.stream()
                .map(this::toSelectionDto)
                .toList();

        Object correctedHeaders = applyHeaderCorrections(table.getHeaders(), corrections);
        Object correctedRows = applyRowCorrections(table.getRows(), corrections);

        return new SinkDetailDto(
                table.getId(),
                table.getFileId(),
                table.getSourceSheet(),
                table.getHeaders(),
                table.getRows(),
                table.getMetadata(),
                table.getCreatedAt(),
                corrections.size(),
                correctionDtos,
                selectionDtos,
                correctedHeaders,
                correctedRows);
    }

    /**
     * Get corrections history for a sink.
     */
    @Transactional(readOnly = true)
    public List<SinkCorrectionDto> getCorrections(String orgId, UUID parsedTableId) {
        setRlsContext(orgId);
        return correctionRepository.findByParsedTableIdOrderByCorrectedAtAsc(parsedTableId)
                .stream()
                .map(this::toCorrectionDto)
                .toList();
    }

    /**
     * Get selections for a period.
     */
    @Transactional(readOnly = true)
    public List<SinkSelectionDto> getSelections(String orgId, String periodId) {
        setRlsContext(orgId);
        return selectionRepository.findByOrgIdAndPeriodIdAndSelectedTrueOrderByPriorityAsc(orgId, periodId)
                .stream()
                .map(this::toSelectionDto)
                .toList();
    }

    /**
     * Get sink data with corrections applied (for dashboard/report consumption).
     */
    @Transactional(readOnly = true)
    public TableDataDto getSinkPreview(String orgId, UUID parsedTableId) {
        setRlsContext(orgId);

        ParsedTableEntity table = parsedTableRepository.findById(parsedTableId).orElse(null);
        if (table == null) return null;

        List<SinkCorrectionEntity> corrections = correctionRepository
                .findByParsedTableIdOrderByCorrectedAtAsc(parsedTableId);

        Object correctedHeaders = applyHeaderCorrections(table.getHeaders(), corrections);
        Object correctedRows = applyRowCorrections(table.getRows(), corrections);

        return new TableDataDto(
                table.getId(),
                table.getFileId(),
                table.getSourceSheet(),
                correctedHeaders,
                correctedRows,
                table.getMetadata(),
                table.getCreatedAt());
    }

    // --- Filename lookup ---

    /**
     * Fetches human-readable filenames from the ingestor schema.
     * {@code ingestor.files.id::text} matches {@code parsed_tables.file_id} directly.
     * Falls back to a truncated file ID when the file is not found.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchFilenames(List<String> fileIds) {
        Map<String, String> result = new HashMap<>();
        if (fileIds.isEmpty()) return result;

        // Pass as a comma-separated string; string_to_array avoids dynamic SQL
        String csv = String.join(",", fileIds);
        try {
            List<Object[]> rows = entityManager.createNativeQuery(
                            "SELECT f.id::text, f.filename " +
                            "FROM ingestor.files f " +
                            "WHERE f.id::text = ANY(string_to_array(:csv, ','))")
                    .setParameter("csv", csv)
                    .getResultList();

            for (Object[] row : rows) {
                if (row[0] != null && row[1] != null) {
                    result.put(row[0].toString(), row[1].toString());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch filenames from ingestor.files: {}", e.getMessage());
        }
        return result;
    }

    // --- Correction overlay logic ---

    private Object applyHeaderCorrections(Object headers, List<SinkCorrectionEntity> corrections) {
        List<String> headerList = deserializeStringList(headers);
        if (headerList == null) return headers;

        for (SinkCorrectionEntity c : corrections) {
            if ("HEADER_RENAME".equals(c.getCorrectionType()) && c.getColIndex() != null
                    && c.getColIndex() < headerList.size()) {
                headerList.set(c.getColIndex(), c.getCorrectedValue());
            }
        }
        return headerList;
    }

    private Object applyRowCorrections(Object rows, List<SinkCorrectionEntity> corrections) {
        List<List<String>> rowList = deserializeRowList(rows);
        if (rowList == null) return rows;

        for (SinkCorrectionEntity c : corrections) {
            if (c.getRowIndex() == null) continue;

            switch (c.getCorrectionType()) {
                case "CELL_VALUE", "COLUMN_TYPE" -> {
                    if (c.getRowIndex() < rowList.size() && c.getColIndex() != null
                            && c.getColIndex() < rowList.get(c.getRowIndex()).size()) {
                        rowList.get(c.getRowIndex()).set(c.getColIndex(), c.getCorrectedValue());
                    }
                }
                case "ROW_DELETE" -> {
                    if (c.getRowIndex() < rowList.size()) {
                        rowList.set(c.getRowIndex(), null); // mark for removal
                    }
                }
                case "ROW_ADD" -> {
                    List<String> newRow = deserializeStringList(c.getCorrectedValue());
                    if (newRow != null) {
                        rowList.add(c.getRowIndex(), newRow);
                    }
                }
                default -> { /* ignore unknown types */ }
            }
        }

        // Remove null-marked rows
        rowList.removeIf(r -> r == null);
        return rowList;
    }

    @SuppressWarnings("unchecked")
    private List<String> deserializeStringList(Object value) {
        if (value == null) return null;
        if (value instanceof List) return new ArrayList<>((List<String>) value);
        try {
            return new ArrayList<>(objectMapper.readValue(value.toString(), new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize string list: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<List<String>> deserializeRowList(Object value) {
        if (value == null) return null;
        if (value instanceof List) {
            List<?> raw = (List<?>) value;
            List<List<String>> result = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof List) {
                    result.add(new ArrayList<>((List<String>) item));
                }
            }
            return result;
        }
        try {
            return new ArrayList<>(objectMapper.readValue(value.toString(),
                    new TypeReference<List<List<String>>>() {}));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize row list: {}", e.getMessage());
            return null;
        }
    }

    // --- Mapping helpers ---

    private SinkListItemDto toListItem(ParsedTableEntity table, Map<String, String> filenameMap) {
        int rowCount = countRows(table.getRows());
        int colCount = countColumns(table.getHeaders());
        long corrCount = correctionRepository.countByParsedTableId(table.getId());
        boolean hasSel = selectionRepository.existsByParsedTableId(table.getId());

        String rawId = table.getFileId();
        String filename = filenameMap.getOrDefault(rawId,
                rawId.length() > 8 ? rawId.substring(0, 8) + "…" : rawId);

        return new SinkListItemDto(
                table.getId(),
                table.getFileId(),
                filename,
                table.getSourceSheet(),
                rowCount,
                colCount,
                table.getMetadata(),
                table.getCreatedAt(),
                corrCount,
                hasSel);
    }

    private SinkCorrectionDto toCorrectionDto(SinkCorrectionEntity e) {
        return new SinkCorrectionDto(
                e.getId(), e.getParsedTableId(),
                e.getRowIndex(), e.getColIndex(),
                e.getOriginalValue(), e.getCorrectedValue(),
                e.getCorrectionType(), e.getCorrectedBy(),
                e.getCorrectedAt(), e.getMetadata());
    }

    private SinkSelectionDto toSelectionDto(SinkSelectionEntity e) {
        return new SinkSelectionDto(
                e.getId(), e.getParsedTableId(),
                e.getPeriodId(), e.getReportType(),
                e.isSelected(), e.getPriority(),
                e.getSelectedBy(), e.getSelectedAt(), e.getNote());
    }

    private int countRows(Object rows) {
        List<?> list = deserializeRowList(rows);
        return list != null ? list.size() : 0;
    }

    private int countColumns(Object headers) {
        List<?> list = deserializeStringList(headers);
        return list != null ? list.size() : 0;
    }
}
