# P2 – Wave 3: Configuration & Data Setup (Haiku/Gemini)

**Phase:** P2 – Extended Parsing & Visualization
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~2.5 MD

---

## P2-W3-001: Docker Compose – P2 Services

**Type:** Infrastructure
**Effort:** 1 MD

**Tasks:**
- [x] Add processor-atomizers:xls, processor-atomizers:pdf, processor-atomizers:csv, processor-atomizers:cleanup to docker-compose.yml
- [x] Add engine-data:query, engine-data:dashboard to docker-compose.yml
- [x] Dapr sidecar configs for new services
- [x] Nginx routing updates for `/api/query/*` and `/api/dashboards/*`
- [x] Tesseract language data volume for processor-atomizers:pdf

---

## P2-W3-002: PostgreSQL Materialized Views & Indexes

**Type:** Database
**Effort:** 1 MD

**Tasks:**
- [x] Flyway migration: materialized views for engine-data:query
- [x] Indexes on JSONB columns for common query patterns
- [x] Refresh strategy for materialized views (triggered by new data)
- [x] RLS policies for new tables/views

---

## P2-W3-003: Sample Test Files

**Type:** Test Data
**Effort:** 0.5 MD

**Tasks:**
- [ ] Sample PPTX files (with tables, charts, SmartArt, notes)
- [ ] Sample XLSX files (multi-sheet, merged cells, formulas, Czech locale)
- [ ] Sample PDF files (text, scanned/OCR, mixed)
- [x] Sample CSV files (various delimiters, encodings)
- [x] EICAR test file for scanner verification
- [x] Place in `tests/fixtures/`
