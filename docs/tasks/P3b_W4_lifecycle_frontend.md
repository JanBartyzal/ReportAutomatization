# P3b – Wave 4: Frontend Lifecycle UI (Gemini Flash/MiniMax)

**Phase:** P3b – Reporting Lifecycle & Period Management
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~10 MD
**Depends on:** P3b-W1 (lifecycle, period endpoints)

---

## P3b-W4-001: Report Lifecycle UI

**Type:** Frontend Feature
**Effort:** 6 MD

**Tasks:**
- [ ] **Report List Page**:
  - Table: report name, org, period, status badge, last updated
  - Filter by status, period, org
  - Quick action buttons: Submit, Review, Approve, Reject
- [ ] **Report Detail Page**:
  - Status badge with current state
  - Timeline view of all state transitions (who, when, comment)
  - Linked files and form responses
  - Submission checklist (green/red indicators)
  - Submit button (disabled until checklist 100%)
- [ ] **Rejection Dialog**:
  - Modal with mandatory comment field
  - Comment visible in timeline after rejection
- [ ] **Bulk Actions** (HoldingAdmin):
  - Multi-select reports in list
  - Bulk approve / bulk reject buttons
  - Confirmation dialog with count
- [ ] **HoldingAdmin Matrix Dashboard**:
  - Grid: rows = companies, columns = periods
  - Cell color: grey (DRAFT), yellow (SUBMITTED), green (APPROVED), red (REJECTED/overdue)
  - Click cell → navigate to report detail

---

## P3b-W4-002: Period Management UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Period List Page**:
  - Table: name, type, dates, status, completion %
  - Filter by type, status, year
- [ ] **Period Detail Page**:
  - Dates and deadlines display
  - Completion matrix (same as HoldingAdmin matrix, scoped to period)
  - Percentage progress bar
  - Export button (PDF/Excel)
- [ ] **Create/Clone Period**:
  - Form: name, type, start_date, submission_deadline, review_deadline
  - Clone button on existing period (pre-fills form with new dates)
- [ ] **Countdown Widget**: Days until deadline, shown on Editor dashboard
