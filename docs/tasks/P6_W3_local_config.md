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
- [ ] Flyway migrations: add `scope` to forms, templates, reports tables
- [ ] Update RLS policies for scope-based access
- [ ] Seed data: sample local forms and templates
- [ ] Indexes for scope-based queries

---

## P6-W3-002: Feature Flags

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] Feature flag: `ENABLE_LOCAL_SCOPE` (disabled by default)
- [ ] Feature flag: `ENABLE_ADVANCED_COMPARISON` (disabled by default)
- [ ] UI hides local scope features when flag disabled
- [ ] Backend enforces flags on API level
