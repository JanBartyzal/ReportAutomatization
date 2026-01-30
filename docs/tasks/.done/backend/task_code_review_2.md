# Task: Comprehensive Backend Code Review & Refactoring

**Type:** Maintenance / Tech Debt
**Target Component:** `backend/` (FastAPI)
**Reference Standard:** `dod_criteria.md`

---

## 1. Objective
Perform a deep-dive code review of the current Python FastAPI backend. The goal is to identify architectural weaknesses, security gaps, and violations of the project's Definition of Done (DoD) before we proceed to complex feature development.

## 2. Scope of Review

### A. Architecture & Pattern enforcement
- **FastAPI Best Practices:** Are we using `APIRouter` correctly to split logic? Are we using Dependency Injection (`Depends`) for services (DB, Redis, AI)?
- **Service Layer Pattern:** Is business logic separated from API endpoints? (e.g., `services/` vs `routers/`).
- **Configuration:** Are we using Pydantic `BaseSettings` for env vars instead of raw `os.getenv` calls scattered around?

### B. Security (Critical)
- **Auth Implementation:** Verify `fastapi-azure-auth` integration. Are ALL private endpoints protected?
- **Data Safety:** Check for SQL Injection risks (ensure SQLAlchemy ORM is used correctly everywhere).
- **Secrets:** Ensure no API keys or connection strings are hardcoded or logged.

### C. Code Quality (DoD Alignment)
- **Typing:** Are all functions fully typed (`List`, `Dict`, `Optional`)?
- **Error Handling:** Is there a global exception handler? Are errors returned as structured JSON (HTTP 4xx/5xx) or do we leak Python tracebacks?
- **Async/Await:** Are we blocking the event loop anywhere? (e.g., performing heavy CPU tasks or synchronous IO inside an `async def`).

## 3. Review Process Instructions (For the AI Agent)

**Step 1: Analysis**
Analyze the provided code files against the criteria above. Do not rewrite anything yet.
Output a **"Code Review Report"** table:
| Severity | File/Module | Issue Description | Violation of DoD | Recommendation |
|----------|-------------|-------------------|------------------|----------------|
| High     | main.py     | Hardcoded secret  | Security         | Use env var    |

**Step 2: Refactoring Plan**
Propose a new directory structure if the current one is messy.
Example structure:
```text
backend/
  app/
    core/       # Config, Security, Logging
    db/         # Session, Models
    routers/    # API Endpoints
    services/   # Logic (Parsers, AI, OCR)
    schemas/    # Pydantic Models (Request/Response)