package com.reportplatform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.audit.model.AuditLogEntity;
import com.reportplatform.audit.model.dto.AuditFilterRequest;
import com.reportplatform.audit.repository.AuditLogRepository;
import com.reportplatform.audit.repository.AuditLogSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportService.
 * Tests CSV/JSON export formatting.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private ExportService exportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exportService = new ExportService(auditLogRepository, objectMapper);
    }

    // ==================== exportLogs CSV Tests ====================

    @Test
    void exportLogs_csvFormat_generatesValidCsv() throws Exception {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest(null, null, null, null, null, null);

        var auditLog1 = mock(AuditLogEntity.class);
        when(auditLog1.getId()).thenReturn(UUID.randomUUID());
        when(auditLog1.getOrgId()).thenReturn(orgId);
        when(auditLog1.getUserId()).thenReturn("user-1");
        when(auditLog1.getAction()).thenReturn("CREATE");
        when(auditLog1.getEntityType()).thenReturn("REPORT");
        when(auditLog1.getEntityId()).thenReturn(UUID.randomUUID());
        when(auditLog1.getDetails()).thenReturn("Created report");
        when(auditLog1.getIpAddress()).thenReturn("192.168.1.1");
        when(auditLog1.getCreatedAt()).thenReturn(java.time.Instant.now());

        var auditLog2 = mock(AuditLogEntity.class);
        when(auditLog2.getId()).thenReturn(UUID.randomUUID());
        when(auditLog2.getOrgId()).thenReturn(orgId);
        when(auditLog2.getUserId()).thenReturn("user-2");
        when(auditLog2.getAction()).thenReturn("UPDATE");
        when(auditLog2.getEntityType()).thenReturn("REPORT");
        when(auditLog2.getEntityId()).thenReturn(UUID.randomUUID());
        when(auditLog2.getDetails()).thenReturn(null);
        when(auditLog2.getIpAddress()).thenReturn(null);
        when(auditLog2.getCreatedAt()).thenReturn(java.time.Instant.now());

        when(auditLogRepository.findAll(any(AuditLogSpecification.class), any(Sort.class)))
                .thenReturn(List.of(auditLog1, auditLog2));

        // Act
        StreamingResponseBody responseBody = exportService.exportLogs(orgId, filter, "csv");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseBody.writeTo(outputStream);

        String result = outputStream.toString();

        // Assert
        assertTrue(result.contains("id,org_id,user_id,action,entity_type,entity_id,details,ip_address,created_at"));
        assertTrue(result.contains("user-1"));
        assertTrue(result.contains("CREATE"));
        assertTrue(result.contains("user-2"));
        assertTrue(result.contains("UPDATE"));
    }

    // ==================== exportLogs JSON Tests ====================

    @Test
    void exportLogs_jsonFormat_generatesValidJson() throws Exception {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest(null, null, null, null, null, null);

        var auditLog = mock(AuditLogEntity.class);
        when(auditLog.getId()).thenReturn(UUID.randomUUID());
        when(auditLog.getOrgId()).thenReturn(orgId);
        when(auditLog.getUserId()).thenReturn("user-1");
        when(auditLog.getAction()).thenReturn("CREATE");
        when(auditLog.getEntityType()).thenReturn("REPORT");
        when(auditLog.getEntityId()).thenReturn(UUID.randomUUID());
        when(auditLog.getDetails()).thenReturn("Created report");
        when(auditLog.getIpAddress()).thenReturn("192.168.1.1");
        when(auditLog.getCreatedAt()).thenReturn(java.time.Instant.parse("2026-01-15T10:30:00Z"));

        when(auditLogRepository.findAll(any(AuditLogSpecification.class), any(Sort.class)))
                .thenReturn(List.of(auditLog));

        // Act
        StreamingResponseBody responseBody = exportService.exportLogs(orgId, filter, "json");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseBody.writeTo(outputStream);

        String result = outputStream.toString();

        // Assert
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("\"userId\":\"user-1\""));
        assertTrue(result.contains("\"action\":\"CREATE\""));
    }

    // ==================== CSV Escaping Tests ====================

    @Test
    void exportLogs_csvWithSpecialCharacters_escapesProperly() throws Exception {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest(null, null, null, null, null, null);

        var auditLog = mock(AuditLogEntity.class);
        when(auditLog.getId()).thenReturn(UUID.randomUUID());
        when(auditLog.getOrgId()).thenReturn(orgId);
        when(auditLog.getUserId()).thenReturn("user, with, commas");
        when(auditLog.getAction()).thenReturn("CREATE");
        when(auditLog.getEntityType()).thenReturn("REPORT");
        when(auditLog.getEntityId()).thenReturn(UUID.randomUUID());
        when(auditLog.getDetails()).thenReturn("Text with \"quotes\"");
        when(auditLog.getIpAddress()).thenReturn(null);
        when(auditLog.getCreatedAt()).thenReturn(java.time.Instant.now());

        when(auditLogRepository.findAll(any(AuditLogSpecification.class), any(Sort.class)))
                .thenReturn(List.of(auditLog));

        // Act
        StreamingResponseBody responseBody = exportService.exportLogs(orgId, filter, "csv");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseBody.writeTo(outputStream);

        String result = outputStream.toString();

        // Assert - quotes should be escaped
        assertTrue(result.contains("\"user, with, commas\""));
        assertTrue(result.contains("\"Text with \"\"quotes\"\"\""));
    }

    // ==================== Edge Cases Tests ====================

    @Test
    void exportLogs_emptyResults_returnsValidHeaders() throws Exception {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest(null, null, null, null, null, null);

        when(auditLogRepository.findAll(any(AuditLogSpecification.class), any(Sort.class)))
                .thenReturn(List.of());

        // Act
        StreamingResponseBody responseBody = exportService.exportLogs(orgId, filter, "csv");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseBody.writeTo(outputStream);

        String result = outputStream.toString();

        // Assert
        assertTrue(result.contains("id,org_id,user_id,action,entity_type,entity_id,details,ip_address,created_at"));
    }

    @Test
    void exportLogs_defaultFormat_usesJson() throws Exception {
        // Arrange
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest(null, null, null, null, null, null);

        var auditLog = mock(AuditLogEntity.class);
        when(auditLog.getId()).thenReturn(UUID.randomUUID());
        when(auditLog.getOrgId()).thenReturn(orgId);
        when(auditLog.getUserId()).thenReturn("user-1");
        when(auditLog.getAction()).thenReturn("CREATE");
        when(auditLog.getEntityType()).thenReturn("REPORT");
        when(auditLog.getEntityId()).thenReturn(UUID.randomUUID());
        when(auditLog.getDetails()).thenReturn(null);
        when(auditLog.getIpAddress()).thenReturn(null);
        when(auditLog.getCreatedAt()).thenReturn(java.time.Instant.now());

        when(auditLogRepository.findAll(any(AuditLogSpecification.class), any(Sort.class)))
                .thenReturn(List.of(auditLog));

        // Act
        StreamingResponseBody responseBody = exportService.exportLogs(orgId, filter, "xml"); // invalid format defaults
                                                                                             // to json
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        responseBody.writeTo(outputStream);

        String result = outputStream.toString();

        // Assert - should default to JSON
        assertTrue(result.startsWith("["));
    }
}
