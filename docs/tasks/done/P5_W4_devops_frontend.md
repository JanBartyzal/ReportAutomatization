# P5 – Wave 4: Frontend Observability (Gemini Flash/MiniMax)

**Phase:** P5 – DevOps Maturity & Onboarding
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~2 MD

---

## UX/UI Design System (povinné)

> Veškerý frontend kód MUSÍ dodržovat projektový design system. Nepoužívat žádné ad-hoc styly.
>
> | Dokument | Obsah |
> |----------|-------|
> | [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) | Layout, typografie, spacing, elevace, formuláře, dark mode, a11y |
> | [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) | Atomické a kompozitní komponenty |
> | [`docs/UX-UI/04-figma-pages.md`](../UX-UI/04-figma-pages.md) | Wireframy stránek, responsive breakpoints |
> | [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) | **Projektově specifické barvy** (liší se od CIM!) |
> | [`docs/UX-UI/Figma/Components/tokens.json`](../UX-UI/Figma/Components/tokens.json) | Implementační design tokeny |
>
> **Klíčová pravidla:**
> - Brand barva: **Crimson `#C4314B`** (NE Azure Blue z CIM)
> - Styling: Fluent UI `makeStyles` + tokeny. Žádné hardcoded hex barvy, raw CSS, `!important`
> - Dark mode: Vše přes `FluentProvider` theme
> - A11y: WCAG 2.1 AA minimum
>
> **Specificky pro P5-W4:**
> - P5-W4-002 (Health Dashboard): Service status grid — Success/Danger/Warning sémantické barvy pro green/red/yellow indikátory

---

## P5-W4-001: Frontend Error Tracking & Analytics

**Type:** Frontend Feature
**Effort:** 1 MD

**Tasks:**
- [ ] Error boundary with structured error reporting
- [ ] OpenTelemetry browser instrumentation
- [ ] Performance metrics: page load, API call latency
- [ ] User session tracking (anonymous, no PII)

---

## P5-W4-002: Health Dashboard Page (Admin)

**Type:** Frontend Feature
**Effort:** 1 MD

**Tasks:**
- [ ] System health page (Admin only):
  - Service status grid (green/red/yellow)
  - Recent error log feed
  - DLQ depth display
  - Active workflow count
- [ ] Link to Grafana dashboards (external)
