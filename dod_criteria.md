# Definition of Done (DoD) - PPTX-AI-Analyzer

**Project:** PPTX-AI-Analyzer  
**Version:** 1.0  
**Last Updated:** 2026-01-25  
**Status:** Active

---

## 📌 Purpose

This document defines the mandatory quality gates that **MUST** be satisfied before any feature, sprint, or release can be considered "Done". No work item can be merged, deployed, or marked as complete without meeting these criteria.

---

## 🎯 Universal Criteria (All Features)

The following criteria apply to **every** feature, bugfix, or enhancement:

### ✅ 1. Code Quality

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **Type Annotations** | 100% of Python functions must have full type hints (parameters + return types) | `mypy --strict` passes with 0 errors |
| **Linting** | Code must pass linting with zero violations | `ruff check .` (Python), `eslint .` (React) returns exit code 0 |
| **Code Formatting** | Code must be auto-formatted consistently | `black .` (Python), `prettier --check .` (React) |
| **Complexity** | No function exceeds cyclomatic complexity of 10 | `radon cc -nb` reports no `F` grades |
| **Docstrings** | All public functions/classes have docstrings (Google style) | Manual review in PR |
| **Naming Conventions** | Follow PEP 8 (Python) and Airbnb style (React) | Linter enforces this |

**🚫 Blocker:** Code with type errors or linting violations **CANNOT** be merged.

---

### ✅ 2. Data Accuracy & Integrity

> [!IMPORTANT]
> This is a **data-heavy** project. Garbage in = garbage out. These criteria are **non-negotiable**.

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **OCR Confidence Threshold** | All OCR-extracted data must have confidence score **≥85%** | Automated validation in extraction pipeline |
| **AI Vision Fallback** | If OCR confidence <85%, AI Vision (GPT-4o/LLaVA) must be triggered automatically | N8N workflow test confirms fallback logic |
| **JSON Schema Validation** | All extracted tables must validate against `table_schema.json` | `jsonschema` library validation in unit tests |
| **Source Metadata Preservation** | Every record MUST include: `filename`, `slide_number`, `review_batch_id` (if applicable), `extraction_timestamp` | Database schema enforces NOT NULL constraints |
| **Data Deduplication** | Identical images (same SHA-256 hash) must retrieve cached results from Redis | Integration test verifies cache hit |
| **Null Handling** | Missing data must be explicitly marked as `null` (not empty string `""`) | Schema validation enforces this |
| **Date Formats** | All dates stored as ISO 8601 (`YYYY-MM-DD`) | Database CHECK constraint + validation |
| **Pseudo-Table Detection** | All extracted tables must validate against `table_schema.json` | `jsonschema` library validation in unit tests |
| **Type Annotations** | Use strict Python typing (List[Dict[str, Any]], etc.). | Manual review in PR |
| **Accuracy** | If the grid structure is ambiguous (confidence < 0.85), flag it for AI Vision processing instead of algorithmic parsing. | Manual review in PR |
| **Performance** | This geometric calculation must be fast (< 200ms per slide). Do not use LLMs for the coordinate calculation itself, use pure Python logic. | Manual review in PR |
| **Testing** | Create a unit test with a mock list of shapes representing a 3x3 grid. | Manual review in PR |

**🚫 Blocker:** Features that corrupt source metadata or violate schema **CANNOT** be deployed.

---

### ✅ 3. Security

> [!CAUTION]
> Security violations can lead to data breaches. These are **zero-tolerance** requirements.

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **No Hardcoded Secrets** | Zero secrets in code. All credentials via environment variables | `git secrets --scan` + pre-commit hook |
| **Environment Variable Validation** | All required env vars documented in `.env.example` | CI/CD checks for missing vars on startup |
| **JWT Validation** | All API endpoints (except `/health`) MUST validate Azure Entra ID tokens | Integration tests with invalid tokens return 401 |
| **Token Expiration** | Expired tokens MUST be rejected | Test with expired JWT returns 401 |
| **Row Level Security (RLS)** | Database queries MUST filter by `user_id` (users see only their data) | SQL audit confirms RLS policies active |
| **HTTPS Only** | All production traffic over HTTPS (no HTTP fallback) | Deployment config validation |
| **Dependency Scanning** | No critical/high CVEs in dependencies | `safety check` (Python), `npm audit` (React) |
| **CORS Configuration** | Frontend origin whitelisted explicitly (no `*` wildcard in prod) | API tests from unauthorized origin return 403 |

