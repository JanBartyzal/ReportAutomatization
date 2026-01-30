# Task: Implement Batch Processing System (FEAT-03) [X]

**Spec:** `docs/specs/feature_03_batch_processing.md`
**Standards:** `docs/project_standards.md`
**DoD:** `docs/dod_criteria.md`

## Overview
Implement a system to group uploads into "Batches" (e.g., "Q1 Review"). This enables versioning, clean deletion, and scoped RAG queries.

## Checklist

### 1. Analysis & Setup
- [x] Review `docs/specs/feature_03_batch_processing.md`.
- [x] Understand the database schema changes required.

### 2. Implementation
- [x] **Database Schema (SQLAlchemy)**
    - [x] Create/Edit `backend/app/models/batch.py` (implemented in `core/models.py` as per standards).
    - [x] Define `Batch` model:
        - `id` (UUID, PK)
        - `name` (String)
        - `status` (Enum: OPEN, PROCESSING, CLOSED)
        - `created_at`
        - `user_id`
    - [x] Update `ExtractedData` and `DocumentChunk` models to include `batch_id` (FK).
    - [x] Generate Alembic migration script (Implemented via `migrate_batches.py`).
    - [x] Apply migration.
- [x] **API Endpoints**
    - [x] Create `backend/app/routers/batches.py`.
    - [x] `POST /api/batches` (Create new batch).
    - [x] `GET /api/batches` (List user batches).
    - [x] `POST /api/batches/{id}/close` (Mark complete, trigger aggregation/indexing if needed).
    - [x] Update `POST /api/upload`:
        - [x] Require `batch_id` query param.
        - [x] Validate batch exists and is OPEN.
- [x] **Logic Integration**
    - [x] Update RAG/Chat Service to support filtering by `batch_id`.
    - [x] Implement Row Level Security (RLS) logic (User can only see their batches).
    - [x] Backward Compatibility: Handle existing data (migration assign "Legacy Batch").

### 3. Verification & Testing
- [x] **Unit Tests**
    - [x] Create `tests/backend/test_batches.py`.
    - [x] Test lifecycle: Create -> Upload -> Close.
    - [x] Test permissions (User A cannot access User B's batch).
    - [x] Test RAG filter (Chat only returns context from specific batch).

### 4. Documentation
- [x] Update API Documentation (Swagger/Walkthrough).
- [x] Update `docs/project_status.md`.
