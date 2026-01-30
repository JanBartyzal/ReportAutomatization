# Task: Implement Missing Frontend Essentials
**Priority:** 🟠 HIGH
**Required Agent:** Senior Frontend Developer
**Estimated Effort:** Medium

## Context
The Gap Analysis revealed that the application lacks a global `ErrorBoundary`, meaning any uncaptured React error will result in a White Screen of Death (WSOD), providing a poor user experience. Additionally, Client-Side Validation for file uploads (Type & Size) is required by the Charter but missing from the current implementation.

## Objectives
1.  **Implement Error Boundary:** Create a wrapper component to catch React lifecycle errors and display a friendly fallback UI.
2.  **Implement File Validation:** Create reusable utility functions for verifying file type (MIME/Extension) and size before upload.

## Step-by-Step Instructions

### 1. Create `ErrorBoundary` Component
*   **File:** `frontend/src/components/ErrorBoundary.tsx`
*   **Action:**
    *   Create a Class Component `ErrorBoundary` (React Error Boundaries must be classes).
    *   Implement `static getDerivedStateFromError(error)` to update state.
    *   Implement `componentDidCatch(error, errorInfo)` to log error (console or logging service).
    *   Render a nice Tailwind-styled UI when `hasError` is true (e.g., "Something went wrong" with a "Reload Page" button).

### 2. Integrate Error Boundary
*   **File:** `frontend/src/main.tsx` or `App.tsx`
*   **Action:**
    *   Wrap the entire application (or at least the `Routes`) in `<ErrorBoundary>`.
    *   Example:
        ```tsx
        <ErrorBoundary>
            <App msalInstance={msalInstance} />
        </ErrorBoundary>
        ```

### 3. Implement File Validation Utils
*   **File:** `frontend/src/utils/fileValidation.ts` (Create new)
*   **Action:**
    *   Implement function `validateFile(file: File, options: ValidationOptions): ValidationResult`.
    *   **Features:**
        *   Check `maxSizeInBytes`.
        *   Check `allowedExtensions` (e.g., `.pptx`, `.xlsx`).
        *   Return `{ isValid: boolean, error?: string }`.
*   **Reference:** Current upload pages (`ImportOpex`, `ExcelImport`) should use this utility in future tasks, but this task is just to create the capability.

## Definition of Done
*   [ ] `ErrorBoundary.tsx` exists and handles crashes gracefully.
*   [ ] Application is wrapped in `ErrorBoundary`.
*   [ ] `validateFile` utility is created and unit tested (or manually verified via console).
*   [ ] Code follows project styling (Tailwind).
