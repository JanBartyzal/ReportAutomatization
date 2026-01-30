# Task: Refactor Frontend Structure
**Priority:** 🟡 MEDIUM / REFACTORING
**Required Agent:** Junior/Standard Frontend Developer
**Estimated Effort:** Low

## Context
`App.tsx` currently contains inline component definitions for pages like `Dashboard`, `Analytics`, and `Admin`. This violates the Single Responsibility Principle and Project Standards. These should be moved to dedicated files in the `pages` directory.

## Objectives
1.  **Extract Components:** Move inline page components from `App.tsx` to separate files.
2.  **Clean up `App.tsx`:** Import these components instead of defining them inline.

## Step-by-Step Instructions

### 1. Create Page Components
*   **Directory:** `frontend/src/pages/`
*   **Action:** Create the following files with basic functional components (Placeholder content is fine for now, just move the existing H1s):
    *   `Dashboard.tsx`
    *   `Analytics.tsx`
    *   `Admin.tsx`
*   **Content:**
    ```tsx
    // Example for Dashboard.tsx
    import React from 'react';
    
    export const Dashboard: React.FC = () => {
        return (
            <div className="p-4">
                <h1 className="text-2xl font-bold">Dashboard</h1>
                {/* Future dashboard content */}
            </div>
        );
    };
    ```

### 2. Update `App.tsx`
*   **Action:**
    *   Remove `const Dashboard = ...`, etc.
    *   Import the new components:
        `import { Dashboard } from './pages/Dashboard';`
    *   Ensure Routing still works correctly.

## Definition of Done
*   [x] No inline component definitions (`const X = () => ...`) in `App.tsx` (except maybe top-level wrappers if strictly necessary, but pages must go).
*   [x] `src/pages` contains the new files.
*   [x] Application compiles and navigates correctly.
