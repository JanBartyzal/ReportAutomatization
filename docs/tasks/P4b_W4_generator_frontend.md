# P4b – Wave 4: Frontend Generator UI (Gemini Flash/MiniMax)

**Phase:** P4b – PPTX Report Generation
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD

---

## P4b-W4-001: Template Management UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Template List Page** (HoldingAdmin):
  - Table: name, version, assigned period, placeholder count
  - Upload new template button
- [ ] **Template Detail Page**:
  - Uploaded PPTX preview (slide thumbnails)
  - Extracted placeholder list
  - Version history
- [ ] **Placeholder Mapping Editor**:
  - Table: placeholder → data source (dropdown)
  - Data source options: form fields, uploaded file columns, dashboard metrics
  - Preview button (generate with sample data)

---

## P4b-W4-002: Report Generation UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Generate Button** on report detail page (visible when APPROVED):
  - Click → trigger generation workflow
  - Progress indicator (polling or SSE)
  - Download link when complete
- [ ] **Batch Generation UI** (HoldingAdmin):
  - Select period → show APPROVED reports
  - "Generate All" button
  - Progress: X/Y completed
  - Download individual or ZIP of all
- [ ] **Generated Reports List**:
  - Table: report, template, generated_at, download link
  - Re-generate button (with current data)
