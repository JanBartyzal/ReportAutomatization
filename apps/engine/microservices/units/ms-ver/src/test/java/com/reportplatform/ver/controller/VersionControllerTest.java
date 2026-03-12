package com.reportplatform.ver.controller;

import com.reportplatform.ver.config.SecurityConfig;
import com.reportplatform.ver.model.dto.FieldChange;
import com.reportplatform.ver.model.dto.VersionDiffResponse;
import com.reportplatform.ver.model.dto.VersionResponse;
import com.reportplatform.ver.model.enums.ChangeType;
import com.reportplatform.ver.service.VersionDiffService;
import com.reportplatform.ver.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersionController.class)
@Import(SecurityConfig.class)
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VersionService versionService;

    @MockBean
    private VersionDiffService versionDiffService;

    @Test
    void listVersions_shouldReturnVersionList() throws Exception {
        UUID entityId = UUID.randomUUID();
        var versions = List.of(
                new VersionResponse(UUID.randomUUID(), "TABLE_RECORD", entityId,
                        2, false, "user1", Instant.now(), null),
                new VersionResponse(UUID.randomUUID(), "TABLE_RECORD", entityId,
                        1, true, "user1", Instant.now(), "Initial")
        );

        when(versionService.listVersions("TABLE_RECORD", entityId)).thenReturn(versions);

        mockMvc.perform(get("/api/versions/TABLE_RECORD/{entityId}", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[1].versionNumber").value(1));
    }

    @Test
    void getDiff_shouldReturnDiffResult() throws Exception {
        UUID entityId = UUID.randomUUID();
        var changes = List.of(
                new FieldChange("name", ChangeType.MODIFIED, "A", "B")
        );
        var diff = new VersionDiffResponse("TABLE_RECORD", entityId, 1, 2, changes);

        when(versionDiffService.getDiff("TABLE_RECORD", entityId, 1, 2)).thenReturn(diff);

        mockMvc.perform(get("/api/versions/TABLE_RECORD/{entityId}/diff", entityId)
                        .param("v1", "1")
                        .param("v2", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromVersion").value(1))
                .andExpect(jsonPath("$.toVersion").value(2))
                .andExpect(jsonPath("$.changes.length()").value(1))
                .andExpect(jsonPath("$.changes[0].changeType").value("MODIFIED"));
    }
}
