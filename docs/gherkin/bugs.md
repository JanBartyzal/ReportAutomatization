# Bugs & Discrepancies Report

## Overview

This document captures discrepancies found between the Gherkin specification scenarios and the actual project implementation. Each issue has been verified against the codebase (2026-03-17).

---

## FS03 - Atomizers

### ~~Issue 3.1: Missing Plotly for Chart Generation~~ FIXED

- **Severity**: Medium
- **Feature**: FS18 PPTX Generation
- **Description**: The specification mentions "python-pptx and matplotlib/plotly" for chart generation in PPTX files, but only matplotlib was included.
- **Evidence**:
  - FS18 Line 97: "And the generator creates charts from mapped metrics using python-pptx and matplotlib/plotly"
  - `apps/processor/processor-generators/pyproject.toml` - was missing `plotly`
- **Resolution**: Added `plotly>=5.18` and `kaleido>=0.2.1` (static export engine) to `pyproject.toml`.

---

## Verified — No Issue Found

The following reported issues were investigated and found to be **not bugs**:

### ~~Issue 1.1 / 9.1: Missing Tailwind CSS~~ NOT A BUG

- **Verdict**: Intentional design decision
- **Evidence**: `docs/UX-UI/02-design-system.md` line 45 explicitly states: "Tailwind utility classes — buď full adoption, nebo nula — **volíme nulu, Fluent stačí**". The `index.css` header also states "No Tailwind utility classes / Use Fluent tokens via makeStyles". The Gherkin spec is outdated on this point.

### ~~Issue 5.1: Redis Cache TTL Mismatch~~ NOT A BUG

- **Verdict**: TTL matches spec exactly
- **Evidence**: `ReportDataAggregationService.java:27` defines `CACHE_TTL_SECONDS = 300` (5 minutes). `CacheService.java:30` uses `cache.ttl-minutes:5`. Both match the specified 5-minute TTL.

### ~~Issue 6.1: Materialized View Refresh~~ NOT A BUG

- **Verdict**: Fully implemented
- **Evidence**: `ViewRefreshService.refreshAllViews()` calls `SELECT refresh_query_views()` which refreshes all 6 materialized views concurrently. `DataChangedSubscriber` triggers this on data ingestion events via Dapr Pub/Sub.

### ~~Issue 9.2: Real-Time Updates~~ NOT A BUG

- **Verdict**: Implemented as designed
- **Evidence**: SSE (`SseEmitter`) and WebSocket are implemented in `NotificationService.java` for the notification system. The original report already acknowledged this.

### ~~Issue 12.1: MCP Server~~ NOT A BUG

- **Verdict**: Fully implemented
- **Evidence**: Complete MCP server at `apps/processor/processor-generators/src/mcp/mcp_server.py` with tools (`query_opex`, `report_status`, `search_documents`, `compare_periods`), OAuth OBO auth flow (`src/mcp/auth/`), AI client integration (`src/mcp/client/`), and tests (`test_mcp_tools.py`, `test_obo_flow.py`).

### ~~Issue 14.1: Version Lock for Approved Reports~~ NOT A BUG

- **Verdict**: Fully implemented
- **Evidence**: `ReportEntity` has `locked` boolean field. `ReportService` throws `DataLockedException` when editing locked reports (lines 112-113, 175-176). `GlobalExceptionHandler` returns proper HTTP error. `DaprEventPublisher` publishes `report.data_locked` event. `VersionService.createVersionOnLockedEntity()` supports creating a new version which returns the report to DRAFT state.

### ~~Issue 17.1: State Transitions~~ NOT A BUG

- **Verdict**: Fully implemented via Spring State Machine
- **Evidence**: `ReportStateMachineConfig.java` defines strict transitions: DRAFT→SUBMITTED, SUBMITTED→UNDER_REVIEW, UNDER_REVIEW→APPROVED/REJECTED, REJECTED→DRAFT, DRAFT→COMPLETED (local scope only), COMPLETED→SUBMITTED. Each transition has role-based guards (`editorRoleGuard`, `reviewerRoleGuard`, `rejectGuard` requiring mandatory comment). Invalid transitions (e.g. DRAFT→APPROVED) are impossible by configuration.

### ~~Issue 17.2: Submission Checklist~~ NOT A BUG

- **Verdict**: Fully implemented
- **Evidence**: `SubmissionChecklistService` with `isComplete()` method checking 100% completion. `SubmissionChecklistEntity` stores checklist JSON with completion percentage. `ChecklistIncompleteException` thrown when checklist is not 100%. Default checklist items: required_fields, required_sheets, validation_rules.

### ~~Issue 19.1: Auto-Save Interval~~ NOT A BUG

- **Verdict**: Implemented with 30-second debounce
- **Evidence**: Frontend `useForms.ts:134` uses `setTimeout(() => mutation.mutate(fields), 30_000)`. Backend `FormResponseService.autoSave()` endpoint at `PUT /forms/{formId}/responses/{respId}/auto-save`. `AutoSaveRequest` DTO exists.

### ~~Issue 20.1: Clone Period Performance~~ INFO ONLY

- **Verdict**: Implementation exists, performance is a runtime concern
- **Evidence**: `PeriodCloneService.clonePeriod()` is a single `@Transactional` method that clones period + org assignments in bulk. For 20 assignments, this should complete well under 2 minutes. Performance testing requires a running environment, not a code bug.

### ~~Issue 22.1: Placeholder Feature~~ EXPECTED

- **Verdict**: FS22 is Phase P6, not yet scheduled
- **Status**: Not a bug — expected per roadmap

---

## Summary

| Issue ID | Feature | Severity | Verdict |
|----------|---------|----------|---------|
| 1.1 / 9.1 | FS09/F01 | — | NOT A BUG (intentional design: Fluent-only, no Tailwind) |
| 3.1 / 18.1 | FS18/F03 | Medium | FIXED — added plotly + kaleido to pyproject.toml |
| 5.1 | FS05/FS06 | — | NOT A BUG (TTL = 300s = 5 min, matches spec) |
| 6.1 | FS06 | — | NOT A BUG (ViewRefreshService + DataChangedSubscriber) |
| 9.2 | FS09 | — | NOT A BUG (SSE + WebSocket implemented) |
| 12.1 | FS12 | — | NOT A BUG (full MCP server with tools + OBO auth) |
| 14.1 | FS14 | — | NOT A BUG (locked field + DataLockedException) |
| 17.1 | FS17 | — | NOT A BUG (Spring State Machine with guards) |
| 17.2 | FS17 | — | NOT A BUG (SubmissionChecklistService) |
| 19.1 | FS19 | — | NOT A BUG (30s debounce auto-save) |
| 20.1 | FS20 | — | INFO (implementation exists, perf is runtime) |
| 22.1 | FS22 | Info | EXPECTED (P6 placeholder) |
