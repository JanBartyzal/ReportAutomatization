# P0 – API Contracts & Proto Definitions

**Version:** 1.0
**Date:** 2026-03-09
**Goal:** Define all gRPC `.proto` files and REST OpenAPI contracts BEFORE any implementation begins. This phase creates the communication backbone for the entire platform.

**Principle:** Internal services communicate via Dapr gRPC. External (frontend-facing) services expose REST via API Gateway. Proto files live in `packages/protos/`, OpenAPI specs in `docs/api/`.

---

## Wave 1 – Core Proto Definitions (Opus)

> Complex service contracts requiring deep architectural understanding. Each proto must anticipate future extensions without breaking backward compatibility.

---

### P0-W1-001: Proto Package Structure & Build Setup

**Type:** Infrastructure
**Effort:** 1 MD

**Description:** Set up the protobuf compilation pipeline in `packages/protos/`.

**Tasks:**
- [ ] Create `packages/protos/buf.yaml` (or `Makefile`) for proto compilation
- [ ] Directory structure:
  ```
  packages/protos/
  ├── buf.yaml
  ├── buf.gen.yaml          # codegen config (Java + Python)
  ├── common/
  │   └── v1/
  │       ├── common.proto       # shared types
  │       └── error.proto        # standard error envelope
  ├── orchestrator/
  │   └── v1/
  │       └── orchestrator.proto
  ├── atomizer/
  │   └── v1/
  │       ├── pptx.proto
  │       ├── excel.proto
  │       ├── pdf.proto
  │       ├── csv.proto
  │       └── ai.proto
  ├── sink/
  │   └── v1/
  │       ├── table.proto
  │       ├── document.proto
  │       └── log.proto
  ├── template/
  │   └── v1/
  │       └── template.proto
  ├── scanner/
  │   └── v1/
  │       └── scanner.proto
  ├── lifecycle/
  │   └── v1/
  │       └── lifecycle.proto
  ├── form/
  │   └── v1/
  │       └── form.proto
  ├── period/
  │   └── v1/
  │       └── period.proto
  ├── notification/
  │   └── v1/
  │       └── notification.proto
  └── generator/
      └── v1/
          └── pptx_generator.proto
  ```
- [ ] `buf.gen.yaml` for Java (protobuf-java + grpc-java) and Python (grpcio-tools)
- [ ] Script `scripts/proto-gen.sh` to compile all protos
- [ ] `.gitignore` for generated code (`gen/`)
- [ ] Verify compilation produces valid Java and Python stubs

**AC:**
- [ ] `./scripts/proto-gen.sh` runs without errors
- [ ] Generated Java classes available in `packages/protos/gen/java/`
- [ ] Generated Python stubs available in `packages/protos/gen/python/`

---

### P0-W1-002: Common Proto Types

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/common/v1/common.proto`

```protobuf
syntax = "proto3";
package common.v1;

option java_package = "com.reportplatform.proto.common.v1";
option java_multiple_files = true;

// Standard request metadata propagated through all internal calls
message RequestContext {
  string trace_id = 1;
  string user_id = 2;
  string org_id = 3;
  repeated string roles = 4;
  string correlation_id = 5;
}

// Standard pagination
message PaginationRequest {
  int32 page = 1;
  int32 page_size = 2;
}

message PaginationResponse {
  int32 page = 1;
  int32 page_size = 2;
  int32 total_items = 3;
  int32 total_pages = 4;
}

// File reference (never inline binary)
message BlobReference {
  string blob_url = 1;
  string content_type = 2;
  int64 size_bytes = 3;
}

// Standard timestamps
message Timestamps {
  string created_at = 1;  // ISO 8601
  string updated_at = 2;
}

// Processing status enum used across services
enum ProcessingStatus {
  PROCESSING_STATUS_UNSPECIFIED = 0;
  PROCESSING_STATUS_PENDING = 1;
  PROCESSING_STATUS_IN_PROGRESS = 2;
  PROCESSING_STATUS_COMPLETED = 3;
  PROCESSING_STATUS_FAILED = 4;
  PROCESSING_STATUS_PARTIAL = 5;
}
```

**File:** `packages/protos/common/v1/error.proto`

```protobuf
syntax = "proto3";
package common.v1;

option java_package = "com.reportplatform.proto.common.v1";
option java_multiple_files = true;

// Standard gRPC error detail
message ErrorDetail {
  string code = 1;          // e.g., "PARSING_FAILED", "INFECTED"
  string message = 2;
  map<string, string> metadata = 3;
}
```

**AC:**
- [ ] All internal services import `common.v1` for shared types
- [ ] No service defines its own `RequestContext` – always reuse common

---

### P0-W1-003: Orchestrator Proto (MS-ORCH)

**Type:** Proto Definition
**Effort:** 1 MD

**File:** `packages/protos/orchestrator/v1/orchestrator.proto`

```protobuf
syntax = "proto3";
package orchestrator.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.orchestrator.v1";
option java_multiple_files = true;

// MS-ORCH: Workflow Engine
// Called via Dapr Pub/Sub (file-uploaded event) and gRPC (manual trigger)

service OrchestratorService {
  // Trigger processing workflow for a file
  rpc StartFileWorkflow(StartFileWorkflowRequest) returns (StartFileWorkflowResponse);

  // Get workflow status
  rpc GetWorkflowStatus(GetWorkflowStatusRequest) returns (WorkflowStatusResponse);

  // Retry a failed workflow
  rpc RetryWorkflow(RetryWorkflowRequest) returns (StartFileWorkflowResponse);

  // Cancel a running workflow
  rpc CancelWorkflow(CancelWorkflowRequest) returns (CancelWorkflowResponse);

  // List failed jobs (DLQ)
  rpc ListFailedJobs(ListFailedJobsRequest) returns (ListFailedJobsResponse);

  // Reprocess a failed job
  rpc ReprocessFailedJob(ReprocessFailedJobRequest) returns (StartFileWorkflowResponse);
}

