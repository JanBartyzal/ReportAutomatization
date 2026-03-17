# P2 – Wave 2: Simpler Atomizers & Cleanup (Sonnet)

**Phase:** P2 – Extended Parsing & Visualization
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~14 MD
**Depends on:** P1-W1 (python-base, orchestrator)

> Straightforward extraction services with well-defined scope.

---

## P2-W2-001: processor-atomizers:pdf – PDF/OCR Atomizer

**Type:** Data Service
**Effort:** 7 MD
**Service:** apps/processor/microservices/units/ms-atm-pdf

**Tasks:**
- [x] FastAPI + gRPC using `packages/python-base`
- [x] Implement `PdfAtomizerService` from `atomizer.v1.pdf` proto
- [x] **Text Layer Detection**: Try text extraction first (PyPDF2/pdfplumber)
- [x] **OCR Fallback**: If text layer empty → Tesseract OCR
  - Language support: Czech, English, German
  - Confidence score per page
- [x] **Table Extraction**: pdfplumber for table detection in text PDFs
- [x] **Page-by-Page Processing**: Each page as separate content block
- [x] Dockerfile with Tesseract + language packs
- [x] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Text PDF → extracted directly (fast path)
- [ ] Scanned PDF → OCR with confidence > 0.8
- [ ] Mixed PDF (some pages text, some scanned) → correct detection per page

---

## P2-W2-002: processor-atomizers:csv – CSV Atomizer

**Type:** Data Service
**Effort:** 2 MD
**Service:** apps/processor/microservices/units/ms-atm-csv

**Tasks:**
- [x] FastAPI + gRPC using `packages/python-base`
- [x] Implement `CsvAtomizerService` from `atomizer.v1.csv` proto
- [x] **Auto-Detection**:
  - Delimiter: `,`, `;`, `|`, `\t` (frequency analysis on first 10 lines)
  - Encoding: UTF-8, Windows-1250, ISO-8859-2 (chardet library)
  - Header row: heuristic detection
- [x] Data type inference per column
- [x] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Semicolon-delimited Czech CSV correctly parsed
- [ ] Windows-1250 encoding auto-detected

---

## P2-W2-003: processor-atomizers:cleanup – Cleanup Worker

**Type:** CronJob
**Effort:** 2 MD
**Service:** apps/processor/microservices/units/ms-atm-cln

**Tasks:**
- [x] Python CronJob (scheduled via Docker or Dapr cron binding)
- [x] **Cleanup Rules**:
  - Delete temporary PNG slides from Blob > 24 hours old
  - Delete temporary CSV exports > 24 hours old
  - Delete `_raw/` original files > 90 days old
  - Delete temporary generator output files > 24 hours old
- [x] Dry-run mode for testing
- [x] Logging of deleted files count and freed storage
- [x] Docker Compose entry (cron schedule: every hour)

---

## P2-W2-004: engine-orchestrator Extension – Multi-Format Router

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-orch (extension)

**Tasks:**
- [x] Add routing for new file types in workflow definitions:
  - `.xlsx` → processor-atomizers:xls
  - `.pdf` → processor-atomizers:pdf
  - `.csv` → processor-atomizers:csv
- [x] JSON workflow definitions for each file type
- [x] Error handling specific to each atomizer type
- [ ] Integration tests: upload each file type → verify full pipeline