**🚫 Blocker:** Any security vulnerability rated **High** or **Critical** blocks deployment.

---

### ✅ 4. Testing

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **Unit Test Coverage** | Minimum **80%** code coverage for business logic | `pytest --cov` reports ≥80% |
| **Critical Path Coverage** | **100%** coverage for table extraction functions | Coverage report reviewed in PR |
| **Integration Tests** | All N8N workflows must have automated integration tests | CI/CD runs workflow tests against mock data |
| **API Contract Tests** | All API endpoints tested with valid/invalid inputs | Postman/Newman collection passes |
| **Edge Case Testing** | Test with malformed PPTX files, corrupted images, 0-row tables | Documented test cases in `tests/edge_cases/` |
| **Performance Tests** | Processing time for 100-slide PPTX <2 minutes | Benchmark test in CI/CD |

**🚫 Blocker:** Features with <80% coverage or failing critical tests **CANNOT** be merged.

---

### ✅ 5. Documentation

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **API Documentation** | All endpoints documented in OpenAPI/Swagger | `/docs` route renders correctly |
| **README Updates** | README.md updated if setup/config changes | Manual PR review |
| **Changelog** | `CHANGELOG.md` updated with feature/fix summary | PR template enforces this |
| **Architecture Diagrams** | Updated if new components added | Diagram sync verified in PR |

---

## 🎯 Feature-Specific Criteria

### **Feature: Pseudo-Table Detection**

| Criterion | Requirement |
|-----------|-------------|
| **Detection Accuracy** | Successfully identify ≥90% of pseudo-tables with ≥3 rows and ≥2 columns in test dataset |
| **Row/Column Reconstruction** | Correctly reconstruct row/column relationships (grid layout) with <5% error rate |
| **Alignment Preservation** | Preserve text alignment (left/center/right) for each cell |
| **Graceful Degradation** | Handle irregular grids (merged cells, varying column counts) without crashing |
| **Performance** | Processing time increase <20% compared to native table extraction |

**Validation:**
- Run against labeled test dataset of 50 presentations with known pseudo-tables
- Confusion matrix shows precision ≥90%, recall ≥85%

---

### **Feature: Template Aggregation**

| Criterion | Requirement |
|-----------|-------------|
| **Schema Matching Accuracy** | Identify identical table schemas with ≥95% accuracy (fuzzy matching for minor variations) |
| **Column Mismatch Handling** | Gracefully handle missing columns by filling with `null` values |
| **Source Traceability** | Preserve original `filename`, `slide_number`, `review_batch_id` for each aggregated row |
| **Conflict Resolution** | When schemas mismatch, log warning and skip aggregation (do NOT guess) |
| **Master Table Validation** | Aggregated table validates against unified schema |

**Validation:**
- Test with 10 PPTX files containing identical schema (e.g., monthly reports from different regions)
- Verify all rows aggregated correctly with source metadata intact

---

### **Feature: Batch Processing**

| Criterion | Requirement |
|-----------|-------------|
| **Batch ID Assignment** | All extracted data MUST have a `review_batch_id` (NOT NULL constraint) |
| **Batch Isolation** | Data from different batches must not leak between queries |
| **Batch Metadata** | Each batch has: `batch_name` ("Review 1", "Review 2", etc.), `created_at`, `created_by_user_id` |
| **API Endpoints** | Implement: `POST /batches/`, `GET /batches/`, `GET /batches/{id}/data` |
| **Concurrent Batches** | Support processing multiple batches in parallel without conflicts |