// --- Requests ---

message StartFileWorkflowRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string file_type = 3;      // "PPTX", "XLSX", "PDF", "CSV"
  string org_id = 4;
  string blob_url = 5;
  string upload_purpose = 6;  // "PARSE" or "FORM_IMPORT"
}

message GetWorkflowStatusRequest {
  common.v1.RequestContext context = 1;
  string workflow_id = 2;
}

message RetryWorkflowRequest {
  common.v1.RequestContext context = 1;
  string workflow_id = 2;
}

message CancelWorkflowRequest {
  common.v1.RequestContext context = 1;
  string workflow_id = 2;
}

message ListFailedJobsRequest {
  common.v1.RequestContext context = 1;
  common.v1.PaginationRequest pagination = 2;
  string org_id = 3;  // optional filter
}

message ReprocessFailedJobRequest {
  common.v1.RequestContext context = 1;
  string failed_job_id = 2;
}

// --- Responses ---

message StartFileWorkflowResponse {
  string workflow_id = 1;
  string status = 2;  // "STARTED", "ALREADY_RUNNING", "QUEUED"
}

message WorkflowStatusResponse {
  string workflow_id = 1;
  string file_id = 2;
  string status = 3;  // "RUNNING", "COMPLETED", "FAILED", "CANCELLED"
  repeated WorkflowStep steps = 4;
  string started_at = 5;
  string completed_at = 6;
  string error_detail = 7;
}

message WorkflowStep {
  string step_name = 1;
  string status = 2;
  int64 duration_ms = 3;
  string error_detail = 4;
  string started_at = 5;
  string completed_at = 6;
}

message CancelWorkflowResponse {
  bool cancelled = 1;
}

message ListFailedJobsResponse {
  repeated FailedJob jobs = 1;
  common.v1.PaginationResponse pagination = 2;
}

message FailedJob {
  string id = 1;
  string file_id = 2;
  string workflow_id = 3;
  string error_type = 4;
  string error_detail = 5;
  string failed_at = 6;
  int32 retry_count = 7;
}

// --- Dapr Pub/Sub Events ---

// Published by MS-ING on topic "file-uploaded"
message FileUploadedEvent {
  string file_id = 1;
  string file_type = 2;
  string org_id = 3;
  string user_id = 4;
  string blob_url = 5;
  string upload_purpose = 6;
  string uploaded_at = 7;
}

// Published by MS-ORCH on topic "processing-completed"
message ProcessingCompletedEvent {
  string file_id = 1;
  string workflow_id = 2;
  string org_id = 3;
  common.v1.ProcessingStatus status = 4;
  string completed_at = 5;
}
```

**AC:**
- [ ] Covers full workflow lifecycle: start, status, retry, cancel, DLQ
- [ ] Saga Pattern compensating actions modeled via CancelWorkflow
- [ ] Pub/Sub event schemas defined for file-uploaded and processing-completed

---

### P0-W1-004: Atomizer Protos (MS-ATM-*)

**Type:** Proto Definition
**Effort:** 2 MD

**File:** `packages/protos/atomizer/v1/pptx.proto`

```protobuf
syntax = "proto3";
package atomizer.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.atomizer.v1";
option java_multiple_files = true;

service PptxAtomizerService {
  // Extract PPTX structure (slide list, metadata)
  rpc ExtractStructure(ExtractRequest) returns (PptxStructureResponse);

  // Extract content from a specific slide
  rpc ExtractSlideContent(SlideRequest) returns (SlideContentResponse);

  // Render slide as PNG image
  rpc RenderSlideImage(SlideRequest) returns (SlideImageResponse);

  // Batch extract all slides (orchestrator typically calls this)
  rpc ExtractAll(ExtractRequest) returns (PptxFullExtractionResponse);
}

message ExtractRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string blob_url = 3;
}

message SlideRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string blob_url = 3;
  int32 slide_index = 4;
}

message PptxStructureResponse {
  string file_id = 1;
  int32 total_slides = 2;
  repeated SlideMetadata slides = 3;
  map<string, string> document_properties = 4;  // author, title, etc.
}

message SlideMetadata {
  int32 slide_index = 1;
  string title = 2;
  string layout_name = 3;
  bool has_tables = 4;
  bool has_text = 5;
  bool has_images = 6;
  bool has_charts = 7;
  bool has_notes = 8;
}

message SlideContentResponse {
  int32 slide_index = 1;
  repeated TextBlock texts = 2;
  repeated TableData tables = 3;
  string notes = 4;
}

message TextBlock {
  string shape_name = 1;
  string text = 2;
  bool is_title = 3;
  int32 position_x = 4;
  int32 position_y = 5;
}

message TableData {
  string table_id = 1;
  repeated string headers = 2;
  repeated TableRow rows = 3;
  double confidence = 4;  // MetaTable confidence threshold
}

message TableRow {
  repeated string cells = 1;
}

message SlideImageResponse {
  int32 slide_index = 1;
  common.v1.BlobReference image = 2;  // PNG URL in Blob Storage
}

message PptxFullExtractionResponse {
  string file_id = 1;
  common.v1.ProcessingStatus status = 2;
  PptxStructureResponse structure = 3;
  repeated SlideContentResponse slide_contents = 4;
  repeated SlideImageResponse slide_images = 5;
  repeated ExtractionError errors = 6;
}

message ExtractionError {
  int32 slide_index = 1;
  string error_code = 2;
  string error_message = 3;
}
```

**File:** `packages/protos/atomizer/v1/excel.proto`

```protobuf
syntax = "proto3";
package atomizer.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.atomizer.v1";
option java_multiple_files = true;

