package com.reportplatform.qry.service;

import com.reportplatform.qry.model.DocumentEntity;
import com.reportplatform.qry.model.FileSummaryView;
import com.reportplatform.qry.model.ParsedTableEntity;
import com.reportplatform.qry.model.ProcessingLogEntity;
import com.reportplatform.qry.model.dto.DocumentDto;
import com.reportplatform.qry.model.dto.FileDataResponse;
import com.reportplatform.qry.model.dto.ProcessingLogDto;
import com.reportplatform.qry.model.dto.SlideDataResponse;
import com.reportplatform.qry.model.dto.SlideDto;
import com.reportplatform.qry.model.dto.TableDataDto;
import com.reportplatform.qry.model.dto.TableQueryResponse;
import com.reportplatform.qry.repository.QryDocumentRepository;
import com.reportplatform.qry.repository.FileSummaryRepository;
import com.reportplatform.qry.repository.QryParsedTableRepository;
import com.reportplatform.qry.repository.QryProcessingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core query service that reads from the database and caches results in Redis.
 * All methods are read-only transactional.
 *
 * <p>Each public method sets {@code app.current_org_id} via {@code SET LOCAL}
 * inside its own {@code @Transactional} boundary so that PostgreSQL RLS
 * policies can filter rows correctly.</p>
 */
