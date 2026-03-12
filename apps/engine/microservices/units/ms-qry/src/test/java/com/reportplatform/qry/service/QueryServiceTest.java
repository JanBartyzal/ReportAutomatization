package com.reportplatform.qry.service;

import com.reportplatform.qry.model.DocumentEntity;
import com.reportplatform.qry.model.FileSummaryView;
import com.reportplatform.qry.model.ParsedTableEntity;
import com.reportplatform.qry.model.ProcessingLogEntity;
import com.reportplatform.qry.model.dto.DocumentDto;
import com.reportplatform.qry.model.dto.FileDataResponse;
import com.reportplatform.qry.model.dto.ProcessingLogDto;
import com.reportplatform.qry.model.dto.TableQueryResponse;
import com.reportplatform.qry.repository.DocumentRepository;
import com.reportplatform.qry.repository.FileSummaryRepository;
import com.reportplatform.qry.repository.ParsedTableRepository;
import com.reportplatform.qry.repository.ProcessingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private ParsedTableRepository parsedTableRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProcessingLogRepository processingLogRepository;

    @Mock
    private FileSummaryRepository fileSummaryRepository;

    @Mock
    private CacheService cacheService;

    private QueryService queryService;

    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID FILE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        queryService = new QueryService(
                parsedTableRepository,
                documentRepository,
                processingLogRepository,
                fileSummaryRepository,
                cacheService
        );
    }

    @Test
    void getFileData_returnsTablesAndDocuments() {
        // Arrange
        when(cacheService.getCached(anyString(), eq(FileDataResponse.class)))
                .thenReturn(Optional.empty());
        when(cacheService.buildKey(anyString(), anyString(), anyString()))
                .thenReturn("test-key");

        when(fileSummaryRepository.findByFileIdAndOrgId(FILE_ID, UUID.fromString(ORG_ID)))
                .thenReturn(Optional.empty());

        ParsedTableEntity tableEntity = createParsedTable(FILE_ID.toString(), "Sheet1");
        when(parsedTableRepository.findByFileId(FILE_ID.toString()))
                .thenReturn(List.of(tableEntity));

        DocumentEntity docEntity = createDocument(FILE_ID.toString(), "PDF_PAGE");
        when(documentRepository.findByFileId(FILE_ID.toString()))
                .thenReturn(List.of(docEntity));

        // Act
        FileDataResponse response = queryService.getFileData(ORG_ID, FILE_ID);

        // Assert
        assertThat(response.fileId()).isEqualTo(FILE_ID);
        assertThat(response.tables()).hasSize(1);
        assertThat(response.tables().get(0).sourceSheet()).isEqualTo("Sheet1");
        assertThat(response.documents()).hasSize(1);
        assertThat(response.documents().get(0).documentType()).isEqualTo("PDF_PAGE");
    }

    @Test
    void queryTables_returnsPaginatedResults() {
        // Arrange
        ParsedTableEntity tableEntity = createParsedTable(FILE_ID.toString(), "Sheet1");
        Page<ParsedTableEntity> page = new PageImpl<>(List.of(tableEntity));

        when(parsedTableRepository.findByOrgIdOrderByCreatedAtDesc(eq(ORG_ID), any(Pageable.class)))
                .thenReturn(page);

        // Act
        TableQueryResponse response = queryService.queryTables(ORG_ID, 0, 20, null, null);

        // Assert
        assertThat(response.tables()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(0);
    }

    @Test
    void queryTables_withSourceSheetFilter() {
        // Arrange
        ParsedTableEntity tableEntity = createParsedTable(FILE_ID.toString(), "OPEX Data");
        Page<ParsedTableEntity> page = new PageImpl<>(List.of(tableEntity));

        when(parsedTableRepository
                .findByOrgIdAndSourceSheetContainingIgnoreCaseOrderByCreatedAtDesc(
                        eq(ORG_ID), eq("OPEX"), any(Pageable.class)))
                .thenReturn(page);

        // Act
        TableQueryResponse response = queryService.queryTables(ORG_ID, 0, 20, "OPEX", null);

        // Assert
        assertThat(response.tables()).hasSize(1);
        assertThat(response.tables().get(0).sourceSheet()).isEqualTo("OPEX Data");
    }

    @Test
    void getDocument_returnsDocumentWhenFound() {
        // Arrange
        UUID docId = UUID.randomUUID();
        when(cacheService.getCached(anyString(), eq(DocumentDto.class)))
                .thenReturn(Optional.empty());
        when(cacheService.buildKey(anyString(), anyString(), anyString()))
                .thenReturn("test-key");

        DocumentEntity docEntity = createDocument(FILE_ID.toString(), "SLIDE_TEXT");
        setField(docEntity, "id", docId);

        when(documentRepository.findByIdAndOrgId(docId, ORG_ID))
                .thenReturn(Optional.of(docEntity));

        // Act
        Optional<DocumentDto> result = queryService.getDocument(ORG_ID, docId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().documentType()).isEqualTo("SLIDE_TEXT");
    }

    @Test
    void getDocument_returnsEmptyWhenNotFound() {
        // Arrange
        UUID docId = UUID.randomUUID();
        when(cacheService.getCached(anyString(), eq(DocumentDto.class)))
                .thenReturn(Optional.empty());
        when(cacheService.buildKey(anyString(), anyString(), anyString()))
                .thenReturn("test-key");

        when(documentRepository.findByIdAndOrgId(docId, ORG_ID))
                .thenReturn(Optional.empty());

        // Act
        Optional<DocumentDto> result = queryService.getDocument(ORG_ID, docId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getProcessingLogs_returnsOrderedLogs() {
        // Arrange
        when(cacheService.getCached(anyString(), eq(List.class)))
                .thenReturn(Optional.empty());
        when(cacheService.buildKey(anyString(), anyString(), anyString()))
                .thenReturn("test-key");

        ProcessingLogEntity log1 = createProcessingLog("UPLOAD", "COMPLETED");
        ProcessingLogEntity log2 = createProcessingLog("PARSE", "STARTED");

        when(processingLogRepository.findByFileIdOrderByCreatedAtAsc(FILE_ID.toString()))
                .thenReturn(List.of(log1, log2));

        // Act
        List<ProcessingLogDto> logs = queryService.getProcessingLogs(ORG_ID, FILE_ID.toString());

        // Assert
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).stepName()).isEqualTo("UPLOAD");
        assertThat(logs.get(1).stepName()).isEqualTo("PARSE");
    }

    // ---- Helper methods ----

    private ParsedTableEntity createParsedTable(String fileId, String sourceSheet) {
        ParsedTableEntity entity = new ParsedTableEntity();
        setField(entity, "id", UUID.randomUUID());
        setField(entity, "fileId", fileId);
        setField(entity, "orgId", ORG_ID);
        setField(entity, "sourceSheet", sourceSheet);
        setField(entity, "headers", List.of("Col1", "Col2"));
        setField(entity, "rows", List.of(List.of("A", "B")));
        setField(entity, "metadata", Collections.emptyMap());
        setField(entity, "createdAt", Instant.now());
        return entity;
    }

    private DocumentEntity createDocument(String fileId, String documentType) {
        DocumentEntity entity = new DocumentEntity();
        setField(entity, "id", UUID.randomUUID());
        setField(entity, "fileId", fileId);
        setField(entity, "orgId", ORG_ID);
        setField(entity, "documentType", documentType);
        setField(entity, "content", "test content");
        setField(entity, "metadata", Collections.emptyMap());
        setField(entity, "createdAt", Instant.now());
        return entity;
    }

    private ProcessingLogEntity createProcessingLog(String stepName, String status) {
        ProcessingLogEntity entity = new ProcessingLogEntity();
        setField(entity, "id", UUID.randomUUID());
        setField(entity, "fileId", FILE_ID.toString());
        setField(entity, "workflowId", "wf-123");
        setField(entity, "stepName", stepName);
        setField(entity, "status", status);
        setField(entity, "durationMs", 1000L);
        setField(entity, "createdAt", Instant.now());
        return entity;
    }

    /**
     * Sets a private field on an entity using reflection.
     * Needed because entities are read-only (no setters).
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