service ExcelAtomizerService {
  rpc ExtractStructure(ExtractRequest) returns (ExcelStructureResponse);
  rpc ExtractSheetContent(SheetRequest) returns (SheetContentResponse);
  rpc ExtractAll(ExtractRequest) returns (ExcelFullExtractionResponse);
}

message SheetRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string blob_url = 3;
  int32 sheet_index = 4;
}

message ExcelStructureResponse {
  string file_id = 1;
  repeated SheetMetadata sheets = 2;
}

message SheetMetadata {
  int32 sheet_index = 1;
  string name = 2;
  int32 row_count = 3;
  int32 col_count = 4;
  bool has_merged_cells = 5;
}

message SheetContentResponse {
  int32 sheet_index = 1;
  string sheet_name = 2;
  repeated string headers = 3;
  repeated SheetRow rows = 4;
  repeated ColumnDataType data_types = 5;
}

message SheetRow {
  int32 row_index = 1;
  repeated string cells = 2;
}

message ColumnDataType {
  int32 col_index = 1;
  string column_name = 2;
  string detected_type = 3;  // STRING, NUMBER, DATE, CURRENCY, PERCENTAGE
}

message ExcelFullExtractionResponse {
  string file_id = 1;
  common.v1.ProcessingStatus status = 2;  // COMPLETED or PARTIAL
  repeated SheetContentResponse successful_sheets = 3;
  repeated SheetExtractionError failed_sheets = 4;
}

message SheetExtractionError {
  int32 sheet_index = 1;
  string sheet_name = 2;
  string error_code = 3;
  string error_message = 4;
}
```

**File:** `packages/protos/atomizer/v1/pdf.proto`

```protobuf
syntax = "proto3";
package atomizer.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.atomizer.v1";
option java_multiple_files = true;

service PdfAtomizerService {
  rpc ExtractPdf(ExtractRequest) returns (PdfExtractionResponse);
}

message PdfExtractionResponse {
  string file_id = 1;
  common.v1.ProcessingStatus status = 2;
  string detection_method = 3;  // "TEXT_LAYER" or "OCR"
  int32 total_pages = 4;
  repeated PdfPageContent pages = 5;
}

message PdfPageContent {
  int32 page_number = 1;
  string text = 2;
  repeated TableData tables = 3;
  bool was_ocr = 4;
  double ocr_confidence = 5;
}
```

**File:** `packages/protos/atomizer/v1/csv.proto`

```protobuf
syntax = "proto3";
package atomizer.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.atomizer.v1";
option java_multiple_files = true;

service CsvAtomizerService {
  rpc ExtractCsv(ExtractRequest) returns (CsvExtractionResponse);
}

message CsvExtractionResponse {
  string file_id = 1;
  common.v1.ProcessingStatus status = 2;
  string detected_delimiter = 3;   // ",", ";", "|", "\t"
  string detected_encoding = 4;     // "UTF-8", "Windows-1250", etc.
  int32 total_rows = 5;
  repeated string headers = 6;
  repeated SheetRow rows = 7;       // reuse from excel.proto
  repeated ColumnDataType data_types = 8;
}
```

**File:** `packages/protos/atomizer/v1/ai.proto`

```protobuf
syntax = "proto3";
package atomizer.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.atomizer.v1";
option java_multiple_files = true;

service AiGatewayService {
  // Semantic analysis of extracted text
  rpc AnalyzeSemantic(SemanticRequest) returns (SemanticResponse);

  // Generate vector embeddings for document
  rpc GenerateEmbeddings(EmbeddingRequest) returns (EmbeddingResponse);

  // Data cleaning suggestion
  rpc SuggestCleaning(CleaningRequest) returns (CleaningResponse);
}

message SemanticRequest {
  common.v1.RequestContext context = 1;
  string text = 2;
  string analysis_type = 3;  // "CLASSIFY", "SUMMARIZE", "EXTRACT_ENTITIES"
  map<string, string> parameters = 4;
}

message SemanticResponse {
  string result = 1;
  map<string, string> entities = 2;
  string classification = 3;
  int32 tokens_used = 4;
  int32 tokens_remaining = 5;
}

message EmbeddingRequest {
  common.v1.RequestContext context = 1;
  string document_id = 2;
  string text = 3;
}

message EmbeddingResponse {
  string document_id = 1;
  repeated float embedding = 2;  // 1536 dimensions
  int32 tokens_used = 3;
}

message CleaningRequest {
  common.v1.RequestContext context = 1;
  repeated string headers = 2;
  repeated SheetRow sample_rows = 3;  // first 10 rows for analysis
}

message CleaningResponse {
  repeated ColumnSuggestion suggestions = 1;
  int32 tokens_used = 2;
}

message ColumnSuggestion {
  int32 col_index = 1;
  string original_name = 2;
  string suggested_name = 3;
  string suggested_type = 4;
  double confidence = 5;
}
```

**AC:**
- [ ] All atomizers follow same `ExtractRequest` input pattern
- [ ] All binary outputs use `BlobReference` (never inline)
- [ ] Partial success modeled for Excel (PARTIAL status)
- [ ] AI gateway tracks token usage in every response

---

### P0-W1-005: Sink Protos (MS-SINK-*)

**Type:** Proto Definition
**Effort:** 1 MD

**File:** `packages/protos/sink/v1/table.proto`

```protobuf
syntax = "proto3";
package sink.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.sink.v1";
option java_multiple_files = true;

service TableSinkService {
  // Bulk insert structured data (tables, OPEX)
  rpc BulkInsert(BulkInsertRequest) returns (BulkInsertResponse);

  // Delete by file_id (Saga compensating action)
  rpc DeleteByFileId(DeleteByFileIdRequest) returns (DeleteResponse);

  // Store form response data
  rpc StoreFormResponse(StoreFormResponseRequest) returns (StoreFormResponseResponse);
}

message BulkInsertRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string org_id = 3;
  string source_type = 4;  // "FILE" or "FORM"
  repeated TableRecord records = 5;
}

message TableRecord {
  string record_id = 1;
  string source_sheet = 2;   // sheet name or slide index
  repeated string headers = 3;
  repeated TableRecordRow rows = 4;
  map<string, string> metadata = 5;
}

message TableRecordRow {
  repeated string cells = 1;
}

message BulkInsertResponse {
  int32 records_inserted = 1;
  common.v1.ProcessingStatus status = 2;
}

message DeleteByFileIdRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
}

message DeleteResponse {
  int32 records_deleted = 1;
}

message StoreFormResponseRequest {
  common.v1.RequestContext context = 1;
  string org_id = 2;
  string period_id = 3;
  string form_version_id = 4;
  repeated FormFieldValue fields = 5;
}

message FormFieldValue {
  string field_id = 1;
  string value = 2;
  string data_type = 3;
}

message StoreFormResponseResponse {
  string response_id = 1;
  string submitted_at = 2;
}
```

**File:** `packages/protos/sink/v1/document.proto`

```protobuf
syntax = "proto3";
package sink.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.sink.v1";
option java_multiple_files = true;

service DocumentSinkService {
  // Store unstructured document (text, notes, etc.)
  rpc StoreDocument(StoreDocumentRequest) returns (StoreDocumentResponse);

  // Delete by file_id (Saga compensating action)
  rpc DeleteByFileId(DeleteByFileIdRequest) returns (DeleteResponse);
}

message StoreDocumentRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string org_id = 3;
  string document_type = 4;  // "SLIDE_TEXT", "PDF_PAGE", "NOTES"
  string content = 5;         // JSON text content
  map<string, string> metadata = 6;
}

message StoreDocumentResponse {
  string document_id = 1;
  bool embedding_queued = 2;  // true if sent to MS-ATM-AI for embedding
}
```

**File:** `packages/protos/sink/v1/log.proto`

```protobuf
syntax = "proto3";
package sink.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.sink.v1";
option java_multiple_files = true;

service LogSinkService {
  // Append processing log entry (append-only)
  rpc AppendLog(AppendLogRequest) returns (AppendLogResponse);

  // Batch append (multiple steps at once)
  rpc BatchAppendLog(BatchAppendLogRequest) returns (AppendLogResponse);
}

message AppendLogRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string workflow_id = 3;
  string step_name = 4;
  string status = 5;          // "STARTED", "COMPLETED", "FAILED"
  int64 duration_ms = 6;
  string error_detail = 7;
  map<string, string> metadata = 8;
}

message BatchAppendLogRequest {
  repeated AppendLogRequest entries = 1;
}

message AppendLogResponse {
  string log_id = 1;
  string recorded_at = 2;
}
```

**AC:**
- [ ] All sinks are write-optimized (no read endpoints – reading via MS-QRY)
- [ ] DeleteByFileId present on all sinks for Saga rollback
- [ ] Form response storage integrated into table sink

---

### P0-W1-006: Scanner Proto (MS-SCAN)

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/scanner/v1/scanner.proto`

```protobuf
syntax = "proto3";
package scanner.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.scanner.v1";
option java_multiple_files = true;

service ScannerService {
  // Scan file for viruses via ClamAV
  rpc ScanFile(ScanFileRequest) returns (ScanFileResponse);

  // Sanitize file (remove macros, external links)
  rpc SanitizeFile(SanitizeFileRequest) returns (SanitizeFileResponse);
}

message ScanFileRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string blob_url = 3;
}

message ScanFileResponse {
  string file_id = 1;
  ScanResult result = 2;
  string threat_name = 3;  // empty if clean
  int64 scan_duration_ms = 4;
}

enum ScanResult {
  SCAN_RESULT_UNSPECIFIED = 0;
  SCAN_RESULT_CLEAN = 1;
  SCAN_RESULT_INFECTED = 2;
  SCAN_RESULT_ERROR = 3;
}

message SanitizeFileRequest {
  common.v1.RequestContext context = 1;
  string file_id = 2;
  string blob_url = 3;
  string mime_type = 4;
}

message SanitizeFileResponse {
  string file_id = 1;
  common.v1.BlobReference sanitized_file = 2;
  repeated string removed_items = 3;  // ["VBA_MACROS", "EXTERNAL_LINKS"]
}
```

---

### P0-W1-007: Template & Schema Mapping Proto (MS-TMPL)

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/template/v1/template.proto`

```protobuf
syntax = "proto3";
package template.v1;

import "common/v1/common.proto";
import "atomizer/v1/excel.proto";

option java_package = "com.reportplatform.proto.template.v1";
option java_multiple_files = true;

service TemplateMappingService {
  // Apply schema mapping to extracted data
  rpc ApplyMapping(ApplyMappingRequest) returns (ApplyMappingResponse);

  // Suggest mapping based on headers (AI-assisted learning)
  rpc SuggestMapping(SuggestMappingRequest) returns (SuggestMappingResponse);

  // Map Excel columns to form fields (FS19 import)
  rpc MapExcelToForm(MapExcelToFormRequest) returns (MapExcelToFormResponse);
}

message ApplyMappingRequest {
  common.v1.RequestContext context = 1;
  string template_id = 2;
  repeated string source_headers = 3;
  repeated atomizer.v1.SheetRow rows = 4;
}

message ApplyMappingResponse {
  repeated string mapped_headers = 1;
  repeated atomizer.v1.SheetRow mapped_rows = 2;
  repeated MappingAction applied_mappings = 3;
}

