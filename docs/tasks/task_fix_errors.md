# Task: Post-Refactoring Stabilization & Bug Fixing

**Type:** Bug Fixing / QA
**Target Component:** `backend/`
**Reference Standard:** `dod_criteria.md`

---

## 1. Context
We have just completed a major refactoring of the backend to a modular structure (Routers/Services). The structure is good, BUT the application is failing to start or run tests due to implementation errors introduced during the migration.

## 2. Objective
Fix the specific errors provided in the logs. make the application build and run successfully.

## 3. Strict Rules (Constraints)
1.  **NO Structural Changes:** Do NOT move files, rename folders, or change the architecture anymore. Work within the current existing structure.
2.  **Fix Imports:** Most errors are likely due to broken relative/absolute imports after moving files. Fix them.
3.  **Fix Circular Dependencies:** If services import each other, refactor the logic inside functions or use `TYPE_CHECKING` blocks.
4.  **Minimal Changes:** Touch only the lines necessary to fix the error.

## 4. Input Data
I will provide:
1.  The current file structure (tree).
2.  The content of the failing files.
3.  The **Traceback / Error Log** from the terminal.

## 5. Definition of Done for this Task
- The specific error passed in the prompt is resolved.
- The application/container starts without crashing on that specific error.