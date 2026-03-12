package com.reportplatform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reportplatform.audit.model.AuditLogEntity;
import com.reportplatform.audit.model.dto.AuditFilterRequest;
import com.reportplatform.audit.model.dto.AuditLogResponse;
import com.reportplatform.audit.model.dto.CreateAuditLogRequest;
import com.reportplatform.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void createAuditLog_shouldSaveEntity() {
        UUID orgId = UUID.randomUUID();
        ObjectNode details = objectMapper.createObjectNode().put("key", "value");

        AuditLogEntity saved = new AuditLogEntity();
        saved.setOrgId(orgId);
        saved.setUserId("user1");
        saved.setAction("DATA_CHANGED");
        saved.setEntityType("TABLE_RECORD");
        saved.setEntityId(UUID.randomUUID());
        saved.setDetails(details.toString());
        saved.setCreatedAt(Instant.now());

        when(auditLogRepository.save(any())).thenReturn(saved);

        var request = new CreateAuditLogRequest(
                orgId, "user1", "DATA_CHANGED", "TABLE_RECORD",
                UUID.randomUUID(), details, "127.0.0.1", "Mozilla");

        AuditLogResponse response = auditLogService.createAuditLog(request);

        assertThat(response.action()).isEqualTo("DATA_CHANGED");
        assertThat(response.userId()).isEqualTo("user1");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void queryLogs_shouldApplyFilters() {
        UUID orgId = UUID.randomUUID();
        var filter = new AuditFilterRequest("user1", "DATA_CHANGED", null, null, null, null);
        var pageable = PageRequest.of(0, 20);

        AuditLogEntity entity = new AuditLogEntity();
        entity.setOrgId(orgId);
        entity.setUserId("user1");
        entity.setAction("DATA_CHANGED");
        entity.setEntityType("TABLE_RECORD");
        entity.setCreatedAt(Instant.now());

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<AuditLogResponse> result = auditLogService.queryLogs(orgId, filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).action()).isEqualTo("DATA_CHANGED");
    }
}
