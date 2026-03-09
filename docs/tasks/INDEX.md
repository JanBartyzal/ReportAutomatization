# Task Index – PPTX Analyzer & Automation Platform

**Version:** 1.0
**Date:** 2026-03-09
**Total Tasks:** 125 across 10 phases
**Total Estimated Effort:** ~464 MD (with AI assistance)

---

## Wave Strategy

Each phase is divided into 4 waves by complexity and target AI agent:

| Wave | Target Agent | Complexity | Description |
|---|---|---|---|
| **Wave 1** | **Opus** | Hard | Core business logic, state machines, Saga patterns, security-critical code, AI integration |
| **Wave 2** | **Sonnet** | Medium | Clear-scope services, CRUD with business rules, service extensions |
| **Wave 3** | **Haiku / Gemini** | Easy | Configuration, Docker Compose, DB migrations, seed data, boilerplate |
| **Wave 4** | **Gemini Flash / MiniMax** | Frontend | React components, UI pages, API client functions, hooks |

**Execution Order:** Within each phase, Wave 1 starts first (sets contracts and core logic). Waves 2-4 can often run in parallel after Wave 1 establishes the foundation.

---

## Phase Overview

| Phase | Name | Waves | Tasks | Effort (MD) | Key Deliverable |
|---|---|---|---|---|---|
| **P0** | API Contracts & Protos | [P0_api_contracts.md](P0_api_contracts.md) | 29 | 22.5 | All `.proto` files + OpenAPI specs + TS types |
| **P1** | MVP Core | W1–W4 (see below) | 21 | 95 | Upload PPTX → parse → store → view |
| **P2** | Extended Parsing | W1–W4 (see below) | 13 | 49.5 | All formats + CQRS + dashboards |
| **P3a** | Intelligence & Admin | W1–W4 (see below) | 9 | 64.5 | Schema mapping + AI + holding admin |
| **P3b** | Reporting Lifecycle | W1–W4 (see below) | 9 | 43 | Report state machine + periods + deadlines |
| **P3c** | Form Builder | W1–W4 (see below) | 11 | 58 | Dynamic forms + Excel import/export |
| **P4a** | Enterprise Features | W1–W4 (see below) | 13 | 42 | Notifications, versioning, audit, search |
| **P4b** | PPTX Generation | W1–W4 (see below) | 9 | 46 | Auto-generate reports from approved data |
| **P5** | DevOps & Onboarding | W1–W4 (see below) | 10 | 29 | CI/CD, observability, production-ready |
| **P6** | Local Scope & Advanced | W1–W4 (see below) | 10 | 36.5 | Subsidiary self-service + advanced analytics |
| | | **TOTAL** | **134** | **~486 MD** | |

---

## Wave Files

### P1 – MVP Core (~95 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P1_W1_mvp_core_backend.md](P1_W1_mvp_core_backend.md) | Opus | ~56 MD |
| W2 | [P1_W2_mvp_sinks.md](P1_W2_mvp_sinks.md) | Sonnet | ~16 MD |
| W3 | [P1_W3_mvp_infra.md](P1_W3_mvp_infra.md) | Haiku/Gemini | ~6 MD |
| W4 | [P1_W4_mvp_frontend.md](P1_W4_mvp_frontend.md) | Flash/MiniMax | ~17 MD |

### P2 – Extended Parsing (~49.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P2_W1_parsing_complex.md](P2_W1_parsing_complex.md) | Opus | ~24 MD |
| W2 | [P2_W2_parsing_simple.md](P2_W2_parsing_simple.md) | Sonnet | ~14 MD |
| W3 | [P2_W3_parsing_infra.md](P2_W3_parsing_infra.md) | Haiku/Gemini | ~2.5 MD |
| W4 | [P2_W4_parsing_frontend.md](P2_W4_parsing_frontend.md) | Flash/MiniMax | ~9 MD |

