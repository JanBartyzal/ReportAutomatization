# P3a – Wave 4: Frontend Admin & AI UI (Gemini Flash/MiniMax)

**Phase:** P3a – Intelligence & Admin
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD
**Depends on:** P3a-W2 (admin endpoints)

---

## P3a-W4-001: Admin Panel UI

**Type:** Frontend Feature
**Effort:** 5 MD

**Tasks:**
- [ ] Admin section in navigation (visible only to Admin/HoldingAdmin)
- [ ] Organization management page:
  - Tree view of holding hierarchy
  - Create/edit/delete organizations
  - Drag & drop reordering
- [ ] User management page:
  - User list with role badges
  - Role assignment dialog
  - Search/filter users
- [ ] API key management page:
  - Key list with usage stats
  - Generate new key (show once)
  - Revoke key with confirmation
- [ ] Failed jobs page:
  - Table with error details
  - Reprocess button with confirmation
  - Status filter (failed, reprocessed)
- [ ] Batch management page:
  - Create/view batches
  - Consolidated file list per batch
  - Status summary

---

## P3a-W4-002: Schema Mapping UI

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Mapping template editor:
  - Source column → target column drag & drop
  - Rule type selector (exact, synonym, regex, AI)
  - Confidence indicator for AI suggestions
  - Preview with sample data
- [ ] Mapping history view (successful mappings per org)
- [ ] Auto-suggestion display during upload processing
- [ ] Template versioning UI
