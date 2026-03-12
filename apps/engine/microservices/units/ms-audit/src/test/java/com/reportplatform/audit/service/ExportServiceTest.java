package com.reportplatform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.audit.model.AuditLogEntity;
import com.reportplatform.audit.model.dto.AuditFilterRequest;
import com.reportplatform.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(auditLogRepository, new ObjectMapper());
    }

    @Test
    void exportToCsv_shouldProduceValidCsv() throws Exception {
        UUID orgId = UUID.randomUUID();
        AuditLogEntity entity = createSampleEntity(orgId);

        when(auditLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(entity));

        var filter = new AuditFilterRequest(null, null, null, null, null, null);
        var body = exportService.exportLogs(orgId, filter, "csv");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String csv = out.toString();

        assertThat(csv).startsWith("id,org_id,user_id,action,entity_type");
        assertThat(csv).contains("DATA_CHANGED");
        assertThat(csv).contains("user1");
        assertThat(csv.lines().count()).isEqualTo(2); // header + 1 row
    }

    @Test
    void exportToJson_shouldProduceValidJson() throws Exception {
        UUID orgId = UUID.randomUUID();
        AuditLogEntity entity = createSampleEntity(orgId);

        when(auditLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(entity));

        var filter = new AuditFilterRequest(null, null, null, null, null, null);
        var body = exportService.exportLogs(orgId, filter, "json");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        String json = out.toString();

        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        assertThat(json).contains("\"action\":\"DATA_CHANGED\"");
    }

    private AuditLogEntity createSampleEntity(UUID orgId) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOrgId(orgId);
        entity.setUserId("user1");
        entity.setAction("DATA_CHANGED");
        entity.setEntityType("TABLE_RECORD");
        entity.setEntityId(UUID.randomUUID());
        entity.setIpAddress("127.0.0.1");
        entity.setCreatedAt(Instant.now());
        // Set ID via reflection since it's generated
        try {
            var idField = AuditLogEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