message MappingAction {
  string source_column = 1;
  string target_column = 2;
  string rule = 3;  // "EXACT_MATCH", "SYNONYM", "AI_SUGGESTED"
  double confidence = 4;
}

message SuggestMappingRequest {
  common.v1.RequestContext context = 1;
  string org_id = 2;
  repeated string source_headers = 3;
}

message SuggestMappingResponse {
  repeated MappingAction suggestions = 1;
}

message MapExcelToFormRequest {
  common.v1.RequestContext context = 1;
  string form_id = 2;
  string form_version_id = 3;
  repeated string excel_headers = 4;
  repeated atomizer.v1.SheetRow sample_rows = 5;
}

message MapExcelToFormResponse {
  repeated FormFieldMapping mappings = 1;
  repeated string unmapped_excel_columns = 2;
  repeated string unmapped_form_fields = 3;
}

message FormFieldMapping {
  string excel_column = 1;
  string form_field_id = 2;
  string form_field_name = 3;
  double confidence = 4;
}
```

---

### P0-W1-008: Lifecycle Proto (MS-LIFECYCLE)

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/lifecycle/v1/lifecycle.proto`

```protobuf
syntax = "proto3";
package lifecycle.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.lifecycle.v1";
option java_multiple_files = true;

// Note: MS-LIFECYCLE exposes REST for frontend via API Gateway
// and publishes Dapr Pub/Sub events consumed by MS-ORCH.
// This proto defines the Pub/Sub event schemas only.

// Published on topic "report.status_changed"
message ReportStatusChangedEvent {
  string report_id = 1;
  string org_id = 2;
  string period_id = 3;
  string report_type = 4;
  ReportStatus from_status = 5;
  ReportStatus to_status = 6;
  string changed_by = 7;   // user_id
  string comment = 8;       // mandatory on REJECTED
  string changed_at = 9;
}

enum ReportStatus {
  REPORT_STATUS_UNSPECIFIED = 0;
  REPORT_STATUS_DRAFT = 1;
  REPORT_STATUS_SUBMITTED = 2;
  REPORT_STATUS_UNDER_REVIEW = 3;
  REPORT_STATUS_APPROVED = 4;
  REPORT_STATUS_REJECTED = 5;
}

// Published on topic "report.data_locked"
message ReportDataLockedEvent {
  string report_id = 1;
  string org_id = 2;
  string locked_at = 3;
}
```

---

### P0-W1-009: Notification Proto (MS-NOTIF)

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/notification/v1/notification.proto`

```protobuf
syntax = "proto3";
package notification.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.notification.v1";
option java_multiple_files = true;

// MS-NOTIF is called via Dapr Pub/Sub (fire-and-forget)
// These are the event schemas published to topic "notify"

message NotificationEvent {
  string notification_id = 1;
  string recipient_user_id = 2;
  string recipient_org_id = 3;
  NotificationType type = 4;
  string title = 5;
  string body = 6;
  NotificationChannel channel = 7;
  map<string, string> data = 8;  // arbitrary payload (file_id, report_id, etc.)
  string created_at = 9;
}

enum NotificationType {
  NOTIFICATION_TYPE_UNSPECIFIED = 0;
  NOTIFICATION_TYPE_FILE_PROCESSED = 1;
  NOTIFICATION_TYPE_FILE_FAILED = 2;
  NOTIFICATION_TYPE_REPORT_SUBMITTED = 3;
  NOTIFICATION_TYPE_REPORT_APPROVED = 4;
  NOTIFICATION_TYPE_REPORT_REJECTED = 5;
  NOTIFICATION_TYPE_DEADLINE_APPROACHING = 6;
  NOTIFICATION_TYPE_DEADLINE_MISSED = 7;
  NOTIFICATION_TYPE_BATCH_COMPLETED = 8;
}

enum NotificationChannel {
  NOTIFICATION_CHANNEL_UNSPECIFIED = 0;
  NOTIFICATION_CHANNEL_IN_APP = 1;   // WebSocket/SSE
  NOTIFICATION_CHANNEL_EMAIL = 2;    // SMTP
  NOTIFICATION_CHANNEL_BOTH = 3;
}
```

---

### P0-W1-010: PPTX Generator Proto (MS-GEN-PPTX)

**Type:** Proto Definition
**Effort:** 0.5 MD

**File:** `packages/protos/generator/v1/pptx_generator.proto`

```protobuf
syntax = "proto3";
package generator.v1;

import "common/v1/common.proto";

option java_package = "com.reportplatform.proto.generator.v1";
option java_multiple_files = true;

service PptxGeneratorService {
  // Generate PPTX report from template + data
  rpc GenerateReport(GenerateReportRequest) returns (GenerateReportResponse);

  // Batch generate for multiple reports
  rpc BatchGenerate(BatchGenerateRequest) returns (BatchGenerateResponse);
}

message GenerateReportRequest {
  common.v1.RequestContext context = 1;
  string template_id = 2;
  string report_id = 3;
  map<string, string> text_placeholders = 4;
  repeated GeneratorTableData tables = 5;
  repeated GeneratorChartData charts = 6;
}

message GeneratorTableData {
  string placeholder_key = 1;  // "TABLE:table_name"
  repeated string headers = 2;
  repeated GeneratorTableRow rows = 3;
}

message GeneratorTableRow {
  repeated string cells = 1;
}

message GeneratorChartData {
  string placeholder_key = 1;  // "CHART:metric_name"
  string chart_type = 2;       // "BAR", "LINE", "PIE"
  repeated string labels = 3;
  repeated ChartSeries series = 4;
}

message ChartSeries {
  string name = 1;
  repeated double values = 2;
}

message GenerateReportResponse {
  string report_id = 1;
  common.v1.BlobReference generated_file = 2;
  repeated string missing_placeholders = 3;  // placeholders without data
  string generated_at = 4;
}

