# Task: QA & Validation for Sprint 2 (Template Aggregation)

**Type:** Quality Assurance
**Target Component:** `backend/app/services/aggregation.py`
**Reference Standard:** `dod_criteria.md`

---

## 1. Objective
Verify that the newly implemented Template Aggregation logic works correctly and meets the DoD.

## 2. Test Scenarios to Implement
Create a new test file `backend/tests/test_aggregation.py` covering:

1.  **Exact Match:** Two dummy datasets with identical headers -> Result is 1 merged table.
2.  **Fuzzy Match:** "Total Revenue" vs "revenue" -> Should merge if similarity > 90%.
3.  **Missing Column:** File A has "Q4", File B does not -> Result should have "Q4" with `null` for File B rows.
4.  **Data Type Conflict:** One file has string "N/A", other has int 100 -> Verify graceful handling (e.g., cast to string).
5.  **Source Tracking:** Verify that the output rows contain the correct `source_file` metadata.

## 3. Execution
- Implement the tests using `pytest`.
- Run the tests and report the output.
- If tests fail, fix the code immediately.
