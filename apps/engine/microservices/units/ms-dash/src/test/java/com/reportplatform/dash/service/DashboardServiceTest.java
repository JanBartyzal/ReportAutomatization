package com.reportplatform.dash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.dash.model.DashboardEntity;
import com.reportplatform.dash.model.dto.DashboardRequest;
import com.reportplatform.dash.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    private DashboardService dashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DASHBOARD_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardRepository, objectMapper);
    }

    @Test
    void listDashboards_returnsAccessibleDashboards() {
        var entity = createEntity();
        when(dashboardRepository.findAccessibleDashboards(ORG_ID, USER_ID))
                .thenReturn(List.of(entity));

        var result = dashboardService.listDashboards(ORG_ID, USER_ID);

        assertNotNull(result);
        assertEquals(1, result.dashboards().size());
        assertEquals("Test Dashboard", result.dashboards().get(0).name());
    }

    @Test
    void createDashboard_savesAndReturnsResponse() {
        var request = new DashboardRequest("New Dashboard", "Description",
                Map.of("key", "value"), "bar", false);

        var savedEntity = createEntity();
        when(dashboardRepository.save(any(DashboardEntity.class))).thenReturn(savedEntity);

        var result = dashboardService.createDashboard(ORG_ID, USER_ID, request);

        assertNotNull(result);
        assertEquals("Test Dashboard", result.name());

        ArgumentCaptor<DashboardEntity> captor = ArgumentCaptor.forClass(DashboardEntity.class);
        verify(dashboardRepository).save(captor.capture());
        var captured = captor.getValue();
        assertEquals(ORG_ID, captured.getOrgId());
        assertEquals(USER_ID, captured.getCreatedBy());
        assertEquals("New Dashboard", captured.getName());
        assertEquals("bar", captured.getChartType());
        assertFalse(captured.isPublic());
    }

    @Test
    void getDashboard_existingId_returnsResponse() {
        var entity = createEntity();
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.of(entity));

        var result = dashboardService.getDashboard(DASHBOARD_ID, ORG_ID);

        assertNotNull(result);
        assertEquals(DASHBOARD_ID, result.id());
    }

    @Test
    void getDashboard_nonExistingId_throwsNotFound() {
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> dashboardService.getDashboard(DASHBOARD_ID, ORG_ID));
    }

    @Test
    void updateDashboard_byCreator_updatesSuccessfully() {
        var entity = createEntity();
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.of(entity));
        when(dashboardRepository.save(any(DashboardEntity.class))).thenReturn(entity);

        var request = new DashboardRequest("Updated", "Updated desc",
                Map.of("updated", true), "line", true);

        var result = dashboardService.updateDashboard(DASHBOARD_ID, ORG_ID, USER_ID, request);

        assertNotNull(result);
        verify(dashboardRepository).save(any(DashboardEntity.class));
    }

    @Test
    void updateDashboard_byNonCreator_throwsForbidden() {
        var entity = createEntity();
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.of(entity));

        var otherUser = UUID.randomUUID();
        var request = new DashboardRequest("Updated", null, Map.of(), "bar", false);

        assertThrows(ResponseStatusException.class,
                () -> dashboardService.updateDashboard(DASHBOARD_ID, ORG_ID, otherUser, request));
    }

    @Test
    void deleteDashboard_existingId_deletes() {
        var entity = createEntity();
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.of(entity));

        dashboardService.deleteDashboard(DASHBOARD_ID, ORG_ID);

        verify(dashboardRepository).delete(entity);
    }

    @Test
    void deleteDashboard_nonExistingId_throwsNotFound() {
        when(dashboardRepository.findByIdAndOrgId(DASHBOARD_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> dashboardService.deleteDashboard(DASHBOARD_ID, ORG_ID));
    }

    private DashboardEntity createEntity() {
        var entity = new DashboardEntity();
        entity.setId(DASHBOARD_ID);
        entity.setOrgId(ORG_ID);
        entity.setCreatedBy(USER_ID);
        entity.setName("Test Dashboard");
        entity.setDescription("A test dashboard");
        entity.setConfig("{\"dataSource\":\"parsed_tables\"}");
        entity.setChartType("bar");
        entity.setPublic(false);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