message BatchGenerateRequest {
  common.v1.RequestContext context = 1;
  string template_id = 2;
  repeated string report_ids = 3;
}

message BatchGenerateResponse {
  repeated GenerateReportResponse results = 1;
  int32 successful = 2;
  int32 failed = 3;
}
```

---

## Wave 2 – REST API OpenAPI Contracts (Sonnet)

> Define OpenAPI 3.0 specs for all frontend-facing REST endpoints. These are the edge services exposed through API Gateway.

---

### P0-W2-001: REST API – Auth Service (MS-AUTH)

**Type:** OpenAPI Spec
**Effort:** 1 MD
**File:** `docs/api/ms-auth-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/auth/verify:
    post:
      summary: Validate JWT token (called by Nginx auth_request)
      responses:
        200: X-User-Id, X-Org-Id, X-Roles headers
        401: Invalid/expired token
        403: Insufficient permissions

  /api/auth/me:
    get:
      summary: Get current user context
      responses:
        200: UserContext (user_id, organizations[], active_org_id)

  /api/auth/switch-org:
    post:
      summary: Switch active organization
      requestBody: { org_id: uuid }
      responses:
        200: Updated UserContext
        403: User not member of org
```

**Tasks:**
- [ ] Full OpenAPI 3.0 YAML with schemas, examples, error responses
- [ ] Request/response schema validation rules
- [ ] Security scheme definition (Bearer JWT)

---

### P0-W2-002: REST API – File Ingestor (MS-ING)

**Type:** OpenAPI Spec
**Effort:** 0.5 MD
**File:** `docs/api/ms-ing-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/upload:
    post:
      summary: Upload file for processing
      requestBody: multipart/form-data (file + upload_purpose)
      responses:
        200: { file_id, filename, size_bytes, mime_type, status, blob_url }
        413: File too large
        415: Unsupported media type
        422: Infected file

  /api/files:
    get:
      summary: List uploaded files for current org
      parameters: page, page_size, status filter
      responses:
        200: Paginated file list

  /api/files/{file_id}:
    get:
      summary: Get file details and processing status
      responses:
        200: File details with workflow status
        404: File not found
```

---

### P0-W2-003: REST API – Query Service (MS-QRY)

**Type:** OpenAPI Spec
**Effort:** 1 MD
**File:** `docs/api/ms-qry-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/query/files/{file_id}/data:
    get:
      summary: Get parsed data for a file
      responses:
        200: Structured data (tables, documents)

  /api/query/files/{file_id}/slides:
    get:
      summary: Get slide-by-slide content for PPTX
      responses:
        200: Array of slide content with image URLs

  /api/query/tables:
    get:
      summary: Query structured table data (OPEX)
      parameters: org_id, period, source_type, pagination
      responses:
        200: Paginated table data

  /api/query/documents/{document_id}:
    get:
      summary: Get a specific document
      responses:
        200: Document content + metadata

  /api/query/processing-logs/{file_id}:
    get:
      summary: Get processing logs for a file
      responses:
        200: Array of processing steps with status
```

---

### P0-W2-004: REST API – Dashboard Service (MS-DASH)

**Type:** OpenAPI Spec
**Effort:** 1 MD
**File:** `docs/api/ms-dash-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/dashboards:
    get:
      summary: List available dashboards
    post:
      summary: Create dashboard configuration

  /api/dashboards/{dashboard_id}:
    get:
      summary: Get dashboard config
    put:
      summary: Update dashboard config
    delete:
      summary: Delete dashboard

  /api/dashboards/{dashboard_id}/data:
    post:
      summary: Execute dashboard query
      requestBody: { group_by, order_by, filters, date_range }
      responses:
        200: Aggregated data for charts

  /api/dashboards/period-comparison:
    post:
      summary: Compare metrics across periods
      requestBody: { metric, period_ids[], org_ids[] }
      responses:
        200: Comparison data with deltas
```

---

### P0-W2-005: REST API – Admin Service (MS-ADMIN)

**Type:** OpenAPI Spec
**Effort:** 1 MD
**File:** `docs/api/ms-admin-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/admin/organizations:
    get/post: CRUD organizations in holding hierarchy
  /api/admin/organizations/{org_id}:
    get/put/delete: Single organization
  /api/admin/users:
    get: List users with roles
  /api/admin/users/{user_id}/roles:
    post/delete: Assign/remove role
  /api/admin/api-keys:
    get/post: Manage API keys
  /api/admin/api-keys/{key_id}:
    delete: Revoke API key
  /api/admin/failed-jobs:
    get: List failed jobs (DLQ)
  /api/admin/failed-jobs/{job_id}/reprocess:
    post: Reprocess failed job
```

---

### P0-W2-006: REST API – Lifecycle Service (MS-LIFECYCLE)

**Type:** OpenAPI Spec
**Effort:** 1 MD
**File:** `docs/api/ms-lifecycle-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/reports:
    get: List reports for org/period
    post: Create new report (auto DRAFT)
  /api/reports/{report_id}:
    get: Report detail with status history
  /api/reports/{report_id}/submit:
    post: Transition DRAFT → SUBMITTED
  /api/reports/{report_id}/review:
    post: Transition SUBMITTED → UNDER_REVIEW
  /api/reports/{report_id}/approve:
    post: Transition UNDER_REVIEW → APPROVED
  /api/reports/{report_id}/reject:
    post: Transition → REJECTED (comment required)
  /api/reports/{report_id}/history:
    get: Full state transition timeline
  /api/reports/bulk-approve:
    post: Approve multiple reports
  /api/reports/bulk-reject:
    post: Reject multiple reports
  /api/reports/matrix:
    get: HoldingAdmin matrix [Company × Period × Status]
