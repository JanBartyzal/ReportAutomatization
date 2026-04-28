# P8-W3: ITSM Report Templates

**Phase:** P8
**Module:** engine-reporting:text-template (seed data) + processor-generators
**Effort:** ~3 MD
**Priority:** HIGH
**Depends on:** P8-W1, P8-W2, P8-W4, P8-W5

---

## Goal

Seed pre-built ITSM report templates into the Text Template catalog.
Templates are generic enough to work with ANY data source (not just ServiceNow).

---

## Report Templates to Create

### 1. ITSM Group Status Report
- **Type:** PPTX
- **Content:**
  - Slide 1: Cover (group name, period, generated date)
  - Slide 2: Executive summary (open/resolved counts, SLA breach %)
  - Slide 3: Priority distribution (pie chart)
  - Slide 4: Trend line – incidents opened vs. resolved (7/30 days)
  - Slide 5: Top 10 open incidents (table)
  - Slide 6: SLA at risk (breached + near-due)
- **Named Queries needed:**
  - `itsm_open_count` – SELECT COUNT(*) FROM snow_incidents WHERE state NOT IN ('resolved','closed') AND resolver_group_id = :groupId
  - `itsm_priority_distribution` – GROUP BY priority
  - `itsm_sla_breach_rate` – breached / total
  - `itsm_open_incidents_top10` – top 10 by age_days DESC
  - `itsm_trend_7d` – daily counts last 7 days

### 2. ITSM Weekly Digest (Excel)
- **Type:** Excel multi-sheet
- **Sheet 1:** Summary KPIs
- **Sheet 2:** All open incidents (filterable table)
- **Sheet 3:** Resolved this week
- **Sheet 4:** SLA analysis

### 3. Universal Data Report (generic, not ITSM-specific)
- Works with ANY named query as data source
- User picks: title, description, named query for main table, optional chart query
- Output: simple PPTX (title + table) or Excel

---

## Migration File

- `V6_0_002__seed_itsm_templates.sql` (engine-reporting migrations)
  - Seeds TextTemplate records with pre-built data_bindings pointing to named queries
  - Marked as `scope=CENTRAL`, `is_system=true`

---

## Acceptance Criteria

- [ ] ITSM Group Status Report template visible in template catalog
- [ ] Universal Data Report template available for any data source
- [ ] Render triggered with valid group_id → PPTX generated successfully
- [ ] Excel digest generated with correct sheets and data
