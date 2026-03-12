# P6 – Wave 3: Local Scope Configuration (Haiku/Gemini)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1.5 MD
**Depends on:** P6-W1 (scope columns and RLS defined)

> Database migrations and feature flags for local scope rollout.

---

## P6-W3-001: Database Migrations for Scope

**Type:** Database
**Effort:** 1 MD

**Tasks:**
- [x] Flyway migrations: add `scope` to forms, templates, reports tables (Already done in V1-V3 migrations)
- [x] Update RLS policies for scope-based access (Already done in V2 migrations)
- [x] Seed data: sample local forms and templates (Created V3 seed migrations)
- [x] Indexes for scope-based queries (Already done in previous migrations)

**Migrations created:**
- [`apps/engine/microservices/units/ms-form/src/main/resources/db/migration/V3__seed_local_forms.sql`](../../apps/engine/microservices/units/ms-form/src/main/resources/db/migration/V3__seed_local_forms.sql) - Local forms seed data
- [`apps/engine/microservices/units/ms-tmpl-pptx/src/main/resources/db/migration/V3__seed_local_templates.sql`](../../apps/engine/microservices/units/ms-tmpl-pptx/src/main/resources/db/migration/V3__seed_local_templates.sql) - Local templates seed data

---

## P6-W3-002: Feature Flags

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [x] Feature flag: `ENABLE_LOCAL_SCOPE` (disabled by default)
- [x] Feature flag: `ENABLE_ADVANCED_COMPARISON` (disabled by default)
- [ ] UI hides local scope features when flag disabled (P6-W4 frontend task)
- [ ] Backend enforces flags on API level (P6-W4 frontend task)

**Configuration created:**
- [`apps/engine/microservices/units/ms-lifecycle/src/main/resources/db/migration/V4__add_feature_flags.sql`](../../apps/engine/microservices/units/ms-lifecycle/src/main/resources/db/migration/V4__add_feature_flags.sql) - Feature flags table with seed data
- [`apps/engine/microservices/units/ms-lifecycle/src/main/resources/application.yml`](../../apps/engine/microservices/units/ms-lifecycle/src/main/resources/application.yml) - Feature flag env vars
- [`apps/engine/microservices/units/ms-form/src/main/resources/application.yml`](../../apps/engine/microservices/units/ms-form/src/main/resources/application.yml) - Feature flag env vars
- [`apps/engine/microservices/units/ms-tmpl-pptx/src/main/resources/application.yml`](../../apps/engine/microservices/units/ms-tmpl-pptx/src/main/resources/application.yml) - Feature flag env vars

---

## Summary

| Task | Status | Notes |
|------|--------|-------|
| Database Migrations | ✅ Complete | Scope columns, RLS, indexes already existed. Added seed data for LOCAL scope. |
| Feature Flags | ✅ Complete | Created `feature_flags` table with two flags (disabled by default). Added env var configs to services. |
| UI Integration | ⏳ Pending | P6-W4 will implement frontend feature flag checks |