```

---

### P0-W2-007: REST API – Form Service (MS-FORM)

**Type:** OpenAPI Spec
**Effort:** 1.5 MD
**File:** `docs/api/ms-form-openapi.yaml`

**Endpoints:**
```yaml
paths:
  # Form Builder (Admin)
  /api/forms:
    get/post: List/create forms
  /api/forms/{form_id}:
    get/put/delete: CRUD form
  /api/forms/{form_id}/publish:
    post: DRAFT → PUBLISHED
  /api/forms/{form_id}/close:
    post: → CLOSED
  /api/forms/{form_id}/versions:
    get: List form versions
  /api/forms/{form_id}/preview:
    get: Render form preview
  /api/forms/{form_id}/export/excel-template:
    get: Download Excel template

  # Form Responses (Editor)
  /api/forms/{form_id}/responses:
    get/post: List/create responses
  /api/forms/{form_id}/responses/{response_id}:
    get/put: Get/update response
  /api/forms/{form_id}/responses/{response_id}/auto-save:
    put: Auto-save partial response
  /api/forms/{form_id}/import/excel:
    post: Import filled Excel back into form

  # Assignments
  /api/forms/{form_id}/assignments:
    get/post: Manage org assignments
```

---

### P0-W2-008: REST API – Period Service (MS-PERIOD)

**Type:** OpenAPI Spec
**Effort:** 0.5 MD
**File:** `docs/api/ms-period-openapi.yaml`

**Endpoints:**
```yaml
paths:
  /api/periods:
    get/post: List/create reporting periods
  /api/periods/{period_id}:
    get/put: Get/update period
  /api/periods/{period_id}/clone:
    post: Clone period from previous
  /api/periods/{period_id}/status:
    get: Completion tracking matrix
  /api/periods/{period_id}/export:
    get: Export status as PDF/Excel
```

---

### P0-W2-009: REST API – Notification, Versioning, Audit, Search

**Type:** OpenAPI Spec
**Effort:** 1.5 MD
**File:** `docs/api/ms-notif-openapi.yaml`, `ms-ver-openapi.yaml`, `ms-audit-openapi.yaml`, `ms-srch-openapi.yaml`

**Notification endpoints:**
```yaml
  /api/notifications: get (list), put (mark read)
  /api/notifications/settings: get/put (per-user preferences)
  /api/notifications/stream: SSE endpoint for real-time
```

**Versioning endpoints:**
```yaml
  /api/versions/{entity_type}/{entity_id}: get (version list)
  /api/versions/{entity_type}/{entity_id}/diff: get (diff between v1-v2)
```

**Audit endpoints:**
```yaml
  /api/audit/logs: get (paginated, filterable)
  /api/audit/export: get (CSV/JSON download)
```

**Search endpoints:**
```yaml
  /api/search: get (full-text + semantic)
  /api/search/suggest: get (autocomplete)