@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private static final String ENTITY_FILE_DATA = "file-data";
    private static final String ENTITY_SLIDES = "slides";
    private static final String ENTITY_TABLES = "tables";
    private static final String ENTITY_DOCUMENT = "document";
    private static final String ENTITY_LOGS = "logs";

    private final QryParsedTableRepository parsedTableRepository;
    private final QryDocumentRepository documentRepository;
    private final QryProcessingLogRepository processingLogRepository;
    private final FileSummaryRepository fileSummaryRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public QueryService(QryParsedTableRepository parsedTableRepository,
                        QryDocumentRepository documentRepository,
                        QryProcessingLogRepository processingLogRepository,
                        FileSummaryRepository fileSummaryRepository,
                        CacheService cacheService,
                        ObjectMapper objectMapper) {
        this.parsedTableRepository = parsedTableRepository;
        this.documentRepository = documentRepository;
        this.processingLogRepository = processingLogRepository;
        this.fileSummaryRepository = fileSummaryRepository;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    /**
     * Sets the RLS context variable for the current transaction.
     * Must be called inside a {@code @Transactional} method.
     */
    private void setRlsContext(String orgId) {
        if (orgId != null && !orgId.isBlank()) {
            UUID.fromString(orgId); // validate format
            entityManager.createNativeQuery(
                            "SELECT set_config('app.current_org_id', :orgId, true)")
                    .setParameter("orgId", orgId)
                    .getSingleResult();
        }
    }

    /**
     * Returns all parsed data (tables + documents) for a specific file.
     */
    @Transactional(readOnly = true)
    public FileDataResponse getFileData(String orgId, UUID fileId) {
        setRlsContext(orgId);

        String cacheKey = cacheService.buildKey(orgId, ENTITY_FILE_DATA, fileId.toString());
        Optional<FileDataResponse> cached = cacheService.getCached(cacheKey, FileDataResponse.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for file data: {}", fileId);
            return cached.get();
        }

        // Get file summary for metadata — validates file belongs to the org
        FileSummaryView summary = fileSummaryRepository
                .findByFileIdAndOrgId(fileId, UUID.fromString(orgId))
                .orElse(null);

        // RLS enforcement: if file doesn't belong to this org, return empty response
        if (summary == null) {
            log.warn("File {} not found for org {} — returning empty response", fileId, orgId);
            return new FileDataResponse(fileId, null, null, List.of(), List.of(), List.of());
        }

        String filename = summary.getFilename();
        String mimeType = summary.getMimeType();

        // Get tables for this file
        List<ParsedTableEntity> tableEntities = parsedTableRepository.findByFileId(fileId.toString());
        List<TableDataDto> tables = tableEntities.stream()
                .map(this::toTableDto)
                .toList();

        // Get documents for this file
        List<DocumentEntity> docEntities = documentRepository.findByFileId(fileId.toString());
        List<DocumentDto> documents = docEntities.stream()
                .map(this::toDocumentDto)
                .toList();

        // Build slides from SLIDE_TEXT_N documents (for PPTX files)
        List<SlideDto> slides = docEntities.stream()
                .filter(d -> d.getDocumentType() != null && d.getDocumentType().startsWith("SLIDE_TEXT_"))
                .map(this::toSlideDto)
                .sorted(java.util.Comparator.comparingInt(SlideDto::slideIndex))
                .toList();

        FileDataResponse response = new FileDataResponse(fileId, filename, mimeType, tables, documents, slides);
        cacheService.putCache(cacheKey, response);
        return response;
    }

    /**
     * Returns slide content with image URLs for a file.
     * Extracts slide data from documents of type SLIDE_TEXT.
     */
    @Transactional(readOnly = true)
    public SlideDataResponse getSlideData(String orgId, UUID fileId) {
        setRlsContext(orgId);

        String cacheKey = cacheService.buildKey(orgId, ENTITY_SLIDES, fileId.toString());
        Optional<SlideDataResponse> cached = cacheService.getCached(cacheKey, SlideDataResponse.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for slide data: {}", fileId);
            return cached.get();
        }

        FileSummaryView summary = fileSummaryRepository
                .findByFileIdAndOrgId(fileId, UUID.fromString(orgId))
                .orElse(null);

        String filename = summary != null ? summary.getFilename() : null;

        // Get SLIDE_TEXT_N documents for this file (stored as SLIDE_TEXT_0, SLIDE_TEXT_1, ...)
        List<DocumentEntity> slideDocuments = documentRepository
                .findSlideDocumentsByFileId(fileId.toString());

        List<SlideDto> slides = slideDocuments.stream()
                .map(this::toSlideDto)
                .sorted(java.util.Comparator.comparingInt(SlideDto::slideIndex))
                .toList();

        SlideDataResponse response = new SlideDataResponse(fileId, filename, slides);
        cacheService.putCache(cacheKey, response);
        return response;
    }

    /**
     * Query table data with pagination and optional filters (backward compatible).
     */
    @Transactional(readOnly = true)
    public TableQueryResponse queryTables(String orgId, int page, int size,
                                          String sourceSheet, String fileId) {
        return queryTables(orgId, page, size, sourceSheet, fileId, null);
    }

    /**
     * Query table data with pagination, optional filters, and scope.
     * When scope is CENTRAL or LOCAL, filters by that scope.
     * When scope is ALL or null, returns all scopes (RLS still enforced at DB level).
     */
    @Transactional(readOnly = true)
    public TableQueryResponse queryTables(String orgId, int page, int size,
                                          String sourceSheet, String fileId, String scope) {
        setRlsContext(orgId);

        size = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<ParsedTableEntity> tablePage;
        boolean useScope = scope != null && !"ALL".equals(scope);

        if (useScope) {
            tablePage = queryTablesWithScope(orgId, scope, fileId, sourceSheet, pageRequest);
        } else {
            tablePage = queryTablesWithoutScope(orgId, fileId, sourceSheet, pageRequest);
        }

        List<TableDataDto> tables = tablePage.getContent().stream()
                .map(this::toTableDto)
                .toList();

        UUID nextCursor = null;
        if (tablePage.hasNext() && !tables.isEmpty()) {
            nextCursor = tables.get(tables.size() - 1).id();
        }

        return new TableQueryResponse(
                tables,
                tablePage.getNumber(),
                tablePage.getSize(),
                tablePage.getTotalElements(),
                tablePage.getTotalPages(),
                nextCursor
        );
    }

    private Page<ParsedTableEntity> queryTablesWithScope(String orgId, String scope,
                                                          String fileId, String sourceSheet,
                                                          PageRequest pageRequest) {
        if (fileId != null && sourceSheet != null) {
            return parsedTableRepository
                    .findByOrgIdAndScopeAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                            orgId, scope, fileId, sourceSheet, pageRequest);
        } else if (fileId != null) {
            return parsedTableRepository
                    .findByOrgIdAndScopeAndFileIdOrderByCreatedAtDesc(orgId, scope, fileId, pageRequest);
        } else if (sourceSheet != null) {
            return parsedTableRepository
                    .findByOrgIdAndScopeAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                            orgId, scope, sourceSheet, pageRequest);
        }
        return parsedTableRepository
                .findByOrgIdAndScopeOrderByCreatedAtDesc(orgId, scope, pageRequest);
    }

    private Page<ParsedTableEntity> queryTablesWithoutScope(String orgId, String fileId,
                                                             String sourceSheet,
                                                             PageRequest pageRequest) {
        if (fileId != null && sourceSheet != null) {
            return parsedTableRepository
                    .findByOrgIdAndFileIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                            orgId, fileId, sourceSheet, pageRequest);
        } else if (fileId != null) {
            return parsedTableRepository
                    .findByOrgIdAndFileIdOrderByCreatedAtDesc(orgId, fileId, pageRequest);
        } else if (sourceSheet != null) {
            return parsedTableRepository
                    .findByOrgIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                            orgId, sourceSheet, pageRequest);
        }
        return parsedTableRepository
                .findByOrgIdOrderByCreatedAtDesc(orgId, pageRequest);
    }

    /**
     * Returns a single document by ID, scoped by org.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentDto> getDocument(String orgId, UUID documentId) {
        setRlsContext(orgId);

        String cacheKey = cacheService.buildKey(orgId, ENTITY_DOCUMENT, documentId.toString());
        Optional<DocumentDto> cached = cacheService.getCached(cacheKey, DocumentDto.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for document: {}", documentId);
            return cached;
        }

        Optional<DocumentDto> result = documentRepository.findByIdAndOrgId(documentId, orgId)
                .map(this::toDocumentDto);

        result.ifPresent(dto -> cacheService.putCache(cacheKey, dto));
        return result;
    }

    /**
     * Returns documents for a specific file ID.
     */
    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsByFileId(String orgId, String fileId) {
        setRlsContext(orgId);

        if (fileId == null || fileId.isBlank()) {
            return List.of();
        }
        return documentRepository.findByFileId(fileId).stream()
                .map(this::toDocumentDto)
                .toList();
    }

    /**
     * Returns the processing step timeline for a file.
     */
    @Transactional(readOnly = true)
    public List<ProcessingLogDto> getProcessingLogs(String orgId, String fileId) {
        setRlsContext(orgId);

        String cacheKey = cacheService.buildKey(orgId, ENTITY_LOGS, fileId);
        Optional<List> cached = cacheService.getCached(cacheKey, List.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for processing logs: {}", fileId);
            @SuppressWarnings("unchecked")
            List<ProcessingLogDto> result = cached.get();
            return result;
        }

        List<ProcessingLogEntity> logEntities = processingLogRepository
                .findByFileIdOrderByCreatedAtAsc(fileId);

        List<ProcessingLogDto> logs = logEntities.stream()
                .map(this::toProcessingLogDto)
                .toList();

        cacheService.putCache(cacheKey, logs);
        return logs;
    }

    // ---- Mapping methods ----

    @SuppressWarnings("unchecked")
    private TableDataDto toTableDto(ParsedTableEntity entity) {
        Object headers = deserializeJsonField(entity.getHeaders(), List.class);
        Object rows = deserializeJsonField(entity.getRows(), List.class);
        Object metadata = deserializeJsonField(entity.getMetadata(), Map.class);

        // Convert numeric strings in rows to actual Number types
        if (rows instanceof List<?> rowList) {
            rows = rowList.stream().map(this::coerceRowNumerics).toList();
        }

        return new TableDataDto(
                entity.getId(),
                entity.getFileId(),
                entity.getSourceSheet(),
                headers,
                rows,
                metadata,
                entity.getCreatedAt()
        );
    }

    /**
     * Converts numeric string values in a row (List or Map) to actual Number types.
     */
    @SuppressWarnings("unchecked")
    private Object coerceRowNumerics(Object row) {
        if (row instanceof List<?> cells) {
            return cells.stream().map(this::tryParseNumeric).toList();
        }
        if (row instanceof Map<?, ?> cellMap) {
            var converted = new java.util.LinkedHashMap<String, Object>();
            ((Map<String, Object>) cellMap).forEach((k, v) -> converted.put(k, tryParseNumeric(v)));
            return converted;
        }
        return row;
    }

    /**
     * Attempts to parse a string value as a number (int or double).
     * Returns the original value if parsing fails.
     */
    private Object tryParseNumeric(Object value) {
        if (!(value instanceof String str) || str.isEmpty()) {
            return value;
        }
        try {
            long l = Long.parseLong(str);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    private DocumentDto toDocumentDto(DocumentEntity entity) {
        return new DocumentDto(
                entity.getId(),
                entity.getFileId(),
                entity.getDocumentType(),
                entity.getContent(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private SlideDto toSlideDto(DocumentEntity entity) {
        Object content = entity.getContent();
        // JSONB may arrive as a raw JSON String depending on the JDBC driver
        if (content instanceof String str && !str.isBlank()) {
            try {
                content = objectMapper.readValue(str, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse slide content JSON: {}", e.getMessage());
            }
        }
        if (content instanceof Map<?, ?> contentMap) {
            // Accept both camelCase (new) and snake_case (legacy) key names
            int slideIndex = 0;
            if (contentMap.containsKey("slideIndex")) {
                slideIndex = ((Number) contentMap.get("slideIndex")).intValue();
            } else if (contentMap.containsKey("slide_index")) {
                slideIndex = ((Number) contentMap.get("slide_index")).intValue();
            } else {
                // Fall back to extracting index from document_type = "SLIDE_TEXT_N"
                String docType = entity.getDocumentType();
                if (docType != null && docType.startsWith("SLIDE_TEXT_")) {
                    try {
                        slideIndex = Integer.parseInt(docType.substring("SLIDE_TEXT_".length()));
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse slide index from document type '{}': {}", docType, e.getMessage());
                    }
                }
            }

            String title = contentMap.containsKey("title") ? (String) contentMap.get("title") : "";
            List<Object> texts = contentMap.containsKey("texts")
                    ? (List<Object>) contentMap.get("texts") : Collections.emptyList();

            // Tables: prefer content (new), fall back to metadata (legacy)
            List<Object> tables = Collections.emptyList();
            if (contentMap.containsKey("tables") && contentMap.get("tables") instanceof List<?> t) {
                tables = (List<Object>) t;
            } else if (entity.getMetadata() instanceof Map<?, ?> metaMap && metaMap.containsKey("tables")
                    && metaMap.get("tables") instanceof List<?> mt) {
                tables = (List<Object>) mt;
            }

            String imageUrl = contentMap.containsKey("imageUrl") ? (String) contentMap.get("imageUrl") : null;
            String notes = contentMap.containsKey("notes") ? (String) contentMap.get("notes") : "";

            return new SlideDto(slideIndex, title, texts, tables, imageUrl, notes);
        }

        return new SlideDto(0, "", Collections.emptyList(), Collections.emptyList(), null, "");
    }

    private ProcessingLogDto toProcessingLogDto(ProcessingLogEntity entity) {
        return new ProcessingLogDto(
                entity.getId(),
                entity.getFileId(),
                entity.getWorkflowId(),
                entity.getStepName(),
                entity.getStatus(),
                entity.getDurationMs(),
                entity.getErrorDetail(),
                entity.getCreatedAt()
        );
    }

    /**
     * Deserialize a JSONB field that may come as String, JsonNode, or already-parsed type.
     * Handles the case where PostgreSQL JSONB is returned as a raw JSON String
     * instead of a parsed object (e.g., headers as '["Id","Name"]' instead of List).
     */
    @SuppressWarnings("unchecked")
    private <T> Object deserializeJsonField(Object value, Class<T> targetType) {
        if (value == null) {
            return targetType == List.class ? List.of() : Map.of();
        }
        if (value instanceof String str) {
            if (str.isBlank()) {
                return targetType == List.class ? List.of() : Map.of();
            }
            try {
                return objectMapper.readValue(str, targetType);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse JSON string field: {}", e.getMessage());
                return targetType == List.class ? List.of() : Map.of();
            }
        }
        if (value instanceof JsonNode jsonNode) {
            return objectMapper.convertValue(jsonNode, targetType);
        }
        // Already the correct type (List, Map, etc.)
        return value;
    }
}
