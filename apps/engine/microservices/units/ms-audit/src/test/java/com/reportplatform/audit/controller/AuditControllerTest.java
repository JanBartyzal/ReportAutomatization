package com.reportplatform.audit.controller;

import com.reportplatform.audit.config.SecurityConfig;
import com.reportplatform.audit.model.dto.AiAuditLogResponse;
import com.reportplatform.audit.model.dto.AuditLogResponse;
import com.reportplatform.audit.model.dto.ReadAccessLogResponse;
import com.reportplatform.audit.service.AiAuditLogService;
import com.reportplatform.audit.service.AuditLogService;
import com.reportplatform.audit.service.ExportService;
import com.reportplatform.audit.service.ReadAccessLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private ReadAccessLogService readAccessLogService;

    @MockBean
    private AiAuditLogService aiAuditLogService;

    @MockBean
    private ExportService exportService;

    @Test
    void getLogs_shouldReturnPaginatedResults() throws Exception {
        UUID orgId = UUID.randomUUID();
        var logs = List.of(
                new AuditLogResponse(UUID.randomUUID(), orgId, "user1",
                        "DATA_CHANGED", "TABLE_RECORD", UUID.randomUUID(),
                        null, "127.0.0.1", Instant.now())
        );

        when(auditLogService.queryLogs(eq(orgId), any(), any()))
                .thenReturn(new PageImpl<>(logs));

        mockMvc.perform(get("/api/audit/logs")
                        .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("DATA_CHANGED"));
    }

    @Test
    void getLogs_withFilters_shouldPassFilters() throws Exception {
        UUID orgId = UUID.randomUUID();

        when(auditLogService.queryLogs(eq(orgId), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/audit/logs")
                        .header("X-Org-Id", orgId.toString())
                        .param("userId", "user1")
                        .param("action", "FILE_UPLOADED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAccessLogs_shouldReturnDocumentAccessHistory() throws Exception {
        UUID documentId = UUID.randomUUID();
        var logs = List.of(
                new ReadAccessLogResponse(UUID.randomUUID(), "user1",
                        documentId, "127.0.0.1", "Mozilla", Instant.now())
        );

        when(readAccessLogService.getAccessHistory(eq(documentId), any()))
                .thenReturn(new PageImpl<>(logs));

        mockMvc.perform(get("/api/audit/access-logs/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value("user1"));
    }

    @Test
    void getAiLogs_shouldReturnAiAuditHistory() throws Exception {
        UUID orgId = UUID.randomUUID();
        var logs = List.of(
                new AiAuditLogResponse(UUID.randomUUID(), "user1",
                        "What is the revenue?", "gpt-4", 150, Instant.now())
        );

        when(aiAuditLogService.getAiAuditHistory(eq(orgId), any()))
                .thenReturn(new PageImpl<>(logs));

        mockMvc.perform(get("/api/audit/ai-logs")
                        .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].model").value("gpt-4"));
    }
}