```

---

## Wave 3 – Dapr Component Definitions & Event Schemas (Haiku/Gemini)

> Boilerplate Dapr configuration files and event topic definitions.

---

### P0-W3-001: Dapr PubSub Component Config

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] `infra/dapr/components/pubsub.yaml` – Redis Streams pubsub component
- [ ] Topic definitions document:
  | Topic | Publisher | Subscriber | Event Schema |
  |---|---|---|---|
  | `file-uploaded` | MS-ING | MS-ORCH | `FileUploadedEvent` |
  | `processing-completed` | MS-ORCH | MS-NOTIF | `ProcessingCompletedEvent` |
  | `report.status_changed` | MS-LIFECYCLE | MS-ORCH, MS-NOTIF | `ReportStatusChangedEvent` |
  | `report.data_locked` | MS-LIFECYCLE | MS-SINK-TBL | `ReportDataLockedEvent` |
  | `notify` | MS-ORCH | MS-NOTIF | `NotificationEvent` |
- [ ] Dead letter topic configuration

---

### P0-W3-002: Dapr State Store Component Config

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] `infra/dapr/components/statestore.yaml` – Redis state store for MS-ORCH workflow state
- [ ] Key naming convention: `workflow:{workflow_id}:state`
- [ ] TTL configuration for running workflows
- [ ] `infra/dapr/components/statestore-pg.yaml` – PostgreSQL state store for paused workflows

---

### P0-W3-003: Dapr Service Invocation Config

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] Per-service Dapr config files in `infra/dapr/config/`:
  | Service | app-id | app-protocol | app-port |
  |---|---|---|---|
  | MS-AUTH | `ms-auth` | `http` | `8000` |
  | MS-ING | `ms-ing` | `http` | `8000` |
  | MS-ORCH | `ms-orch` | `grpc` | `50051` |
  | MS-ATM-PPTX | `ms-atm-pptx` | `grpc` | `50051` |
  | MS-ATM-XLS | `ms-atm-xls` | `grpc` | `50051` |
  | MS-ATM-PDF | `ms-atm-pdf` | `grpc` | `50051` |
  | MS-ATM-CSV | `ms-atm-csv` | `grpc` | `50051` |
  | MS-ATM-AI | `ms-atm-ai` | `grpc` | `50051` |
  | MS-SINK-TBL | `ms-sink-tbl` | `grpc` | `50051` |
  | MS-SINK-DOC | `ms-sink-doc` | `grpc` | `50051` |
  | MS-SINK-LOG | `ms-sink-log` | `grpc` | `50051` |
  | MS-TMPL | `ms-tmpl` | `grpc` | `50051` |
  | MS-NOTIF | `ms-notif` | `grpc` | `50051` |
  | MS-QRY | `ms-qry` | `http` | `8080` |
  | MS-DASH | `ms-dash` | `http` | `8080` |
- [ ] Access control policies (which service can call which)

---

### P0-W3-004: TypeScript API Types (packages/types)

**Type:** Type Definitions
**Effort:** 1 MD

**Tasks:**
- [ ] `packages/types/src/auth.ts` – UserContext, Organization, Role types
- [ ] `packages/types/src/files.ts` – FileUpload, FileDetails, ProcessingStatus
- [ ] `packages/types/src/query.ts` – TableData, SlideContent, DocumentContent
- [ ] `packages/types/src/dashboard.ts` – DashboardConfig, ChartData, AggregatedData
- [ ] `packages/types/src/reports.ts` – Report, ReportStatus, StatusTransition
- [ ] `packages/types/src/forms.ts` – FormDefinition, FormField, FormResponse
- [ ] `packages/types/src/periods.ts` – Period, PeriodStatus, CompletionMatrix
- [ ] `packages/types/src/notifications.ts` – Notification, NotificationType, NotificationSettings
- [ ] `packages/types/src/admin.ts` – Organization, UserRole, ApiKey, FailedJob
- [ ] `packages/types/src/common.ts` – Pagination, ApiError, ApiResponse wrapper
- [ ] `packages/types/src/index.ts` – barrel export
- [ ] `packages/types/package.json` – npm package config

---

## Wave 4 – Frontend API Client Layer (Gemini Flash/MiniMax)

> Generate typed Axios client functions from OpenAPI specs.

---

### P0-W4-001: Axios Instance & Auth Interceptor

**Type:** Frontend Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] `apps/frontend/src/api/axios.ts` – centralized Axios instance
- [ ] MSAL token interceptor (acquireTokenSilent → Bearer header)
- [ ] Error interceptor (401 → redirect to login, 429 → rate limit message)
- [ ] Base URL from `VITE_API_BASE_URL`

---

### P0-W4-002: API Client Functions – Auth

**Type:** Frontend API Client
**Effort:** 0.5 MD

**Tasks:**
- [ ] `apps/frontend/src/api/auth.ts`
  - `getMe(): Promise<UserContext>`
  - `switchOrg(orgId: string): Promise<UserContext>`

---

### P0-W4-003: API Client Functions – Files & Upload

**Type:** Frontend API Client
**Effort:** 0.5 MD

**Tasks:**
- [ ] `apps/frontend/src/api/files.ts`
  - `uploadFile(file: File, purpose?: UploadPurpose): Promise<FileUploadResponse>`
  - `listFiles(params: FileListParams): Promise<PaginatedResponse<FileDetails>>`
  - `getFile(fileId: string): Promise<FileDetails>`

---

### P0-W4-004: API Client Functions – Query & Dashboard

**Type:** Frontend API Client
**Effort:** 0.5 MD

**Tasks:**
- [ ] `apps/frontend/src/api/query.ts`
  - `getFileData(fileId: string): Promise<ParsedData>`
  - `getSlides(fileId: string): Promise<SlideContent[]>`
  - `queryTables(params: TableQueryParams): Promise<PaginatedResponse<TableData>>`
  - `getProcessingLogs(fileId: string): Promise<ProcessingLog[]>`
- [ ] `apps/frontend/src/api/dashboards.ts`
  - CRUD operations + `executeDashboardQuery()`

---

### P0-W4-005: API Client Functions – Reports, Forms, Periods

**Type:** Frontend API Client
**Effort:** 1 MD

**Tasks:**
- [ ] `apps/frontend/src/api/reports.ts` – full lifecycle operations
- [ ] `apps/frontend/src/api/forms.ts` – form builder + response CRUD + Excel import/export
- [ ] `apps/frontend/src/api/periods.ts` – period management
- [ ] `apps/frontend/src/api/admin.ts` – org/user/role management
- [ ] `apps/frontend/src/api/notifications.ts` – notification list + SSE stream
- [ ] `apps/frontend/src/api/search.ts` – search + suggest

---

### P0-W4-006: React Query Hooks Scaffold

**Type:** Frontend Data Layer
**Effort:** 1 MD

**Tasks:**
- [ ] `apps/frontend/src/hooks/useAuth.ts` – useMe, useSwitchOrg
- [ ] `apps/frontend/src/hooks/useFiles.ts` – useFiles, useUpload (with invalidation)
- [ ] `apps/frontend/src/hooks/useQuery.ts` – useFileData, useSlides, useTables
- [ ] `apps/frontend/src/hooks/useReports.ts` – useReports, useSubmitReport, useApproveReport
- [ ] `apps/frontend/src/hooks/useForms.ts` – useForms, useFormResponse, useAutoSave
- [ ] `apps/frontend/src/hooks/usePeriods.ts` – usePeriods, usePeriodStatus
- [ ] `apps/frontend/src/hooks/useNotifications.ts` – useNotifications, useSSE
- [ ] `apps/frontend/src/hooks/useAdmin.ts` – useOrganizations, useUsers, useFailedJobs
- [ ] Each hook: loading state, error state, cache invalidation after mutations

---

## Summary

| Wave | Agent | Tasks | Total Effort |
|---|---|---|---|
| W1 – Core Protos | Opus | P0-W1-001 to P0-W1-010 | ~8 MD |
| W2 – REST OpenAPI | Sonnet | P0-W2-001 to P0-W2-009 | ~8 MD |
| W3 – Dapr Config + TS Types | Haiku/Gemini | P0-W3-001 to P0-W3-004 | ~2.5 MD |
| W4 – FE API Client | Gemini Flash/MiniMax | P0-W4-001 to P0-W4-006 | ~4 MD |
| **Total P0** | | **29 tasks** | **~22.5 MD** |

**Dependencies:**
- W2, W3, W4 can start after W1 (common types needed)
- W4 depends on W2 (OpenAPI specs) and P0-W3-004 (TS types)
- W3-001..003 are independent and can run in parallel with W1
