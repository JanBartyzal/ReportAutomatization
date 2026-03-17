# Mock Data Audit Report

## Summary

This report identifies locations in the codebase where mock data, hardcoded sample data, or unimplemented features (TODOs) are used instead of real data. These findings are categorized by severity and type.

---

## 1. Production Code - Hardcoded Sample/Fallback Data

### HIGH PRIORITY - Sample Data in Production

#### [`apps/engine/engine-data/query/src/main/java/com/reportplatform/qry/service/ReportDataAggregationService.java`](apps/engine/engine-data/query/src/main/java/com/reportplatform/qry/service/ReportDataAggregationService.java:147)

**Issue**: Hardcoded sample/fallback data used when real data fetch fails

**Code Location**: Lines 147-171

**Data Found**:
```java
// Sample table data as fallback
List<String> tableHeaders = Arrays.asList("Category", "Q1", "Q2", "Q3", "Q4", "Total");
tableRows.add(Arrays.asList("Personnel", "300,000", "310,000", "320,000", "330,000", "1,260,000"));
tableRows.add(Arrays.asList("Materials", "150,000", "155,000", "160,000", "165,000", "630,000"));
tableRows.add(Arrays.asList("Services", "80,000", "85,000", "90,000", "95,000", "350,000"));

// Sample chart data
List<String> chartLabels = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun");
chartSeriesList.add(new ChartSeries("Costs", Arrays.asList(180000.0, 195000.0, 210000.0, 185000.0, 200000.0, 215000.0)));
```

**Recommendation**: Replace with actual data from engine-data:sink-tbl or remove fallback and propagate errors properly.

---

## 2. Production Code - TODOs Indicating Unimplemented Features

### CRITICAL - Unimplemented Service Integrations

#### [`apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/PromotionDetectionService.java`](apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/PromotionDetectionService.java:128)

**Issue**: `fetchHighUsageMappingsFromTmpl()` returns empty list instead of calling ms-tmpl

**Code Location**: Lines 119-129

```java
private List<HighUsageMapping> fetchHighUsageMappingsFromTmpl() {
    // TODO: Implement Dapr service invocation to ms-tmpl
    logger.debug("TODO: Fetch high-usage mappings from ms-tmpl via Dapr (threshold={})", promotionThreshold);
    return Collections.emptyList();  // <-- MOCK: Returns empty instead of real data
}
```

---

#### [`apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/PromotionApprovalService.java`](apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/PromotionApprovalService.java)

**Multiple TODOs** (Lines 174, 182, 234, 287, 329, 399, 423):

| Line | Feature | Status |
|------|---------|--------|
| 174-182 | Call engine-data:sink-tbl to create promoted table | TODO - Not implemented |
| 234-237 | Create promoted table for mapping | TODO - Not implemented |
| 287 | Query engine-data:sink-tbl for dual-write end date | Hardcoded: `"2026-12-31"` |
| 329-335 | Start data migration via Dapr | TODO - Not implemented |
| 399-405 | Migrate data for table | TODO - Not implemented |
| 423 | Query migration progress | TODO - Not implemented |

---

#### [`apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/SchemaProposalGenerator.java`](apps/engine/engine-core/admin/src/main/java/com/reportplatform/admin/service/SchemaProposalGenerator.java:37)

**Issue**: Returns empty-column proposal as placeholder

**Code Location**: Lines 37-38

```java
// TODO: Fetch sample data from ms-tmpl via Dapr service invocation
// For now, return an empty-column proposal as a placeholder
```

---

#### [`apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/DataFetchService.java`](apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/DataFetchService.java:108)

**Issue**: TODO - Data not sent to engine-data:template or engine-data:sink-tbl

```java
// TODO: Send fetched data to engine-data:template via Dapr gRPC for template matching/transformation
// TODO: Send transformed data to engine-data:sink-tbl via Dapr gRPC for persistence
```

---

#### [`apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/SyncJobService.java`](apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/SyncJobService.java:81)

**Issue**: Events not published to Dapr Pub/Sub

```java
// TODO: Publish event to Dapr Pub/Sub (snow.sync.completed)
// TODO: Publish event to Dapr Pub/Sub (snow.sync.failed)
```

