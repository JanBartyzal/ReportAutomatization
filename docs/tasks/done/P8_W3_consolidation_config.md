# P8 – Wave 3: Microservice Consolidation – Config & Boilerplate (Haiku/Gemini)

**Phase:** P8 – Microservice Consolidation Refactoring
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~4 MD
**Depends on:** P8-W1 (architecture), P8-W2 (infra)

> Environment files, migration ordering, documentation, cleanup of old service directories.

---

## P8-W3-001: Environment & Config File Consolidation

**Type:** Configuration
**Effort:** 1.5 MD

**Tasks:**
- [ ] **`infra/docker/.env.example`**:
  - Remove per-service port variables (29 → 8)
  - Consolidate duplicate variables (shared DB URLs, Redis URLs)
  - New port variables: `ENGINE_CORE_PORT=8081`, `ENGINE_DATA_PORT=8100`, etc.
- [ ] **`apps/frontend/.env.example`**:
  - API base URLs unchanged (Nginx routes handle mapping)
  - Verify no frontend references to individual service ports
- [ ] **`application.yml` per consolidated unit**:
  - Merge configs from constituent services
  - Profile-based overrides: `dev`, `staging`, `prod`
  - Shared DB connection pool settings
- [ ] **`requirements.txt` / `pyproject.toml`** for Python units:
  - Merge all atomizer dependencies into single file
  - Merge all generator dependencies into single file
  - Remove duplicates, pin versions

**AC:**
- [ ] All env example files reflect consolidated architecture
- [ ] No references to old service names in config files

---

## P8-W3-002: Flyway Migration Ordering & Verification

**Type:** Database
**Effort:** 1 MD

**Tasks:**
- [ ] **Migration directory consolidation**:
  - engine-core: Merge migrations from ms-auth, ms-admin, ms-batch, ms-ver, ms-audit
  - engine-data: Merge from ms-sink-tbl, ms-sink-doc, ms-sink-log, ms-qry, ms-dash, ms-srch, ms-tmpl
  - engine-reporting: Merge from ms-lifecycle, ms-period, ms-form, ms-tmpl-pptx, ms-notif
- [ ] **Ordering convention**:
  - Prefix: `V{original_service_order}_{original_version}__description.sql`
  - e.g., `V01_001__create_auth_tables.sql`, `V02_001__create_admin_tables.sql`
- [ ] **Verification script**: SQL script to validate all tables/indexes exist after consolidated migration run
- [ ] **Rollback scripts**: Verify rollback order is correct
- [ ] Test on fresh database: drop all → run consolidated migrations → verify schema

**AC:**
- [ ] Fresh database build completes with all tables from all modules
- [ ] Migration order respects inter-table foreign key dependencies
- [ ] Rollback in reverse order succeeds

---

## P8-W3-003: Documentation & Migration Guide

**Type:** Documentation
**Effort:** 1 MD

**Tasks:**
- [ ] **Migration guide**: `docs/CONSOLIDATION_GUIDE.md`
  - Mapping table: old service name → new unit name
  - Port mapping: old ports → new ports
  - Dapr app-id mapping: old → new
  - Step-by-step migration checklist for existing deployments
- [ ] **Updated architecture diagram** (Mermaid in project_charter.md or separate doc):
  - 8 units instead of 29 services
  - Simplified communication paths
- [ ] **README.md updates**:
  - Each consolidated unit gets README with merged module descriptions
  - Root README updated with new architecture overview
- [ ] Update `docs/roadmap.md` with P8 phase

**AC:**
- [ ] Migration guide covers all service mappings
- [ ] Architecture diagram reflects consolidated state

---

## P8-W3-004: Old Service Directory Cleanup

**Type:** Cleanup
**Effort:** 0.5 MD

**Tasks:**
- [ ] **Archive old directories**:
  - `apps/engine/microservices/units/` → archive or delete after consolidated units verified
  - `apps/processor/microservices/units/` → archive or delete
  - Keep git history (directories removed in commit, not force-deleted)
- [ ] **Remove orphaned Dapr configs**: `infra/docker/dapr/ms-*/` old configs
- [ ] **Remove orphaned Dockerfiles**: Individual service Dockerfiles replaced by consolidated ones
- [ ] **.gitignore**: Clean up any service-specific ignores

**AC:**
- [ ] No old service directories remain in active codebase
- [ ] Git history preserved for all removed files
- [ ] No broken references from remaining code to deleted paths

---