### P3a – Intelligence & Admin (~64.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3a_W1_intelligence_core.md](P3a_W1_intelligence_core.md) | Opus | ~35 MD |
| W2 | [P3a_W2_admin_batch.md](P3a_W2_admin_batch.md) | Sonnet | ~20 MD |
| W3 | [P3a_W3_intelligence_config.md](P3a_W3_intelligence_config.md) | Haiku/Gemini | ~1.5 MD |
| W4 | [P3a_W4_intelligence_frontend.md](P3a_W4_intelligence_frontend.md) | Flash/MiniMax | ~8 MD |

### P3b – Reporting Lifecycle (~43 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3b_W1_lifecycle_core.md](P3b_W1_lifecycle_core.md) | Opus | ~25 MD |
| W2 | [P3b_W2_lifecycle_notif.md](P3b_W2_lifecycle_notif.md) | Sonnet | ~7 MD |
| W3 | [P3b_W3_lifecycle_config.md](P3b_W3_lifecycle_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P3b_W4_lifecycle_frontend.md](P3b_W4_lifecycle_frontend.md) | Flash/MiniMax | ~10 MD |

### P3c – Form Builder (~58 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P3c_W1_form_engine.md](P3c_W1_form_engine.md) | Opus | ~30 MD |
| W2 | [P3c_W2_form_support.md](P3c_W2_form_support.md) | Sonnet | ~8 MD |
| W3 | [P3c_W3_form_config.md](P3c_W3_form_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P3c_W4_form_frontend.md](P3c_W4_form_frontend.md) | Flash/MiniMax | ~19 MD |

### P4a – Enterprise Features (~42 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P4a_W1_versioning_audit.md](P4a_W1_versioning_audit.md) | Opus | ~17 MD |
| W2 | [P4a_W2_notif_search.md](P4a_W2_notif_search.md) | Sonnet | ~12 MD |
| W3 | [P4a_W3_enterprise_config.md](P4a_W3_enterprise_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P4a_W4_enterprise_frontend.md](P4a_W4_enterprise_frontend.md) | Flash/MiniMax | ~12 MD |

### P4b – PPTX Generation (~46 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P4b_W1_generator_engine.md](P4b_W1_generator_engine.md) | Opus | ~31 MD |
| W2 | [P4b_W2_generator_data.md](P4b_W2_generator_data.md) | Sonnet | ~6 MD |
| W3 | [P4b_W3_generator_config.md](P4b_W3_generator_config.md) | Haiku/Gemini | ~1 MD |
| W4 | [P4b_W4_generator_frontend.md](P4b_W4_generator_frontend.md) | Flash/MiniMax | ~8 MD |

### P5 – DevOps & Onboarding (~29 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P5_W1_observability_cicd.md](P5_W1_observability_cicd.md) | Opus | ~15 MD |
| W2 | [P5_W2_monitoring.md](P5_W2_monitoring.md) | Sonnet | ~7 MD |
| W3 | [P5_W3_localdev_testing.md](P5_W3_localdev_testing.md) | Haiku/Gemini | ~5 MD |
| W4 | [P5_W4_devops_frontend.md](P5_W4_devops_frontend.md) | Flash/MiniMax | ~2 MD |

### P6 – Local Scope & Advanced (~36.5 MD)
| Wave | File | Agent | Effort |
|---|---|---|---|
| W1 | [P6_W1_local_scope.md](P6_W1_local_scope.md) | Opus | ~22 MD |
| W2 | [P6_W2_local_data.md](P6_W2_local_data.md) | Sonnet | ~5 MD |
| W3 | [P6_W3_local_config.md](P6_W3_local_config.md) | Haiku/Gemini | ~1.5 MD |
| W4 | [P6_W4_local_frontend.md](P6_W4_local_frontend.md) | Flash/MiniMax | ~8 MD |

---

## Effort by Agent Type

