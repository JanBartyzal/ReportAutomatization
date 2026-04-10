# P12 – Wave 1: Live Excel Export & External Sync – Core Logic (Opus)

**Phase:** P12 – Live Excel Export & External Sync
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~18 MD
**Depends on:** P7 (engine-integrations established, processor-generators:xls exists), P2 (engine-data:query)
**Feature Set:** FS27

> Core business logic for Export Flow management, SharePoint connector, partial sheet update, and event-driven trigger pipeline.

---

## P12-W1-001: engine-integrations:excel-sync – Export Flow Management Service

**Type:** New Module
**Effort:** 5 MD
**Service:** `apps/engine/engine-integrations/excel-sync` (new module)
**Feature Set:** FS27

**Tasks:**
- [ ] **Module scaffolding** (Java 21 + Spring Boot):
  - New module `apps/engine/engine-integrations/excel-sync`
  - Reuse `engine-integrations` deployment unit (same container as servicenow)
  - Spring module registration in main application class
  - Flyway migrations for `export_flow_definitions` and `export_flow_executions` tables
- [ ] **Entity & Repository Layer**:
  - JPA entity `ExportFlowDefinition`:
    - `id` (UUID), `orgId`, `name`, `description`, `sqlQuery`, `targetType` (SHAREPOINT/LOCAL_PATH), `targetPath`, `targetSheet`, `fileNaming` (CUSTOM/BATCH_NAME), `customFileName`, `triggerType` (AUTO/MANUAL), `triggerFilter` (JSONB), `sharepointConfig` (JSONB), `isActive`, `createdBy`, `createdAt`, `updatedAt`
  - JPA entity `ExportFlowExecution`:
    - `id` (UUID), `flowId`, `orgId`, `triggerSource`, `triggerEventId`, `status` (PENDING/RUNNING/SUCCESS/FAILED), `rowsExported`, `targetPathUsed`, `errorMessage`, `startedAt`, `completedAt`
  - Spring Data JPA repositories with RLS-aware queries
- [ ] **REST Controller** (`/api/export-flows`):
  - `GET /api/export-flows` – List flows for current org (paginated, filterable)
  - `GET /api/export-flows/{id}` – Detail with last execution status
  - `POST /api/export-flows` – Create new flow (validate SQL syntax, target reachability)
  - `PUT /api/export-flows/{id}` – Update flow configuration
  - `DELETE /api/export-flows/{id}` – Soft delete (set `isActive = false`)
  - `POST /api/export-flows/{id}/execute` – Manual trigger (async, returns 202)
  - `GET /api/export-flows/{id}/executions` – Execution history (paginated)
  - `POST /api/export-flows/{id}/test` – Dry-run: execute SQL, return preview (max 100 rows, no file write)
- [ ] **Authorization**:
  - `@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")` on create/update/delete/execute
  - `@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")` on read endpoints
  - RLS enforcement via `SET app.current_org_id` before queries