---

#### [`apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/DistributionService.java`](apps/engine/engine-integrations/servicenow/src/main/java/com/reportplatform/snow/service/DistributionService.java:133)

**Issue**: Report generation and notification not wired

```java
// TODO: Step 1 - Call processor-generators:xls via Dapr gRPC to generate the report
// TODO: Step 2 - Publish email notification event via Dapr Pub/Sub to engine-reporting:notification
```

---

#### [`apps/engine/engine-data/dashboard/src/main/java/com/reportplatform/dash/service/DashboardPptxService.java`](apps/engine/engine-data/dashboard/src/main/java/com/reportplatform/dash/service/DashboardPptxService.java:219)

**Issue**: Job status tracking not implemented

```java
"UNKNOWN",
"Job status tracking not yet implemented or job not found",
```

---

## 3. Frontend Test Mocks

### INFO - Test Files Only (Not Production)

#### [`apps/frontend/src/test/mocks/handlers.ts`](apps/frontend/src/test/mocks/handlers.ts)

**Purpose**: Mock API handlers for frontend testing

Contains mock data for:
- `mockFiles` (lines 56-87)
- `mockReports` (lines 89-128)
- `mockDashboards` (lines 130-143)
- `mockPeriods` (lines 145-162)
- `mockTemplates` (lines 164-192)

**Status**: These are test mocks, not production code. Should remain for testing purposes.

---

#### [`apps/frontend/src/test/setup.ts`](apps/frontend/src/test/setup.ts)

**Purpose**: Test setup with mocked MSAL, React Query, and other browser APIs

**Status**: Test file only, OK for testing.

---

## 4. Backend Test Files (Mocked Dependencies)

### INFO - Test Files Only

The following files use Mockito `@Mock` annotations for unit testing:

| File | Location |
|------|----------|
| `VersionServiceTest.java` | `apps/engine/engine-core/versioning/src/test/` |
| `BatchControllerTest.java` | `apps/engine/engine-core/batch/src/test/` |
| `TokenValidationServiceTest.java` | `apps/engine/engine-core/auth/src/test/` |
| `RbacServiceTest.java` | `apps/engine/engine-core/auth/src/test/` |
| `ExportServiceTest.java` | `apps/engine/engine-core/audit/src/test/` |
| `AuthControllerTest.java` | `apps/engine/engine-core/auth/src/test/` |
| `OrganizationServiceTest.java` | `apps/engine/engine-core/admin/src/test/` |

**Status**: Test files only, OK for testing.

---

## 5. Python Test Files (Mocked Dependencies)

### INFO - Test Files Only

| File | Location |
|------|----------|
| `test_rls.py` | `apps/processor/processor-generators/src/tests/` |
| `test_mcp_tools.py` | `apps/processor/processor-generators/src/tests/` |
| `test_placeholder_parser.py` | `apps/processor/processor-generators/src/tests/` |
| `test_quota_service.py` | `apps/processor/processor-atomizers/src/tests/` |
| `test_pptx_parser.py` | `apps/processor/processor-atomizers/src/tests/` |
| `test_ai_service.py` | `apps/processor/processor-atomizers/src/tests/` |

**Status**: Test files only, OK for testing.

---

## Summary Statistics

| Category | Count | Priority |
|----------|-------|----------|
| Hardcoded sample data in production | 1 | HIGH |
| Unimplemented service integrations (TODOs) | 10+ | HIGH |
| Frontend test mocks | 2 | INFO |
| Backend test mocks | 7 | INFO |
| Python test mocks | 6 | INFO |

---

## Recommended Actions

1. **HIGH**: Replace hardcoded sample data in `ReportDataAggregationService.java` with proper error handling
2. **HIGH**: Implement Dapr service calls in `PromotionDetectionService.java` and `PromotionApprovalService.java`
3. **HIGH**: Wire up ServiceNow integrations in `DataFetchService.java`, `SyncJobService.java`, and `DistributionService.java`
4. **HIGH**: Implement job status tracking in `DashboardPptxService.java`
5. **INFO**: Frontend and test mocks can remain as-is for testing purposes
