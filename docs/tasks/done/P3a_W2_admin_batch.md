# P3a – Wave 2: Admin Backend & Batch Service (Sonnet)

**Phase:** P3a – Intelligence & Admin
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~20 MD

---

## P3a-W2-001: engine-core:admin – Admin Backend

**Type:** Core Service
**Effort:** 12 MD
**Service:** apps/engine/microservices/units/ms-admin

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **REST endpoints** (frontend-facing via API Gateway):
  - `CRUD /api/admin/organizations` – holding hierarchy management
  - `GET /api/admin/users` – list users with roles
  - `POST/DELETE /api/admin/users/{id}/roles` – assign/remove roles
  - `CRUD /api/admin/api-keys` – API key management
  - `GET /api/admin/failed-jobs` – DLQ viewer
  - `POST /api/admin/failed-jobs/{id}/reprocess` – manual retry
- [ ] **Organization Hierarchy**:
  - 3-level structure: Holding → Company → Division/Cost Center
  - CRUD operations with validation (no orphan orgs)
  - Tree view data for frontend
- [ ] **Role Management**:
  - Assign roles per org (Admin, Editor, Viewer, HoldingAdmin)
  - Validate: only HoldingAdmin can assign roles cross-org
  - Audit log for role changes
- [ ] **API Key Management**:
  - Generate API keys for service accounts
  - Keys stored as bcrypt hash (never plaintext)
  - Key metadata: name, created_by, expires_at, last_used
  - Revocation endpoint
- [ ] **Secrets Management** (Superadmin):
  - UI proxy for Azure KeyVault operations
  - List secrets, update values
  - Only accessible to Superadmin role
- [ ] **Failed Jobs UI Support**:
  - List failed jobs with filters (date, error type, org)
  - Reprocess trigger → calls engine-orchestrator.ReprocessFailedJob
  - Job detail with full error stacktrace
- [ ] Flyway migrations: `api_keys` table
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/admin/*` → engine-core:admin

**AC:**
- [ ] Org hierarchy CRUD works with validation
- [ ] API key generation returns key only once (on create)
- [ ] Failed job reprocess triggers engine-orchestrator workflow
- [ ] Non-admin users get `403` on admin endpoints

---

## P3a-W2-002: engine-core:batch – Batch & Org Service

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/engine/microservices/units/ms-batch

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Batch Management**:
  - Create batch (e.g., `Q2/2025`) linked to holding
  - Assign files to batch via metadata tag
  - Batch status: OPEN, COLLECTING, CLOSED
  - HoldingAdmin creates, subsidiaries upload to
- [ ] **REST endpoints** (via API Gateway):
  - `CRUD /api/batches` – batch lifecycle
  - `GET /api/batches/{id}/files` – files in batch
  - `GET /api/batches/{id}/status` – consolidated view
- [ ] **Organization Metadata**:
  - Extend file metadata with `holding_id`, `company_id`, `batch_id`
  - Holding-level aggregation queries
- [ ] **RLS Enforcement**:
  - PostgreSQL RLS: every query auto-filtered by `org_id` from JWT
  - Cross-tenant access impossible at DB level
  - Integration tests verifying RLS isolation
- [ ] **Batch → Period Mapping**: `period_id` replaces generic `batch_id` for OPEX reporting
- [ ] Flyway migrations: `batches` table with RLS
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] HoldingAdmin creates batch visible to all subsidiaries
- [ ] Files tagged with batch appear in consolidated view
- [ ] RLS prevents cross-tenant data leak (integration test verified)
