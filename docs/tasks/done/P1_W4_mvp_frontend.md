# P1 – Wave 4: Frontend MVP (Gemini Flash/MiniMax)

**Phase:** P1 – MVP Core
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~17 MD
**Depends on:** P1-W1-004 (auth), P1-W1-005 (upload) for integration

> React SPA with upload, basic viewer, and auth integration.

---

## UX/UI Design System (povinné)

> Veškerý frontend kód MUSÍ dodržovat projektový design system. Nepoužívat žádné ad-hoc styly.
>
> | Dokument | Obsah |
> |----------|-------|
> | [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) | Layout, typografie, spacing, elevace, formuláře, dark mode, a11y |
> | [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) | Atomické a kompozitní komponenty (Buttons, Inputs, Cards, DataGrid...) |
> | [`docs/UX-UI/04-figma-pages.md`](../UX-UI/04-figma-pages.md) | Wireframy stránek, responsive breakpoints |
> | [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) | **Projektově specifické barvy** (liší se od CIM!) |
> | [`docs/UX-UI/Figma/Components/tokens.json`](../UX-UI/Figma/Components/tokens.json) | Implementační design tokeny |
>
> **Klíčová pravidla:**
> - Brand barva: **Crimson `#C4314B`** (NE Azure Blue z CIM)
> - Styling: Fluent UI `makeStyles` + tokeny. Žádné hardcoded hex barvy, raw CSS pro komponenty, `!important`
> - Dark mode: Vše přes `FluentProvider` theme — automatické přepínání
> - A11y: WCAG 2.1 AA minimum (kontrast 4.5:1 text, 3:1 interaktivní)
>
> **Specificky pro P1-W4:**
> - P1-W4-001 (Project Setup): Nastavit Fluent theme s projektovými `BrandVariants` dle `00-project-color-overrides.md` sekce 6.1
> - P1-W4-002 (Auth): Login page dle wireframu v `04-figma-pages.md` (Login page)
> - P1-W4-003 (Upload UI): Status badges dle kontextových barev (`00-project-color-overrides.md` sekce 5)
> - P1-W4-004 (PPTX Viewer): Sidebar, tabulky dle DataGrid spec v `02-design-system.md` sekce 10
> - P1-W4-005 (Layout Shell): Sidebar + TopNav dle organisms v `03-figma-components.md`, glassmorphism na TopNav

---

## P1-W4-001: Frontend Project Setup

**Type:** Frontend Infrastructure
**Effort:** 2 MD
**Service:** apps/frontend

**Tasks:**
- [ ] Vite + React 18 + TypeScript project scaffolding
- [ ] Dependencies: Tailwind CSS, FluentUI, React Router DOM, Axios, TanStack Query, Zustand, MSAL
- [ ] `vite.config.ts` with proxy to API Gateway
- [ ] `.env.example` with `VITE_AZURE_CLIENT_ID`, `VITE_AZURE_TENANT_ID`, `VITE_API_BASE_URL`
- [ ] ESLint + Prettier configuration
- [ ] `tsconfig.json` with strict mode
- [ ] Dockerfile (multi-stage: build → nginx serve)
- [ ] Docker Compose entry (port 3000)

---

## P1-W4-002: MSAL Auth Integration

**Type:** Frontend Auth
**Effort:** 3 MD

**Tasks:**
- [ ] `MsalProvider` wrapping entire app
- [ ] `src/auth/msalConfig.ts` – MSAL configuration
- [ ] `src/auth/AuthProvider.tsx` – login/logout flow
- [ ] `AuthenticatedTemplate` / `UnauthenticatedTemplate` usage
- [ ] `interaction_in_progress` state handling (race condition prevention)
- [ ] Axios interceptor: `acquireTokenSilent` → Bearer header on every request
- [ ] Fallback: if no active account → `getAllAccounts()[0]`
- [ ] Login page with Microsoft SSO button
- [ ] Auto-redirect to login on 401
- [ ] Local dev bypass option (when `VITE_AUTH_BYPASS=true`)

**AC:**
- [ ] User can log in via Microsoft SSO
- [ ] Token auto-refreshes without user interaction
- [ ] All API calls include valid Bearer token

---

## P1-W4-003: File Upload UI

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Upload page with drag & drop zone (`react-dropzone`)
- [ ] File type validation (`.pptx`, `.xlsx`, `.pdf`, `.csv`)
- [ ] Upload progress bar (`onUploadProgress` from Axios)
- [ ] `upload_purpose` selector (PARSE / FORM_IMPORT)
- [ ] Post-upload: automatic file list refresh (React Query invalidation)
- [ ] File list table: filename, size, upload date, processing status
- [ ] Status badges: UPLOADED, PROCESSING, COMPLETED, FAILED, PARTIAL
- [ ] Error display for rejected files (infected, wrong type, too large)

**AC:**
- [ ] Drag & drop + click upload both work
- [ ] Progress bar visible during upload
- [ ] File list updates automatically after upload

---

## P1-W4-004: Basic PPTX Viewer

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] File detail page (`/files/{file_id}`)
- [ ] Slide-by-slide navigation (previous/next)
- [ ] Slide image preview (PNG from Blob Storage)
- [ ] Extracted text display (per slide)
- [ ] Table data display (extracted tables rendered as HTML tables)
- [ ] Speaker notes display (collapsible panel)
- [ ] Processing log timeline (per-step status)
- [ ] File metadata sidebar (filename, size, upload date, status)
- [ ] Loading states and error states

**AC:**
- [ ] User sees slide images with extracted text alongside
- [ ] Tables render correctly with headers
- [ ] Processing steps show success/failure timeline

---

## P1-W4-005: Layout & Navigation Shell

**Type:** Frontend UI
**Effort:** 3 MD

**Tasks:**
- [ ] App layout: sidebar navigation + main content area
- [ ] Navigation items: Dashboard (home), Files (upload + list), Settings
- [ ] Top bar: user avatar, org selector, logout button
- [ ] Org switcher dropdown (if user belongs to multiple orgs)
- [ ] Responsive design (desktop-first, tablet support)
- [ ] FluentUI theme setup with Tailwind integration
- [ ] 404 page, error boundary
- [ ] Loading spinner component

---

## P1-W4-006: React Query Data Layer

**Type:** Frontend Infrastructure
**Effort:** 2 MD

**Tasks:**
- [ ] React Query provider setup with default options
- [ ] Custom hooks from P0-W4 (useFiles, useAuth, useFileData, useSlides)
- [ ] Cache invalidation strategy:
  - After upload → invalidate file list
  - After org switch → invalidate all queries
- [ ] Polling for processing status (3s interval until COMPLETED/FAILED)
- [ ] Error handling: toast notifications for API errors
- [ ] Optimistic updates where applicable
