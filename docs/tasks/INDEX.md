# Task Index – PPTX Analyzer & Automation Platform

**Version:** 2.0
**Date:** 2026-03-12
**Total Tasks:** ~180 across 14 phases
**Total Estimated Effort:** ~640 MD (with AI assistance)

---

## Wave Strategy

Each phase is divided into waves by complexity and target AI agent:

| Wave | Target Agent | Complexity | Description |
|---|---|---|---|
| **Wave 1** | **Opus** | Hard | Core business logic, state machines, Saga patterns, security-critical code, AI integration |
| **Wave 2** | **Sonnet** | Medium | Clear-scope services, CRUD with business rules, service extensions |
| **Wave 3** | **Haiku / Gemini** | Easy | Configuration, Docker Compose, DB migrations, seed data, boilerplate |
| **Wave 4** | **Gemini Flash / MiniMax** | Frontend | React components, UI pages, API client functions, hooks, scripts |

---

## Phase Overview

| Phase | Name | Tasks | Effort (MD) | Status | Key Deliverable |
|---|---|---|---|---|---|
| **P0** | API Contracts & Protos | 29 | 22.5 | DONE | All `.proto` files + OpenAPI specs + TS types |
| **P1** | MVP Core | 21 | 95 | DONE | Upload PPTX → parse → store → view |
| **P2** | Extended Parsing | 13 | 49.5 | DONE | All formats + CQRS + dashboards |
| **P3a** | Intelligence & Admin | 9 | 64.5 | DONE | Schema mapping + AI + holding admin |
| **P3b** | Reporting Lifecycle | 9 | 43 | DONE | Report state machine + periods + deadlines |
| **P3c** | Form Builder | 11 | 58 | DONE | Dynamic forms + Excel import/export |
| **P4a** | Enterprise Features | 13 | 42 | DONE | Notifications, versioning, audit, search |
| **P4b** | PPTX Generation | 9 | 46 | DONE | Auto-generate reports from approved data |
| **P5** | DevOps & Onboarding | 10 | 29 | DONE | CI/CD, observability, production-ready |
| **P6** | Local Scope & Advanced | 10 | 36.5 | DONE | Subsidiary self-service + advanced analytics |
| **P7** | External Integrations & Data Optimization | ~18 | ~49 | TODO | Service-Now + Smart Persistence (FS23, FS24) |
| **P8** | Microservice Consolidation | ~18 | ~55 | TODO | 29 services → 8 deployment units |
| **P9** | Frontend Style Unification | ~13 | ~36 | TODO | Unified design system, JSON-configurable theme |
| **P10** | Technical Audit & Quality Gate | ~5 | ~20 | TODO | Full charter compliance verification |
| | | **TOTAL** | **~646 MD** | | |

---

## Completed Phases (P0–P6)

All task files moved to `docs/tasks/done/`:

### P0 – API Contracts
| File | Description |
|---|---|
| [P0_api_contracts.md](done/P0_api_contracts.md) | Proto definitions + OpenAPI specs |

### P1 – MVP Core (~95 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P1_W1_mvp_core_backend.md](done/P1_W1_mvp_core_backend.md) | Opus | ~56 MD |
| W2 | [P1_W2_mvp_sinks.md](done/P1_W2_mvp_sinks.md) | Sonnet | ~16 MD |
| W3 | [P1_W3_mvp_infra.md](done/P1_W3_mvp_infra.md) | Haiku/Gemini | ~6 MD |
| W4 | [P1_W4_mvp_frontend.md](done/P1_W4_mvp_frontend.md) | Flash/MiniMax | ~17 MD |

### P2 – Extended Parsing (~49.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P2_W1_parsing_complex.md](done/P2_W1_parsing_complex.md) | Opus | ~24 MD |
| W2 | [P2_W2_parsing_simple.md](done/P2_W2_parsing_simple.md) | Sonnet | ~14 MD |
| W3 | [P2_W3_parsing_infra.md](done/P2_W3_parsing_infra.md) | Haiku/Gemini | ~2.5 MD |
| W4 | [P2_W4_parsing_frontend.md](done/P2_W4_parsing_frontend.md) | Flash/MiniMax | ~9 MD |

### P3a – Intelligence & Admin (~64.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3a_W1_intelligence_core.md](done/P3a_W1_intelligence_core.md) | Opus | ~35 MD |
| W2 | [P3a_W2_admin_batch.md](done/P3a_W2_admin_batch.md) | Sonnet | ~20 MD |
| W3 | [P3a_W3_intelligence_config.md](done/P3a_W3_intelligence_config.md) | Haiku/Gemini | ~1.5 MD |
| W4 | [P3a_W4_intelligence_frontend.md](done/P3a_W4_intelligence_frontend.md) | Flash/MiniMax | ~8 MD |