| Agent | Phase Waves | Total Tasks | Total Effort (MD) |
|---|---|---|---|
| **Opus** (Wave 1) | P0-W1, P1-W1, P2-W1, P3a-W1, P3b-W1, P3c-W1, P4a-W1, P4b-W1, P5-W1, P6-W1 | ~48 | ~249 |
| **Sonnet** (Wave 2) | P0-W2, P1-W2, P2-W2, P3a-W2, P3b-W2, P3c-W2, P4a-W2, P4b-W2, P5-W2, P6-W2 | ~34 | ~113 |
| **Haiku/Gemini** (Wave 3) | P0-W3, P1-W3, P2-W3, P3a-W3, P3b-W3, P3c-W3, P4a-W3, P4b-W3, P5-W3, P6-W3 | ~26 | ~27 |
| **Gemini Flash/MiniMax** (Wave 4) | P0-W4, P1-W4, P2-W4, P3a-W4, P3b-W4, P3c-W4, P4a-W4, P4b-W4, P5-W4, P6-W4 | ~26 | ~97 |

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
      └─► P5 (DevOps) [can start partially in parallel with P2+]
           └─► P6 (Local Scope & Advanced)
```

**Parallelization Opportunities:**
- P3a and P3b can run in parallel (independent service sets)
- P4a and P4b can run in parallel
- P5 DevOps work can start alongside P2/P3 (observability, CI/CD)
- Within each phase, Waves 2-4 can run in parallel after Wave 1 completes

---

## Microservice Coverage by Phase

| Service | P0 | P1 | P2 | P3a | P3b | P3c | P4a | P4b | P5 | P6 |
|---|---|---|---|---|---|---|---|---|---|---|
| MS-GW | proto | impl | | | routing | routing | routing | routing | | |
| MS-AUTH | proto+api | impl | | | | | | | | ext |
| MS-ING | proto+api | impl | | | | | | | | |
| MS-SCAN | proto | impl | | | | | | | | |
| MS-ORCH | proto | impl | ext | | ext | ext | | ext | | |
| MS-ATM-PPTX | proto | impl | | | | | | | | |
| MS-ATM-XLS | proto | | impl | | | | | | | |
| MS-ATM-PDF | proto | | impl | | | | | | | |
| MS-ATM-CSV | proto | | impl | | | | | | | |
| MS-ATM-AI | proto | | | impl | | | | | | |
| MS-ATM-CLN | | | impl | | | | | | | |
| MS-SINK-TBL | proto | impl | | | | ext | | | | |
| MS-SINK-DOC | proto | impl | | | | | | | | |
| MS-SINK-LOG | proto | impl | | | | | | | | |
| MS-QRY | api | | impl | | | ext | | ext | | ext |
| MS-DASH | api | | impl | | | | ext | ext | | ext |
| MS-SRCH | api | | | | | | impl | | | |
| MS-ADMIN | api | | | impl | | | | | | ext |
| MS-BATCH | | | | impl | | | | | | |
| MS-TMPL | proto | | | impl | | ext | | | | |
| MS-NOTIF | proto | | | | prep | | impl | | | |
| MS-LIFECYCLE | proto+api | | | | impl | | | | | ext |
| MS-PERIOD | api | | | | impl | | | | | ext |
| MS-FORM | api | | | | | impl | | | | ext |
| MS-TMPL-PPTX | | | | | | | | impl | | ext |
| MS-GEN-PPTX | proto | | | | | | | impl | | |
| MS-MCP | | | | impl | | | | | | |
| MS-VER | api | | | | | | impl | | | |
| MS-AUDIT | api | | | | | | impl | | | |
| MS-FE | types | impl | ext | ext | ext | ext | ext | ext | ext | ext |

**Legend:** proto = proto/API definition, impl = initial implementation, ext = extension, prep = preparation, routing = nginx config, types = TS types

---

## Quick Reference – Task Naming Convention

```
{Phase}-{Wave}-{Sequence}: {Title}

Examples:
  P0-W1-001: Proto Package Structure & Build Setup
  P1-W1-003: MS-ORCH – Custom Orchestrator (Core Engine)
  P3c-W4-002: Form Filling UI
```

---

## Related Documents

- [Project Charter](../project_charter.md) – architecture, feature sets, NFR
- [Roadmap](../roadmap.md) – timeline and effort estimates
- [Standards](../STANDARDS.md) – coding standards and tech stack
- [DoD Criteria](../dod_criteria.md) – definition of done checklist
- [Microservices Decomposition](../microservices_decomposition.md) – architecture diagrams
- [Task Breakdown (Legacy)](task_breakdown.md) – original detailed task breakdown for P1
