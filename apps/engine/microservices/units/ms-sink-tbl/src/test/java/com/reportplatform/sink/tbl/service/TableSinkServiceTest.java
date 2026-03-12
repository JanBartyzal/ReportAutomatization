package com.reportplatform.sink.tbl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.sink.tbl.entity.FormResponseEntity;
import com.reportplatform.sink.tbl.entity.ParsedTableEntity;
import com.reportplatform.sink.tbl.repository.FormResponseRepository;
import com.reportplatform.sink.tbl.repository.ParsedTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TableSinkService.
 */
@ExtendWith(MockitoExtension.class)
class TableSinkServiceTest {

    @Mock
    private ParsedTableRepository parsedTableRepository;

    @Mock
    private FormResponseRepository formResponseRepository;

    private ObjectMapper objectMapper;

    private TableSinkService tableSinkService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tableSinkService = new TableSinkService(
                parsedTableRepository,
                formResponseRepository,
                objectMapper);
    }

    @Test
    void bulkInsert_shouldInsertRecordsSuccessfully() {
        // Given
        String fileId = "file-123";
        String orgId = "org-456";
        String sourceType = "FILE";

        List<String> headers = List.of("Column A", "Column B", "Column C");
        List<List<String>> rows = List.of(
                List.of("Value 1", "Value 2", "Value 3"),
                List.of("Value 4", "Value 5", "Value 6"));
        Map<String, String> metadata = Map.of("tableIndex", "0");

        TableSinkService.TableRecordData record = new TableSinkService.TableRecordData(
                "record-1",
                "Sheet1",
                headers,
                rows,
                metadata);

        when(parsedTableRepository.save(any(ParsedTableEntity.class)))
                .thenAnswer(invocation -> {
                    ParsedTableEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        int result = tableSinkService.bulkInsert(fileId, orgId, sourceType, List.of(record));

        // Then
        assertEquals(1, result);

        ArgumentCaptor<ParsedTableEntity> captor = ArgumentCaptor.forClass(ParsedTableEntity.class);
        verify(parsedTableRepository, times(1)).save(captor.capture());

        ParsedTableEntity savedEntity = captor.getValue();
        assertEquals(fileId, savedEntity.getFileId());
        assertEquals(orgId, savedEntity.getOrgId());
        assertEquals("Sheet1", savedEntity.getSourceSheet());
    }

    @Test
    void deleteByFileId_shouldDeleteAllRecordsForFile() {
        // Given
        String fileId = "file-123";
        when(parsedTableRepository.deleteByFileId(fileId)).thenReturn(5);

        // When
        int result = tableSinkService.deleteByFileId(fileId);

        // Then
        assertEquals(5, result);
        verify(parsedTableRepository, times(1)).deleteByFileId(fileId);
    }

    @Test
    void storeFormResponse_shouldStoreAllFields() {
        // Given
        String orgId = "org-456";
        String periodId = "period-2026-Q1";
        String formVersionId = "form-v1";

        TableSinkService.FormFieldValueData field1 = new TableSinkService.FormFieldValueData(
                "field-1", "1000", "NUMBER");
        TableSinkService.FormFieldValueData field2 = new TableSinkService.FormFieldValueData(
                "field-2", "some text", "TEXT");

        when(formResponseRepository.save(any(FormResponseEntity.class)))
                .thenAnswer(invocation -> {
                    FormResponseEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        String responseId = tableSinkService.storeFormResponse(
                orgId, periodId, formVersionId, List.of(field1, field2));

        // Then
        assertNotNull(responseId);
        verify(formResponseRepository, times(2)).save(any(FormResponseEntity.class));
    }
}
