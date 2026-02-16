# Definition of Done (DoD) Criteria


**Context:** A task or feature is considered "COMPLETE" only when all applicable criteria below are met.

**Instruction for AI:** Before responding that a task is finished, review your work against this checklist.


A. Code Quality & Integrity
[ ] Linting & Formatting: Passes all checks (Checkstyle, Black, ESLint) per project_standards.md.
[ ] No Dead Code: Unused imports and commented-out blocks are removed.
[ ] Native Compatibility: (Java only) Code is verified to be "GraalVM Native-Image friendly".
[ ] Security: No hardcoded secrets; input validation present; Entra ID scopes verified.
[ ] Auth: Azure application manifest has token version set to v2.
[ ] Tokens: Axios interceptor successfully renews tokens without user interaction (Silent Flow).
[ ] UX: File upload triggers automatic refresh of file list (React Query invalidation).

B. Documentation & Mermaid
[ ] Module README: README.md updated with purpose and Dapr app-id.
[ ] Mermaid Diagrams: README includes Sequence or Flowchart diagrams for the feature.
[ ] Inline Docs: Complex logic explained (the why, not the what).

C. Testing & Results
[ ] Unit Tests: New logic covered; external services (Dapr/LiteLLM) are mocked.
[ ] test-result.md: This file is updated with the latest test execution summary and coverage %. Template file /docs/template-test-result.md
[ ] Happy Path & Edges: Verified functionality including null inputs and network failures.

