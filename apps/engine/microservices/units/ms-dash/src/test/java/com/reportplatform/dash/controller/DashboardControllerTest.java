package com.reportplatform.dash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.DashboardDataResponse;
import com.reportplatform.dash.model.dto.DashboardListResponse;
import com.reportplatform.dash.model.dto.DashboardRequest;
import com.reportplatform.dash.model.dto.DashboardResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonRequest;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse;
import com.reportplatform.dash.model.dto.PeriodComparisonResponse.PeriodComparisonRow;
import com.reportplatform.dash.service.AggregationService;
import com.reportplatform.dash.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private AggregationService aggregationService;

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DASHBOARD_ID = UUID.randomUUID();

    @Test
    void listDashboards_returnsOk() throws Exception {
        var response = new DashboardListResponse(List.of(
                sampleResponse()
        ));
        when(dashboardService.listDashboards(ORG_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/dashboards")
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboards").isArray())
                .andExpect(jsonPath("$.dashboards[0].name").value("Test Dashboard"));
    }

    @Test
    void createDashboard_returnsCreated() throws Exception {
        var request = new DashboardRequest("Test Dashboard", "Description",
                Map.of("dataSource", "parsed_tables"), "bar", false);
        when(dashboardService.createDashboard(eq(ORG_ID), eq(USER_ID), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/dashboards")
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Dashboard"));
    }

    @Test
    void createDashboard_invalidRequest_returnsBadRequest() throws Exception {
        // Missing required fields
        var request = Map.of("description", "no name");

        mockMvc.perform(post("/api/dashboards")
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboard_returnsOk() throws Exception {
        when(dashboardService.getDashboard(DASHBOARD_ID, ORG_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/dashboards/{id}", DASHBOARD_ID)
                        .header("X-Org-Id", ORG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DASHBOARD_ID.toString()));
    }

    @Test
    void updateDashboard_returnsOk() throws Exception {
        var request = new DashboardRequest("Updated", "Updated desc",
                Map.of("dataSource", "parsed_tables"), "line", true);
        var updated = new DashboardResponse(DASHBOARD_ID, ORG_ID, USER_ID,
                "Updated", "Updated desc", Map.of("dataSource", "parsed_tables"),
                "line", true, Instant.now(), Instant.now());
        when(dashboardService.updateDashboard(eq(DASHBOARD_ID), eq(ORG_ID), eq(USER_ID), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/dashboards/{id}", DASHBOARD_ID)
                        .header("X-Org-Id", ORG_ID)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteDashboard_returnsNoContent() throws Exception {
        doNothing().when(dashboardService).deleteDashboard(DASHBOARD_ID, ORG_ID);

        mockMvc.perform(delete("/api/dashboards/{id}", DASHBOARD_ID)
                        .header("X-Org-Id", ORG_ID))
                .andExpect(status().isNoContent());

        verify(dashboardService).deleteDashboard(DASHBOARD_ID, ORG_ID);
    }

    @Test
    void executeDashboardQuery_returnsOk() throws Exception {
        when(dashboardService.getDashboard(DASHBOARD_ID, ORG_ID)).thenReturn(sampleResponse());

        var dataRequest = new DashboardDataRequest(
                List.of("Department"), "SUM", "Amount",
                null, null, null, "ALL");
        var dataResponse = new DashboardDataResponse(
                List.of(Map.of("Department", "Sales", "agg_value", 1500.0)),
                Map.of("rowCount", 1));
        when(aggregationService.executeQuery(eq(ORG_ID), any())).thenReturn(dataResponse);

        mockMvc.perform(post("/api/dashboards/{id}/data", DASHBOARD_ID)
                        .header("X-Org-Id", ORG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dataRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].Department").value("Sales"))
                .andExpect(jsonPath("$.data[0].agg_value").value(1500.0));
    }

    @Test
    void periodComparison_returnsOk() throws Exception {
        var request = new PeriodComparisonRequest(
                "2025-01-01", "2025-03-31",
                "2025-04-01", "2025-06-30",
                List.of("Department"), "SUM", "Amount", "ALL");
        var response = new PeriodComparisonResponse(List.of(
                new PeriodComparisonRow(
                        Map.of("Department", "Sales"),
                        1000.0, 1500.0, 500.0, 50.0)
        ));
        when(aggregationService.comparePeriods(eq(ORG_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/dashboards/period-comparison")
                        .header("X-Org-Id", ORG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].period1Value").value(1000.0))
                .andExpect(jsonPath("$.rows[0].percentageDelta").value(50.0));
    }

    private DashboardResponse sampleResponse() {
        return new DashboardResponse(
                DASHBOARD_ID, ORG_ID, USER_ID,
                "Test Dashboard", "A test dashboard",
                Map.of("dataSource", "parsed_tables"),
                "bar", false,
                Instant.now(), Instant.now()
        );
    }
}
