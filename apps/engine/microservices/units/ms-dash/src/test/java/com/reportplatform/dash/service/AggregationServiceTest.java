package com.reportplatform.dash.service;

import com.reportplatform.dash.model.dto.DashboardDataRequest;
import com.reportplatform.dash.model.dto.PeriodComparisonRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private AggregationService aggregationService;

    private static final UUID ORG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        aggregationService = new AggregationService(jdbcTemplate);
    }

    @Test
    void executeQuery_validRequest_returnsAggregatedData() {
        var request = new DashboardDataRequest(
                List.of("Department"), "SUM", "Amount",
                null, null, null, "ALL");

        List<Map<String, Object>> mockResult = List.of(
                Map.of("Department", "Sales", "agg_value", 1500.0),
                Map.of("Department", "Marketing", "agg_value", 800.0)
        );
        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(mockResult);

        var result = aggregationService.executeQuery(ORG_ID, request);

        assertNotNull(result);
        assertEquals(2, result.data().size());
        assertEquals("SUM", result.metadata().get("aggregation"));
        assertEquals(2, result.metadata().get("rowCount"));

        // Verify SQL was built and executed
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("SUM"));
        assertTrue(sql.contains("parsed_tables"));
        assertTrue(sql.contains(":orgId"));
    }

    @Test
    void executeQuery_withFilters_includesFilterInSql() {
        var request = new DashboardDataRequest(
                List.of("Department"), "COUNT", "Amount",
                Map.of("Region", "North"), null, null, "FILE");

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(List.of());

        var result = aggregationService.executeQuery(ORG_ID, request);

        assertNotNull(result);
        assertEquals("FILE", result.metadata().get("sourceType"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains(":sourceType"));
        assertTrue(sql.contains(":filter_value_0"));
    }

    @Test
    void executeQuery_multipleGroupBy_buildsCorrectSql() {
        var request = new DashboardDataRequest(
                List.of("Organization", "Period", "CostCenter"),
                "AVG", "Revenue",
                null, null, null, null);

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(List.of());

        var result = aggregationService.executeQuery(ORG_ID, request);

        assertNotNull(result);
        assertEquals(List.of("Organization", "Period", "CostCenter"),
                result.metadata().get("groupBy"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(SqlParameterSource.class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("GROUP BY 1, 2, 3"));
        assertTrue(sql.contains("AVG"));
    }

    @Test
    void executeQuery_invalidAggregation_throwsException() {
        var request = new DashboardDataRequest(
                List.of("Department"), "DROP TABLE", "Amount",
                null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> aggregationService.executeQuery(ORG_ID, request));
    }

    @Test
    void executeQuery_invalidFieldName_throwsException() {
        var request = new DashboardDataRequest(
                List.of("Department; DROP TABLE"), "SUM", "Amount",
                null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> aggregationService.executeQuery(ORG_ID, request));
    }

    @Test
    void executeQuery_blankFieldName_throwsException() {
        var request = new DashboardDataRequest(
                List.of(""), "SUM", "Amount",
                null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> aggregationService.executeQuery(ORG_ID, request));
    }

    @Test
    void comparePeriods_returnsDeltasCorrectly() {
        var request = new PeriodComparisonRequest(
                "2025-01-01", "2025-03-31",
                "2025-04-01", "2025-06-30",
                List.of("Department"), "SUM", "Amount", "ALL");

        // Period 1 results
        List<Map<String, Object>> period1 = List.of(
                Map.of("Department", "Sales", "agg_value", 1000.0)
        );
        // Period 2 results
        List<Map<String, Object>> period2 = List.of(
                Map.of("Department", "Sales", "agg_value", 1500.0)
        );

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(period1)
                .thenReturn(period2);

        var result = aggregationService.comparePeriods(ORG_ID, request);

        assertNotNull(result);
        assertEquals(1, result.rows().size());

        var row = result.rows().get(0);
        assertEquals(1000.0, row.period1Value());
        assertEquals(1500.0, row.period2Value());
        assertEquals(500.0, row.absoluteDelta());
        assertEquals(50.0, row.percentageDelta());
    }

    @Test
    void comparePeriods_zeroPeriod1Value_handlesInfinity() {
        var request = new PeriodComparisonRequest(
                "2025-01-01", "2025-03-31",
                "2025-04-01", "2025-06-30",
                List.of("Department"), "SUM", "Amount", "ALL");

        // Period 1: no results for a group
        List<Map<String, Object>> period1 = List.of();
        List<Map<String, Object>> period2 = List.of(
                Map.of("Department", "NewDept", "agg_value", 500.0)
        );

        when(jdbcTemplate.queryForList(anyString(), any(SqlParameterSource.class)))
                .thenReturn(period1)
                .thenReturn(period2);

        var result = aggregationService.comparePeriods(ORG_ID, request);

        assertNotNull(result);
        assertEquals(1, result.rows().size());

        var row = result.rows().get(0);
        assertEquals(0.0, row.period1Value());
        assertEquals(500.0, row.period2Value());
        assertTrue(Double.isInfinite(row.percentageDelta()));
    }
}