**Validation:**
- Create 3 batches, ingest data into each, verify queries return correct batch-scoped data

---

### **Feature: Supplemental Excel Ingestion**

| Criterion | Requirement |
|-----------|-------------|
| **File Format Support** | Accept `.xlsx` and `.xls` files |
| **Multi-Sheet Support** | Process all sheets in a workbook |
| **Header Detection** | Automatically detect header row (first row heuristic + ML-based validation) |
| **Formula Handling** | Store calculated values (not formulas) in JSON output |
| **Merged Cell Handling** | Expand merged cells into individual cells with duplicated values |
| **Batch Association** | Excel data tagged with current `review_batch_id` |

**Validation:**
- Test with complex Excel file (5 sheets, merged cells, formulas)
- Verify JSON output matches expected structure

---

## 🚀 Release Criteria

Before any production release, the following **MUST** be completed:

- [ ] All DoD criteria above satisfied for included features
- [ ] Security scan (SAST + dependency audit) passes with 0 critical/high issues
- [ ] Smoke tests pass in staging environment
- [ ] Database migration scripts tested on staging database
- [ ] Rollback plan documented and tested
- [ ] Monitoring dashboards configured (API latency, error rates, queue depth)
- [ ] User-facing documentation updated (if applicable)
- [ ] Stakeholder approval obtained

---

## 📋 Checklist Template (For Each PR/Feature)

Use this checklist in every Pull Request description:

```markdown
## Definition of Done Checklist

### Code Quality
- [ ] Type annotations: `mypy --strict` passes
- [ ] Linting: `ruff check .` passes
- [ ] Formatting: `black .` applied
- [ ] Docstrings: All public functions documented

### Data Accuracy
- [ ] OCR confidence threshold ≥85% enforced
- [ ] JSON schema validation passes
- [ ] Source metadata (`filename`, `slide_number`, `review_batch_id`) preserved
- [ ] Null handling validated

### Security
- [ ] No hardcoded secrets (`git secrets --scan` passes)
- [ ] JWT validation on all protected endpoints
- [ ] RLS policies verified
- [ ] Dependency scan clean (`safety check` / `npm audit`)

### Testing
- [ ] Unit test coverage ≥80%
- [ ] Integration tests for N8N workflows pass
- [ ] API contract tests pass
- [ ] Edge cases tested

### Documentation
- [ ] API docs updated (`/docs` route)
- [ ] `CHANGELOG.md` updated
- [ ] Architecture diagrams updated (if applicable)

### Feature-Specific (if applicable)
- [ ] [Add feature-specific criteria from above]
```

---

## 🔒 Enforcement

- **Automated Gates:** CI/CD pipeline enforces linting, testing, security scans (blocks merge if fails)
- **Manual Review:** Architects review data accuracy, schema validation, RLS implementation in PR reviews
- **Staging Deployment:** All features deployed to staging for 48 hours before production
- **Incident Response:** If DoD was bypassed and caused production issues, retrospective + process improvement required

---

## 📞 Questions or Exceptions?

If a DoD criterion cannot be met due to legitimate constraints, escalate to:
1. **Tech Lead** (technical exceptions)
2. **Project Owner** (business exceptions)

**Default stance:** DoD is **non-negotiable** unless explicitly waived with documented justification.

---


**Document Version:** 1.0  
**Classification:** Internal Use Only  
**Next Review Date:** 2026-04-25

---

## 📋 Newly Identified DoD Criteria (Added 2026-01-25)

Based on comprehensive code review and refactoring, the following additional criteria are now mandatory:

