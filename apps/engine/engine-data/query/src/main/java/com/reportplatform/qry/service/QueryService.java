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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core query service that reads from the database and caches results in Redis.
 * All methods are read-only transactional.
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

    public QueryService(QryParsedTableRepository parsedTableRepository,
                        QryDocumentRepository documentRepository,
                        QryProcessingLogRepository processingLogRepository,
                        FileSummaryRepository fileSummaryRepository,
                        CacheService cacheService) {
        this.parsedTableRepository = parsedTableRepository;
        this.documentRepository = documentRepository;
        this.processingLogRepository = processingLogRepository;
        this.fileSummaryRepository = fileSummaryRepository;
        this.cacheService = cacheService;
    }

    /**
     * Returns all parsed data (tables + documents) for a specific file.
     */
    @Transactional(readOnly = true)
    public FileDataResponse getFileData(String orgId, UUID fileId) {
        String cacheKey = cacheService.buildKey(orgId, ENTITY_FILE_DATA, fileId.toString());
        Optional<FileDataResponse> cached = cacheService.getCached(cacheKey, FileDataResponse.class);
        if (cached.isPresent()) {
            log.debug("Cache hit for file data: {}", fileId);
            return cached.get();
        }

        // Get file summary for metadata
        FileSummaryView summary = fileSummaryRepository
                .findByFileIdAndOrgId(fileId, UUID.fromString(orgId))
                .orElse(null);

        String filename = summary != null ? summary.getFilename() : null;
        String mimeType = summary != null ? summary.getMimeType() : null;

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

        FileDataResponse response = new FileDataResponse(fileId, filename, mimeType, tables, documents);
        cacheService.putCache(cacheKey, response);
        return response;
    }

    /**
     * Returns slide content with image URLs for a file.
     * Extracts slide data from documents of type SLIDE_TEXT.
     */
    @Transactional(readOnly = true)
    public SlideDataResponse getSlideData(String orgId, UUID fileId) {
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

        // Get SLIDE_TEXT documents for this file
        List<DocumentEntity> slideDocuments = documentRepository
                .findByFileIdAndDocumentType(fileId.toString(), "SLIDE_TEXT");

        List<SlideDto> slides = slideDocuments.stream()
                .map(this::toSlideDto)
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
     * Returns the processing step timeline for a file.
     */
    @Transactional(readOnly = true)
    public List<ProcessingLogDto> getProcessingLogs(String orgId, String fileId) {
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

    private TableDataDto toTableDto(ParsedTableEntity entity) {
        return new TableDataDto(
                entity.getId(),
                entity.getFileId(),
                entity.getSourceSheet(),
                entity.getHeaders(),
                entity.getRows(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
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
        if (content instanceof Map<?, ?> contentMap) {
            int slideIndex = contentMap.containsKey("slideIndex")
                    ? ((Number) contentMap.get("slideIndex")).intValue() : 0;
            String title = contentMap.containsKey("title") ? (String) contentMap.get("title") : "";
            List<Object> texts = contentMap.containsKey("texts")
                    ? (List<Object>) contentMap.get("texts") : Collections.emptyList();
            List<Object> tables = contentMap.containsKey("tables")
                    ? (List<Object>) contentMap.get("tables") : Collections.emptyList();
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
}
