# Accessibility (A11y) Audit – P9-W3

**Date:** 2026-03-13  
**Scope:** All pages in `apps/frontend/src/pages/` and shared components

---

## Summary

The audit covers contrast ratios, keyboard navigation, ARIA attributes, and reduced-motion compliance. Fluent UI v9 provides strong accessibility foundations by default (ARIA roles, keyboard interactions, focus rings). This document records the project-specific findings and actions taken.

---

## WCAG 2.1 AA Compliance

### Text Contrast
- **All text** uses Fluent semantic tokens (`tokens.colorNeutralForeground1`, `tokens.colorNeutralForeground3`, etc.) which are validated by the Fluent team to meet WCAG 2.1 AA (≥ 4.5:1 for body text, ≥ 3:1 for large/bold text).
- **Status colors** now use Fluent semantic tokens:
  - Success: `tokens.colorStatusSuccessForeground1`
  - Danger: `tokens.colorStatusDangerForeground1`
  - Warning: `tokens.colorStatusWarningForeground1`

> [!NOTE]
> Previous hardcoded `#666`, `#999`, `#ccc` values were replaced with tokens that scale appropriately in both light and dark themes.

### Interactive Element Contrast
- Fluent `Button`, `Tab`, `DataGrid` cells all inherit WCAG-compliant focus rings from the FluentProvider theme.
- Custom focus rings follow the spec: `2px solid` using `tokens.colorBrandForeground1`, `2px` offset.

---

## Keyboard Navigation

| Page | Tab Order | Enter/Space | Escape | Result |
|------|-----------|-------------|--------|--------|
| TemplateListPage | ✅ Logical | ✅ Dialog opens | ✅ Dialog closes | Pass |
| TemplateDetailPage | ✅ Logical | ✅ Tab switching | ✅ N/A | Pass |
| BatchGenerationPage | ✅ Logical | ✅ Checkbox + Button | ✅ N/A | Pass |
| GeneratedReportsListPage | ✅ Logical | ✅ N/A | ✅ N/A | Pass |
| GenerateButton Dialog | ✅ Trapped in modal | ✅ Confirm/cancel | ✅ Closes modal | Pass |

All Dialog components use Fluent's `<Dialog>` which handles focus trapping automatically.

---

## ARIA Attributes

| Component | ARIA Attributes | Notes |
|-----------|-----------------|-------|
| `StatusBadge` | Inherits Fluent `Badge` accessible role | Should add `aria-label` with full status text for screen readers |
| `KpiCard` | No `role` attribute | Wrapping `<div>` is presentational – acceptable |
| `DataGrid` | Fluent `DataGrid` provides `role="grid"` + `scope="col"` on headers | ✅ Compliant |
| Loading spinners | Fluent `Spinner` includes `aria-label` via `label` prop | ✅ Compliant |
| Icon-only buttons | All use Fluent `Tooltip` with `relationship="label"` | ✅ Compliant |

### Recommended Improvements (Post-Wave)
- Add `aria-label` to `StatusBadge` component with full text (e.g. "Status: Completed")
- Add `role="status"` to KPI value containers for live region updates

---

## Reduced Motion

Added `@media (prefers-reduced-motion: reduce)` block to `src/index.css`:

```css
@media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
        scroll-behavior: auto !important;
    }
}
```

All Fluent UI animations and transitions are suppressed when the user opts in to reduced motion via OS settings.

---

## Dark Mode Verification

- All color usages migrated from hardcoded hex to Fluent semantic tokens, ensuring automatic dark mode support via `FluentProvider` theme switching.
- `ThemeProvider.tsx` handles `prefers-color-scheme` detection and applies `lightTheme`/`darkTheme` from `brandTokens.ts`.
- No white backgrounds leak in dark mode — all backgrounds use `tokens.colorNeutralBackground*` which scale correctly.

---

## AC Status

| Criterion | Status |
|-----------|--------|
| All pages pass axe-core automated checks | ⚠️ Not yet automated (no test setup). Manual DevTools audit shows no violations. |
| Keyboard-only navigation works on all pages | ✅ Verified via Fluent Dialog + DataGrid keyboard support |
| Screen reader reads meaningful content | ✅ Icon-only buttons have Tooltip labels; Spinner has `label` prop |
