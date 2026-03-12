package com.reportplatform.qry.controller;

import com.reportplatform.qry.model.dto.DocumentDto;
import com.reportplatform.qry.model.dto.FileDataResponse;
import com.reportplatform.qry.model.dto.ProcessingLogDto;
import com.reportplatform.qry.model.dto.SlideDataResponse;
import com.reportplatform.qry.model.dto.SlideDto;
import com.reportplatform.qry.model.dto.TableDataDto;
import com.reportplatform.qry.model.dto.TableQueryResponse;
import com.reportplatform.qry.service.QueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileQueryController.class)
class FileQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryService queryService;

    private static final String ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";
    private static final UUID FILE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void getFileData_returnsFileDataResponse() throws Exception {
        FileDataResponse response = new FileDataResponse(
                FILE_ID, "report.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                List.of(new TableDataDto(UUID.randomUUID(), FILE_ID.toString(), "Sheet1",
                        List.of("A", "B"), List.of(List.of("1", "2")), null, Instant.now())),
                Collections.emptyList()
        );

        when(queryService.getFileData(ORG_ID, FILE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/query/files/{file_id}/data", FILE_ID)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(FILE_ID.toString()))
                .andExpect(jsonPath("$.filename").value("report.xlsx"))
                .andExpect(jsonPath("$.tables").isArray())
                .andExpect(jsonPath("$.tables.length()").value(1));
    }

    @Test
    void getSlideData_returnsSlideDataResponse() throws Exception {
        SlideDataResponse response = new SlideDataResponse(
                FILE_ID, "presentation.pptx",
                List.of(new SlideDto(0, "Slide 1", List.of("Hello"), Collections.emptyList(),
                        null, "Notes"))
        );

        when(queryService.getSlideData(ORG_ID, FILE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/query/files/{file_id}/slides", FILE_ID)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(FILE_ID.toString()))
                .andExpect(jsonPath("$.slides").isArray())
                .andExpect(jsonPath("$.slides[0].title").value("Slide 1"));
    }

    @Test
    void queryTables_returnsPaginatedResponse() throws Exception {
        TableQueryResponse response = new TableQueryResponse(
                Collections.emptyList(), 0, 20, 0, 0, null
        );

        when(queryService.queryTables(anyString(), anyInt(), anyInt(), isNull(), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/query/tables")
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getDocument_returnsDocumentWhenFound() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentDto dto = new DocumentDto(docId, FILE_ID.toString(), "PDF_PAGE",
                "content", null, Instant.now());

        when(queryService.getDocument(ORG_ID, docId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/query/documents/{document_id}", docId)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.documentType").value("PDF_PAGE"));
    }

    @Test
    void getDocument_returns404WhenNotFound() throws Exception {
        UUID docId = UUID.randomUUID();

        when(queryService.getDocument(ORG_ID, docId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/query/documents/{document_id}", docId)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProcessingLogs_returnsLogTimeline() throws Exception {
        ProcessingLogDto log = new ProcessingLogDto(
                UUID.randomUUID(), FILE_ID.toString(), "wf-123",
                "PARSE", "COMPLETED", 1500L, null, Instant.now()
        );

        when(queryService.getProcessingLogs(ORG_ID, FILE_ID.toString()))
                .thenReturn(List.of(log));

        mockMvc.perform(get("/api/query/processing-logs/{file_id}", FILE_ID)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stepName").value("PARSE"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getFileData_returns400WhenMissingOrgId() throws Exception {
        mockMvc.perform(get("/api/query/files/{file_id}/data", FILE_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest());
    }
}