### ✅ 6. Architecture & Patterns

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **Centralized Configuration** | All environment variables managed via Pydantic `BaseSettings` (no `os.getenv()` scattered throughout code) | Manual code review + grep for `os.getenv` |
| **Dependency Injection** | All database sessions via `Depends(get_db)`, no global `SessionLocal()` instances | Manual code review of routers |
| **Async/Await Consistency** | All async functions must `await` async dependencies (Redis cache, database queries if using async drivers) | `mypy` async checks |
| **Service Layer Separation** | Business logic extracted to service classes (not embedded in route handlers) | Manual code review |
| **No Comments in Other Languages** | All code comments, docstrings, and documentation in English | Manual review |
| **Proper Session Management** | Database sessions closed via `finally` blocks or  context managers | Manual code review |
| **Module Docstrings** | All Python modules have docstrings explaining their purpose | `pydocstyle` check |
| **Type Aliases for Complex Types** | Complex nested types defined as TypedDict or Pydantic models (not inline `Dict[str, Any]`) | Manual code review |

### ✅ 7. File Upload Security

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **File Type Validation** | Validate file extensions against whitelist (e.g., only `.pptx`, `.xlsx`) | Code review + security audit |
| **File Size Limits** | Enforce maximum upload size (e.g., 50MB per file) | FastAPI `File(...)` max_length parameter |
| **Path Traversal Prevention** | Use `os.path.abspath()` + validate against base directory | Security audit |
| **Filename Sanitization** | Remove/replace special characters, prevent directory traversal (`../`) | Code review |
| **Malware Scanning** | Optional but recommended: integrate antivirus scan on upload | Manual verification if implemented |

### ✅ 8. Row Level Security (RLS) Enforcement

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **User OID Filtering** | All database queries MUST filter by `user.oid` (e.g., `WHERE oid = :user_oid`) | SQL audit + manual code review |
| **Test RLS Isolation** | Integration tests verify users cannot access other users' data | Automated test suite |
| **Admin Override Documented** | If admins can see all data, this must be explicitly documented and tested | Code review + documentation |

### ✅ 9. Error Handling & Observability

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **Structured Logging** | All log messages use logger (not `print()`), with appropriate levels (INFO, WARNING, ERROR) | Grep for `print(` statements |
| **Request ID Tracking** | Each request logs with correlation ID for tracing | Middleware implementation review |
| **Exception to HTTP Mapping** | Custom exceptions mapped to appropriate HTTP status codes (404, 403, 400, 500) | Exception handler code review |
| **No Sensitive Data in Logs** | Logs do not contain passwords, tokens, or PII | Security audit of logging statements |

### ✅ 10. Development Workflow

| Criterion | Requirement | Validation Method |
|-----------|-------------|-------------------|
| **Git Pre-Commit Hooks** | Automated checks for secrets, linting, formatting before commit | `.pre-commit-config.yaml` exists and runs |
| **Environment Variable Documentation** | `.env.example` file documents all required env vars with descriptions | File exists and is up-to-date |
| **Local Development Setup** | `README.md` includes step-by-step local setup instructions (Docker Compose) | Manual verification |
| **Alembic Migrations** | Database schema changes via Alembic (not manual SQL or `create_all()`) | `alembic/versions/` directory exists |

---

## 🚫 Anti-Patterns to Avoid

The following patterns were found during code review and are now **explicitly prohibited**:

1. ❌ **Global Database Sessions**: `db = SessionLocal()` at module level
2. ❌ **Mixing Sync/Async**: Calling sync Redis client from async endpoints
3. ❌ **Hardcoded Config**: `os.getenv("VAR", "default_value")` outside Settings
4. ❌ **Business Logic in Routes**: Complex processing directly in `@router` functions
5. ❌ **Silent Failures**: `try/except: pass` without logging
6. ❌ **String Interpolation in SQL**: `f"SELECT * FROM {table}"` (SQL injection risk)
7. ❌ **Missing Resource Cleanup**: File handles, DB connections not closed in `finally`
8. ❌ **Print Debugging**: `print()` statements instead of proper logging
9. ❌ **Commented-Out Code**: Dead code left in comments instead of removed
10. ❌ **Inconsistent (Czech/English Comments**: Mixed language code

---

**Revision History:**
- **v1.0 (2026-01-25)**: Added Architecture, File Upload Security, RLS, Error Handling, Development Workflow criteria based on code review findings