### P3b – Reporting Lifecycle (~43 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3b_W1_lifecycle_core.md](done/P3b_W1_lifecycle_core.md) | Opus | ~25 MD |
| W2 | [P3b_W2_lifecycle_notif.md](done/P3b_W2_lifecycle_notif.md) | Sonnet | ~7 MD |
| W3 | [P3b_W3_lifecycle_config.md](done/P3b_W3_lifecycle_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P3b_W4_lifecycle_frontend.md](done/P3b_W4_lifecycle_frontend.md) | Flash/MiniMax | ~10 MD |

### P3c – Form Builder (~58 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3c_W1_form_engine.md](done/P3c_W1_form_engine.md) | Opus | ~30 MD |
| W2 | [P3c_W2_form_support.md](done/P3c_W2_form_support.md) | Sonnet | ~8 MD |
| W3 | [P3c_W3_form_config.md](done/P3c_W3_form_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P3c_W4_form_frontend.md](done/P3c_W4_form_frontend.md) | Flash/MiniMax | ~19 MD |

### P4a – Enterprise Features (~42 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P4a_W1_versioning_audit.md](done/P4a_W1_versioning_audit.md) | Opus | ~17 MD |
| W2 | [P4a_W2_notif_search.md](done/P4a_W2_notif_search.md) | Sonnet | ~12 MD |
| W3 | [P4a_W3_enterprise_config.md](done/P4a_W3_enterprise_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P4a_W4_enterprise_frontend.md](done/P4a_W4_enterprise_frontend.md) | Flash/MiniMax | ~12 MD |

### P4b – PPTX Generation (~46 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P4b_W1_generator_engine.md](done/P4b_W1_generator_engine.md) | Opus | ~31 MD |
| W2 | [P4b_W2_generator_data.md](done/P4b_W2_generator_data.md) | Sonnet | ~6 MD |
| W3 | [P4b_W3_generator_config.md](done/P4b_W3_generator_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P4b_W4_generator_frontend.md](done/P4b_W4_generator_frontend.md) | Flash/MiniMax | ~8 MD |

### P5 – DevOps & Onboarding (~29 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P5_W1_observability_cicd.md](done/P5_W1_observability_cicd.md) | Opus | ~15 MD |
| W2 | [P5_W2_monitoring.md](done/P5_W2_monitoring.md) | Sonnet | ~7 MD |
| W3 | [P5_W3_localdev_testing.md](done/P5_W3_localdev_testing.md) | Haiku/Gemini | ~5 MD |
| W4 | [P5_W4_devops_frontend.md](done/P5_W4_devops_frontend.md) | Flash/MiniMax | ~2 MD |

### P6 – Local Scope & Advanced (~36.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P6_W1_local_scope.md](done/P6_W1_local_scope.md) | Opus | ~22 MD |
| W2 | [P6_W2_local_data.md](done/P6_W2_local_data.md) | Sonnet | ~5 MD |
| W3 | [P6_W3_local_config.md](done/P6_W3_local_config.md) | Haiku/Gemini | ~1.5 MD |
| W4 | [P6_W4_local_frontend.md](done/P6_W4_local_frontend.md) | Flash/MiniMax | ~8 MD |

---

## Active Phases (P7–P10)

### P7 – External Integrations & Data Optimization (~49 MD)

> **Feature Sets:** FS23 (Service-Now Integration), FS24 (Smart Persistence Promotion)

| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P7_W1_servicenow_smartpersistence.md](P7_W1_servicenow_smartpersistence.md) | Opus | ~28 MD |
| W2 | [P7_W2_servicenow_config.md](P7_W2_servicenow_config.md) | Sonnet | ~8 MD |
| W3 | [P7_W3_servicenow_infra.md](P7_W3_servicenow_infra.md) | Haiku/Gemini | ~3 MD |
| W4 | [P7_W4_servicenow_frontend.md](P7_W4_servicenow_frontend.md) | Flash/MiniMax | ~10 MD |

**New Services:** MS-EXT-SNOW (Java), MS-GEN-XLS (Python)
**Extensions:** MS-ADMIN, MS-TMPL, MS-SINK-TBL, MS-ORCH, MS-DASH, MS-NOTIF

### P8 – Microservice Consolidation (~55 MD)

> **Goal:** Consolidate 29+ individual microservices into 8 deployment units (6 Java + 2 Python) to reduce operational complexity, sidecar overhead, and deployment surface.

| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P8_W1_consolidation_core.md](P8_W1_consolidation_core.md) | Opus | ~35 MD |
| W2 | [P8_W2_consolidation_infra.md](P8_W2_consolidation_infra.md) | Sonnet | ~12 MD |
| W3 | [P8_W3_consolidation_config.md](P8_W3_consolidation_config.md) | Haiku/Gemini | ~4 MD |
| W4 | [P8_W4_consolidation_scripts.md](P8_W4_consolidation_scripts.md) | Flash/MiniMax | ~4 MD |

**Consolidated Units:**

