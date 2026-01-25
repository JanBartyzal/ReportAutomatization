# Feature Specification: Template Aggregation Engine

**Feature ID:** FEAT-02
**Parent Epic:** Data Analytics
**Prerequisites:** Native Table Extraction (Complete), Refactored Service Layer

---

## 1. Problem Statement
We process presentations from multiple countries (e.g., "Monthly Report - Germany.pptx", "Monthly Report - France.pptx"). These files often contain identical table structures (templates) but different data.
Currently, these are stored as isolated JSON records. We need to identify identical schemas and aggregate the data into a "Master Dataset" to allow cross-country analysis (e.g., "Show me Total Revenue across all Europe").

## 2. Technical Requirements

### New Service: `AggregationService`
Create a service that logic resides in `backend/app/services/aggregation.py`.

### Core Logic (The Algorithm)
1.  **Schema Fingerprinting:**
    - For every extracted table, generate a "fingerprint" based on:
        - Column Names (normalized: lowercase, removed special chars).
        - Data Types (inferred: numeric vs string).
        - Column Count.
2.  **Fuzzy Matching:**
    - Use `TheFuzz` or `Levenshtein` to match column headers (e.g., allow matching "Total Revenue" with "Revenue (EUR)").
    - Threshold: > 90% similarity.
3.  **Virtual Merging (SQL View or Dynamic Query):**
    - Instead of creating a new physical table, create an API endpoint that performs a `UNION ALL` query on extracted data that shares the same fingerprint.

### API Endpoints (`backend/app/routers/analytics.py`)
- `POST /api/analytics/aggregate/preview`: Input list of file IDs. Output: Detected common schema and row count.
- `GET /api/analytics/aggregate/{schema_fingerprint}`: Returns the merged dataset from all matching files.

## 3. Data Integrity & DoD
- **Source Tracking:** The aggregated result MUST preserve `source_file`, `slide_number`, and `country/region` (parsed from filename).
- **Missing Columns:** If one file misses a column present in others, fill with `null`.
- **Conflict Resolution:** If data types mismatch (one is Text, one is Int), cast to Text or log a warning.

## 4. Implementation Steps
1.  Install `thefuzz` or similar library for string matching.
2.  Implement `generate_schema_hash(headers: List[str]) -> str`.
3.  Implement the Aggregation Logic.
4.  Update `project_status.md` and `docs/api/openapi_summary.md`.