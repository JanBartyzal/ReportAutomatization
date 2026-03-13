# P9 – Wave 3: Frontend Style Unification – Cleanup & Verification (Haiku/Gemini)

**Phase:** P9 – Frontend Style Unification
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~3 MD
**Depends on:** P9-W1 (theme system), P9-W2 (page migration)

> CSS cleanup, dead style removal, accessibility audit, dark mode verification.

---

## P9-W3-001: Dead CSS & Style Cleanup

**Type:** Cleanup
**Effort:** 1 MD

**Tasks:**
- [ ] **Remove unused CSS files**:
  - Scan for CSS files not imported anywhere
  - Remove `responsive.css` if replaced by `useBreakpoint()` hook
  - Remove any page-specific `.css` files replaced by `makeStyles`
- [ ] **Remove Tailwind remnants**:
  - Verify `tailwind.config.js` can be removed (if fully migrated to Fluent)
  - Remove `postcss.config.js` Tailwind plugin if unused
  - Remove Tailwind from `package.json` dependencies
  - Grep for any remaining Tailwind classes (`className="[a-z]+-[a-z0-9]+"`)
- [ ] **Remove hardcoded colors**:
  - Grep all `.tsx` and `.css` files for hex color patterns (`#[0-9a-fA-F]{3,8}`)
  - Replace any remaining hardcoded colors with Fluent tokens or CSS variables
- [ ] **Remove inline styles**:
  - Grep for `style={{` patterns with color/spacing values
  - Replace with `makeStyles()` or Fluent token references
  - Exception: dynamic runtime values (position, dimensions from data) may remain inline

**AC:**
- [ ] Zero unused CSS files in codebase
- [ ] Zero hardcoded hex colors outside of `theme-config.json` and `theme/` directory
- [ ] Zero Tailwind classes in components (if fully migrated)
- [ ] ESLint passes without style-related warnings

---

## P9-W3-002: Accessibility (A11y) Verification

**Type:** Quality Assurance
**Effort:** 1 MD

**Tasks:**
- [ ] **Contrast audit**:
  - Verify all text meets WCAG 2.1 AA: ≥ 4.5:1 contrast ratio
  - Verify interactive elements: ≥ 3:1 contrast ratio
  - Test with both light and dark themes
  - Use browser DevTools or axe-core for automated checks
- [ ] **Keyboard navigation**:
  - Tab order logical on all pages
  - Enter/Space for actions, Escape for close
  - Focus ring: `2px solid Brand90`, offset `2px`
  - No focus traps except in modals
- [ ] **ARIA attributes**:
  - Data tables: `role="grid"`, header cells `scope="col"`
  - Status badges: `aria-label` with full status text
  - Loading states: `aria-busy="true"` on containers
  - Icons without text: `aria-label` present
  - Live KPI updates: `role="status"`
- [ ] **`prefers-reduced-motion`**:
  - All animations respect `prefers-reduced-motion: reduce`
  - Only opacity transitions remain in reduced mode
- [ ] Document findings in `docs/a11y-audit.md`

**AC:**
- [ ] All pages pass axe-core automated checks (zero violations)
- [ ] Keyboard-only navigation works on all pages
- [ ] Screen reader reads meaningful content for all interactive elements

---

## P9-W3-003: Dark Mode & Theme Switching Verification

**Type:** Quality Assurance
**Effort:** 0.5 MD

**Tasks:**
- [ ] **Light → Dark mode toggle**:
  - Test every page in both modes
  - Verify no white/light backgrounds leak in dark mode
  - Verify no dark text on dark background
  - Glassmorphism effects work in dark mode (lower opacity)
- [ ] **System preference**: `prefers-color-scheme` correctly detected
- [ ] **Charts in dark mode**: All chart series visible with sufficient contrast
- [ ] **Shadows in dark mode**: Opacity reduced (0.3→0.2 range)
- [ ] Screenshot comparison: take light/dark screenshots of all pages for review

**AC:**
- [ ] Zero visual issues in dark mode
- [ ] Theme toggle works without page reload
- [ ] System preference respected on initial load

---

## P9-W3-004: Visual Regression Testing Setup

**Type:** Testing Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] **Playwright visual snapshots**:
  - Baseline screenshots for all pages (light + dark)
  - CI check: compare new screenshots against baseline
  - Threshold: <0.1% pixel difference triggers failure
- [ ] **Component stories** (if Storybook present):
  - Stories for all shared components (StatusBadge, KpiCard, DataTable, etc.)
  - Dark mode variant stories
- [ ] **Test matrix**:
  - Viewports: xs (375px), md (768px), lg (1280px)
  - Themes: light, dark
  - States: empty, loading, loaded, error

**AC:**
- [ ] Visual regression baseline established
- [ ] CI catches unintended visual changes
- [ ] All viewports and themes covered

---
