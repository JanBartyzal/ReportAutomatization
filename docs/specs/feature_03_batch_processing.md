# Feature Specification: Batch Processing System

**Feature ID:** FEAT-03
**Parent Epic:** Workflow Management
**Prerequisites:** Database Schema Refactoring

---

## 1. Problem Statement
Users upload files in waves (e.g., "Q1 Review", "Q2 Strategy"). Currently, all data is dumped into a flat structure. We need to group uploads into **"Batches"** to allow versioning, comparison, and clean deletion of old data.

## 2. Technical Requirements

### Database Schema Updates (SQLAlchemy)
1.  **New Table:** `batches`
    - `id` (UUID, PK)
    - `name` (String, e.g., "Review 2026-01")
    - `status` (Enum: OPEN, PROCESSING, CLOSED)
    - `created_at`
    - `user_id` (Owner)
2.  **Update Table:** `extracted_data` (and `document_chunks` for RAG)
    - Add `batch_id` (ForeignKey -> batches.id).

### API Endpoints (`backend/app/routers/batches.py`)
- `POST /api/batches`: Create a new batch context.
- `POST /api/batches/{id}/close`: Mark batch as complete (triggers final aggregation or RAG indexing).
- `GET /api/batches`: List my batches.
- **Update Upload Endpoint:** Modify `POST /api/upload` to accept `batch_id` as a required query parameter.

### Logic Changes
- **RAG Context:** When chatting with data (`/api/chat-with-data`), user must be able to filter by Batch ID (e.g., "Chat only with Q1 Review data").
- **Security (RLS):** Ensure users can only see batches they created (or belong to their tenant).

## 3. DoD Specifics
- **Migration:** Create an Alembic migration script for the DB changes.
- **Backward Compatibility:** For existing data without a batch, assign a default "Legacy Batch".
- **Tests:** Test the lifecycle: Create Batch -> Upload File -> Verify File has Batch ID -> Close Batch.