- [ ] **Validation**:
  - SQL query syntax validation (dry-run via engine-data:query)
  - Target path format validation (UNC path regex, SharePoint URL regex)
  - Sheet name validation (max 31 chars, no special chars `[]:*?/\`)
  - File name pattern validation
- [ ] Unit tests: CRUD operations, validation logic, authorization checks (JUnit 5 + Mockito)

**AC:**
- [ ] Admin/Editor can create, read, update, delete Export Flows via REST API
- [ ] Soft delete deactivates flow without data loss
- [ ] Dry-run returns preview data without writing to target
- [ ] RLS ensures org isolation – user cannot see/modify flows of other orgs
- [ ] Invalid SQL, target path, or sheet name returns 400 with clear error message

---

## P12-W1-002: engine-integrations:excel-sync – Event-Driven Trigger Pipeline

**Type:** New Feature
**Effort:** 3 MD
**Service:** `apps/engine/engine-integrations/excel-sync`
**Feature Set:** FS27

**Tasks:**
- [ ] **Dapr PubSub Subscriber**:
  - Subscribe to `data-imported` topic from engine-orchestrator
  - Event payload: `{ batchId, orgId, fileId, sourceType, timestamp }`
  - Filter active Export Flows matching `orgId` and `triggerFilter` criteria
  - For each matching flow: enqueue execution
- [ ] **Execution Engine**:
  - Async execution via `@Async` + thread pool (configurable: `excel-sync.thread-pool-size=4`)
  - Execution workflow:
    1. Create `ExportFlowExecution` record (status: RUNNING)
    2. Execute SQL query via engine-data:query (Dapr gRPC service invocation)
    3. Resolve target file name (custom or batch-name-based)
    4. Fetch existing Excel from target (SharePoint/local) – or create new if not exists
    5. Call processor-generators:xls `UpdateSheet` (Dapr gRPC) with existing Excel binary + sheet name + data
    6. Write updated Excel back to target
    7. Update execution record (status: SUCCESS/FAILED, rowsExported, completedAt)
  - Error handling: catch all exceptions, log to execution record, never fail the main pipeline
- [ ] **Manual Trigger**:
  - `POST /api/export-flows/{id}/execute` creates execution and enqueues immediately
  - Returns `202 Accepted` with `executionId` for polling
- [ ] **Notification Integration**:
  - On SUCCESS/FAILED: publish event to `notify` topic (Dapr PubSub) for engine-reporting:notification
  - Notification payload: `{ type: EXPORT_COMPLETED/EXPORT_FAILED, flowName, targetPath, error }`
- [ ] **Concurrency Guard**:
  - Redis distributed lock per flow ID to prevent concurrent executions of the same flow
  - Lock TTL: 5 minutes (auto-release on timeout)
- [ ] Unit tests: trigger filtering, execution workflow, error scenarios
- [ ] Integration tests: full pipeline with Testcontainers (PostgreSQL + Redis)

**AC:**
- [ ] Data import event triggers matching Export Flows within 60 seconds
- [ ] Manual trigger starts execution immediately and returns execution ID
- [ ] Failed export does not affect main data processing pipeline
- [ ] Concurrent trigger for same flow is rejected (lock)
- [ ] Execution history shows correct status, timing, and row counts

---

## P12-W1-003: engine-integrations:excel-sync – SharePoint Connector

**Type:** New Feature
**Effort:** 4 MD
**Service:** `apps/engine/engine-integrations/excel-sync`
**Feature Set:** FS27

**Tasks:**
- [ ] **Microsoft Graph API Client**:
  - OAuth2 client credentials flow (app-only, no user context):
    - `client_id`, `client_secret`, `tenant_id` from Azure KeyVault
    - Token endpoint: `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token`
    - Scope: `https://graph.microsoft.com/.default`
  - Token caching with auto-refresh (MSAL4J or manual implementation)
- [ ] **File Operations**:
  - **Download**: `GET /drives/{drive-id}/items/{item-id}/content` → byte[]
  - **Upload (replace)**: `PUT /drives/{drive-id}/items/{item-id}/content` ← byte[]
  - **Upload (new file)**: `PUT /drives/{drive-id}/root:/{path}:/content` ← byte[]
  - **File metadata**: `GET /drives/{drive-id}/items/{item-id}` → name, size, lastModified
  - Support for files up to 4MB via simple upload, >4MB via upload session
- [ ] **Path Resolution**:
  - Parse SharePoint URL formats:
    - `https://{tenant}.sharepoint.com/sites/{site}/Shared Documents/{path}`
    - `https://{tenant}.sharepoint.com/:x:/s/{site}/{encoded-item-id}`
  - Resolve to Graph API drive/item identifiers
  - Site discovery: `GET /sites/{tenant}.sharepoint.com:/sites/{site-name}`
  - Drive discovery: `GET /sites/{site-id}/drives`
- [ ] **Error Handling**:
  - `401` → refresh token and retry once
  - `404` → file not found: create new file at target path
  - `409` → conflict: retry with exponential backoff (max 3 retries)
  - `429` → rate limited: respect `Retry-After` header
  - Network errors → log and mark execution as FAILED
- [ ] **Configuration Model** (`sharepointConfig` JSONB):
  ```json
  {
    "tenantId": "...",
    "clientId": "...",
    "secretKeyVaultRef": "keyvault://excel-sync-sp-secret",
    "siteUrl": "https://tenant.sharepoint.com/sites/Finance",
    "driveName": "Documents"
  }
  ```
- [ ] **Connection Test** (reused by `POST /api/export-flows/{id}/test`):
  - Authenticate → list root folder → verify write permission (create temp file, delete)
- [ ] Unit tests with WireMock for Graph API simulation
- [ ] Integration test: upload/download cycle with real SharePoint (optional, env-gated)

**AC:**
- [ ] Download existing Excel from SharePoint → valid byte array
- [ ] Upload modified Excel back to SharePoint → file updated, version incremented
- [ ] New file created at path when target does not exist
- [ ] Authentication failure returns clear error (not generic 500)
- [ ] Rate limiting handled gracefully (retry after delay)
- [ ] Connection test validates both auth and write permissions

---

## P12-W1-004: engine-integrations:excel-sync – Local/Network Path Writer

**Type:** New Feature
**Effort:** 2 MD
**Service:** `apps/engine/engine-integrations/excel-sync`
**Feature Set:** FS27

**Tasks:**
- [ ] **File System Connector**:
  - Read existing Excel: `java.nio.file.Files.readAllBytes(path)`
  - Write updated Excel: atomic write via temp file + rename (`Files.move` with `ATOMIC_MOVE`)
  - Create parent directories if not exist
  - Support for UNC paths (`\\server\share\path\file.xlsx`) and local paths
- [ ] **Path Validation**:
  - Check path is within allowed base directories (configurable whitelist: `excel-sync.allowed-paths`)
  - Reject paths outside whitelist (security: prevent arbitrary file write)
  - Validate write permissions before execution
- [ ] **Docker Volume Mapping**:
  - Document required Docker volume mount: `-v /host/shared:/mnt/exports`
  - Container path prefix: `/mnt/exports/` (configurable)
  - Path translation: user-configured path → container-internal path
- [ ] **File Locking**:
  - `java.nio.channels.FileLock` for concurrent write protection
  - Lock timeout: 30 seconds
  - If file is locked (e.g., opened in Excel by user): retry 3x with 5s interval, then fail gracefully
- [ ] Unit tests: path validation, atomic write, lock handling
- [ ] Integration test: write/read cycle on temp directory

**AC:**
- [ ] Write to local path creates/updates Excel file atomically
- [ ] Write to UNC network path works via mounted volume
- [ ] Path outside allowed whitelist returns 403
- [ ] Locked file (opened by user) → clear error after retry exhaustion
- [ ] Parent directories created automatically if missing

---

## P12-W1-005: processor-generators:xls – Partial Sheet Update (UpdateSheet gRPC)

**Type:** Feature Extension
**Effort:** 4 MD
**Service:** `apps/processor/processor-generators` (extension)
**Feature Set:** FS27

**Tasks:**
- [ ] **Proto Definition** (`packages/protos/generator/v1/excel_service.proto`):
  ```protobuf
  message UpdateSheetRequest {
    bytes excel_binary = 1;      // existing Excel file content (empty = create new)
    string sheet_name = 2;       // target sheet to overwrite
    repeated Row data_rows = 3;  // rows to write
    repeated string headers = 4; // column headers
    SheetFormatting formatting = 5; // optional formatting config
  }

  message Row {
    repeated CellValue cells = 1;
  }

  message CellValue {
    oneof value {
      string string_value = 1;
      double number_value = 2;
      bool bool_value = 3;
      string date_value = 4;     // ISO 8601 format
    }
  }

  message SheetFormatting {
    bool auto_filter = 1;        // apply auto-filter on headers
    bool freeze_header = 2;      // freeze first row
    bool auto_column_width = 3;  // auto-fit column widths
  }

  message UpdateSheetResponse {
    bytes updated_excel = 1;     // updated Excel binary
    int32 rows_written = 2;
    string sheet_name = 3;
  }

  service ExcelGeneratorService {
    rpc UpdateSheet(UpdateSheetRequest) returns (UpdateSheetResponse);
  }
  ```
- [ ] **Implementation** (Python + openpyxl):
  - Load existing workbook from `excel_binary` (`openpyxl.load_workbook(BytesIO(binary))`)
  - If `excel_binary` is empty: create new workbook
  - Target sheet handling:
    - If sheet exists: clear all data (rows + columns) but preserve no other sheets
    - If sheet does not exist: create new sheet
  - **Critical: preserve other sheets** – do NOT delete or modify any sheet other than `sheet_name`
  - Write headers to row 1
  - Write data rows starting from row 2
  - Apply formatting (auto-filter, freeze pane, column width)
  - Data type handling:
    - `string_value` → cell as string
    - `number_value` → cell as number (preserve decimal precision)
    - `date_value` → cell as datetime with date format
    - `bool_value` → cell as boolean
  - Save workbook to BytesIO, return bytes
- [ ] **Preservation Verification**:
  - Verify all non-target sheets remain unchanged (content, formatting, charts, pivot tables)
  - Verify workbook properties (author, title) preserved
  - Verify named ranges in other sheets preserved
  - Verify cross-sheet formulas referencing target sheet: log warning (formulas may break if data structure changes)
- [ ] **Size Limits**:
  - Max input Excel size: 50 MB (configurable)
  - Max output rows: 1,048,576 (Excel row limit)
  - Max output columns: 16,384 (Excel column limit)
- [ ] Unit tests:
  - Update existing sheet in multi-sheet workbook → other sheets unchanged
  - Create new sheet in existing workbook
  - Empty binary → new workbook created
  - Data type mapping (string, number, date, boolean)
  - Formatting options applied correctly
  - Large dataset performance test (10,000+ rows)
- [ ] gRPC health check endpoint

**AC:**
- [ ] `UpdateSheet` with existing Excel overwrites only target sheet – all other sheets 100% preserved
- [ ] Charts, pivot tables, and formulas in non-target sheets remain intact
- [ ] New sheet created when target sheet does not exist in workbook
- [ ] Empty input creates new workbook with single sheet
- [ ] Headers and data types correctly rendered in Excel
- [ ] Processing time for 10,000 rows < 5 seconds

---

## P12-W1-006: engine-orchestrator – PubSub Event `data-imported`

**Type:** Feature Extension
**Effort:** 1 MD
**Service:** `apps/engine/engine-orchestration` (extension)
**Feature Set:** FS27

**Tasks:**
- [ ] **New PubSub Event**:
  - After successful completion of ingestion workflow (all sinks written):
  - Publish event to topic `data-imported` via Dapr PubSub
  - Event payload:
    ```json
    {
      "eventType": "data-imported",
      "batchId": "uuid",
      "orgId": "uuid",
      "fileId": "uuid",
      "fileName": "report.xlsx",
      "sourceType": "EXCEL",
      "processedAt": "2026-04-10T12:00:00Z",
      "tablesStored": 5,
      "correlationId": "trace-id"
    }
    ```
  - Publish AFTER all sinks confirmed (not before) – fire-and-forget semantics
- [ ] **Workflow State Update**:
  - Add `DATA_IMPORTED_EVENT_PUBLISHED` as terminal state marker in workflow log
  - Do NOT block workflow completion on event delivery
- [ ] **Topic Registration**:
  - Add `data-imported` to `TOPICS.md` documentation
  - Dapr PubSub component config for engine-integrations subscription
- [ ] Unit test: verify event published after successful workflow
- [ ] Unit test: verify workflow completes even if PubSub publish fails

**AC:**
- [ ] Successful file processing publishes `data-imported` event with correct payload
- [ ] Failed PubSub publish does not fail the processing workflow
- [ ] Event contains all required fields (batchId, orgId, fileId, sourceType)
- [ ] Topic documented in TOPICS.md

---

## P12-W1-007: UAT Tests – Step26: Live Excel Export & External Sync

**Type:** UAT Tests
**Effort:** 3 MD
**Service:** `tests/UAT/Step26_Excel_Sync/`
**Feature Set:** FS27

**Tasks:**
- [ ] **Test file**: `tests/UAT/Step26_Excel_Sync/test_26_excel_sync.py`
- [ ] **Runner**: `tests/UAT/Step26_Excel_Sync/run.ps1`
- [ ] **Test Scenarios**:

  **26.1 – Export Flow CRUD:**
  - [ ] `POST /api/export-flows` – Create Export Flow with valid SQL, target path, sheet name → 201
  - [ ] `GET /api/export-flows` – List flows → contains created flow
  - [ ] `GET /api/export-flows/{id}` – Detail → matches created data
  - [ ] `PUT /api/export-flows/{id}` – Update sheet name → 200
  - [ ] `DELETE /api/export-flows/{id}` – Soft delete → 200, flow no longer in list

  **26.2 – Authorization & RLS:**
  - [ ] Viewer cannot create Export Flow → 403
  - [ ] admin2 cannot see admin1's Export Flows → empty list
  - [ ] Editor can create and execute Export Flow → 201, 202

  **26.3 – Dry Run (Test):**
  - [ ] `POST /api/export-flows/{id}/test` – Returns preview data without file write → 200
  - [ ] Preview contains correct column headers and row count
  - [ ] Invalid SQL in flow → test returns 400 with SQL error message

  **26.4 – Manual Export Execution:**
  - [ ] `POST /api/export-flows/{id}/execute` – Trigger manual export → 202 with executionId
  - [ ] `GET /api/export-flows/{id}/executions` – Execution appears with status RUNNING → SUCCESS
  - [ ] Execution record contains `rowsExported > 0` and `completedAt`

  **26.5 – Execution History:**
  - [ ] Multiple executions appear in history (ordered by startedAt DESC)
  - [ ] Failed execution shows `errorMessage` (simulate with invalid target path)

  **26.6 – Partial Sheet Update Verification:**
  - [ ] Upload multi-sheet Excel to platform (via engine-ingestor)
  - [ ] Configure Export Flow targeting specific sheet
  - [ ] Execute export → download result from target
  - [ ] Verify: target sheet contains exported data
  - [ ] Verify: other sheets remain unchanged (content hash comparison)

  **26.7 – Concurrent Execution Guard:**
  - [ ] Two simultaneous `POST /api/export-flows/{id}/execute` → second returns 409 (locked)

  **26.8 – Invalid Configurations:**
  - [ ] Export Flow with empty SQL → 400
  - [ ] Export Flow with invalid sheet name (contains `[`) → 400
  - [ ] Export Flow with target path outside allowed whitelist → 400

- [ ] **Test Data**:
  - `tests/UAT/data/export_test_workbook.xlsx` – multi-sheet Excel (3 sheets: Data, Summary, Charts)
  - SQL query referencing seed data from previous UAT steps
- [ ] **Assertions**: Use `UATSession` framework (assert_true, call, missing_feature)
- [ ] **State management**: Store `export_flow_id`, `execution_id` in `uat_state.json`

**AC:**
- [ ] All 8 test groups pass (26.1–26.8)
- [ ] Test covers CRUD, auth, RLS, execution, history, sheet preservation, concurrency
- [ ] Failed scenarios handled gracefully (missing_feature for unimplemented endpoints)
- [ ] Test executable via `run.ps1` standalone and via `run_all.ps1`
