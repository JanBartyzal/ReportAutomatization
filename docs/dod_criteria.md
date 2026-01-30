# Definition of Done (DoD) Criteria


**Context:** A task or feature is considered "COMPLETE" only when all applicable criteria below are met.

**Instruction for AI:** Before responding that a task is finished, review your work against this checklist.


## 1. Code Quality Check
- [ ] **Linting:** Code passes all linter checks (ESLint, Pylint, Roslyn) without errors or warnings.
- [ ] **Formatting:** Code is formatted according to the `project\_standards.md` (Prettier/Black/dotnet format).
- [ ] **No Dead Code:** Unused imports, variables, and commented-out code blocks are removed.
- [ ] **Type Safety:** No `any` types (TS) or missing type hints (Python) unless strictly justified.

## 2. Functionality & Integrity
- [ ] **Happy Path:** The code performs the requested function correctly.
- [ ] **Edge Cases:** Basic edge cases (null inputs, empty lists, network failures) are handled gracefully.
- [ ] **Security:** No secrets/API keys are hardcoded. Input validation is present.

## 3. Testing
- [ ] **Unit Tests:** New logic is covered by unit tests.
- [ ] **Pass Rate:** All specific tests related to the change pass successfully.
- [ ] **Mocking:** External services are mocked in tests; no real API calls during testing.

## 4. Documentation
- [ ] **Inline Comments:** Complex logic is commented in English.
- [ ] **Docstrings:** Classes and public functions have updated docstrings.
- [ ] **Module README:** If a new file/module was created, `README.md` is created or updated.

## 5. Project Specific Requirements
- [ ] {{ INSERT: e.g., Migration file created }}
- [ ] {{ INSERT: e.g., UI matches Figma design ID... }}
- [ ] {{ INSERT: e.g., Swagger definition updated }}