| # | Unit | Merged From | Tech |
|---|------|-------------|------|
| 1 | engine-core | MS-AUTH, MS-ADMIN, MS-BATCH, MS-VER, MS-AUDIT | Java 21 |
| 2 | engine-ingestor | MS-ING, MS-SCAN | Java 21 |
| 3 | engine-orchestrator | MS-ORCH | Java 21 |
| 4 | engine-data | MS-SINK-TBL/DOC/LOG, MS-QRY, MS-DASH, MS-SRCH, MS-TMPL | Java 21 |
| 5 | engine-reporting | MS-LIFECYCLE, MS-PERIOD, MS-FORM, MS-TMPL-PPTX, MS-NOTIF | Java 21 |
| 6 | engine-integrations | MS-EXT-SNOW | Java 21 |
| 7 | processor-atomizers | MS-ATM-PPTX/XLS/PDF/CSV/AI/CLN | Python |
| 8 | processor-generators | MS-GEN-PPTX, MS-GEN-XLS, MS-MCP | Python |

**Scripts (PowerShell + Bash):**
- `scripts/build.ps1` / `build.sh` — Build Docker images
- `scripts/deploy.ps1` / `deploy.sh` — Docker Compose deploy
- `scripts/test.ps1` / `test.sh` — Run unit/integration/E2E tests

### P9 – Frontend Style Unification (~36 MD)

> **Goal:** Refactor all frontend pages to use unified design tokens from `docs/UX-UI/` with JSON-configurable color scheme. Eliminate per-page ad-hoc styling.

| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P9_W1_style_unification.md](P9_W1_style_unification.md) | Opus | ~18 MD |
| W2 | [P9_W2_style_pages.md](P9_W2_style_pages.md) | Sonnet | ~15 MD |
| W3 | [P9_W3_style_cleanup.md](P9_W3_style_cleanup.md) | Haiku/Gemini | ~3 MD |

**Key Deliverables:**
- `theme-config.json` — single JSON file controlling all brand colors, status colors, chart palette
- Shared component library: StatusBadge, KpiCard, DataTable, PageHeader, FormField, ChartWrapper
- All 29 pages migrated to unified design system
- WCAG 2.1 AA compliance verified
- Dark mode working on all pages

### P10 – Technical Audit & Quality Gate (~20 MD)

> **Goal:** Comprehensive audit verifying all implementations match project_charter.md, DoD criteria, and STANDARDS.md. Gap analysis with remediation tasks.

| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P10_W1_technical_audit.md](P10_W1_technical_audit.md) | Opus | ~20 MD |

**Audit Scope:**
- FS01–FS24 feature completeness (every acceptance criterion checked)
- NFR compliance (performance, security, scalability, observability)
- DoD compliance per service (linting, docs, tests, coverage)
- Communication contract audit (proto/API consistency, Dapr routing)
- Remediation task generation with prioritized gap list

**Outputs:**
- `docs/audit/fs-completeness-report.md`
- `docs/audit/nfr-compliance-report.md`
- `docs/audit/dod-compliance-report.md`
- `docs/audit/communication-audit-report.md`
- `docs/audit/AUDIT_SUMMARY.md`
- `docs/tasks/P10_W2_remediation.md` (generated from findings)

---

## Dependency Graph

```
P0 (API Contracts)
 └─► P1 (MVP Core)
      ├─► P2 (Extended Parsing)
      │    └─► P3a (Intelligence & Admin)
      │         ├─► P3b (Reporting Lifecycle)
      │         │    └─► P3c (Form Builder)
      │         │         ├─► P4a (Enterprise Features)
      │         │         └─► P4b (PPTX Generation)
      │         └─► P3b ──► P3c
      └─► P5 (DevOps) [parallel with P2+]
           └─► P6 (Local Scope & Advanced)
                └─► P7 (Service-Now & Smart Persistence)
                     └─► P8 (Microservice Consolidation)
                          └─► P9 (Frontend Style Unification)
                               └─► P10 (Technical Audit)
```

---

## Cross-Cutting: UX/UI Design System

> **Všechny Wave 4 (Frontend) a P9 úlohy MUSÍ dodržovat projektový design system.**

| Dokument | Obsah |
|----------|-------|
| [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) | Brand palette (Crimson `#C4314B`), status/chart colors |
| [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) | Layout, typografie, spacing, elevace, formuláře, dark mode, a11y |
| [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) | Atomické a molekulární komponenty |
| [`docs/UX-UI/04-figma-pages.md`](../UX-UI/04-figma-pages.md) | Wireframy stránek + responsive breakpoints |

---

## Related Documents

- [Project Charter](../project_charter.md) – architecture, feature sets, NFR
- [Roadmap](../roadmap.md) – timeline and effort estimates
- [Standards](../STANDARDS.md) – coding standards and tech stack
- [DoD Criteria](../dod_criteria.md) – definition of done checklist
