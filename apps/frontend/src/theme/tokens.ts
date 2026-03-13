/**
 * Design tokens derived from docs/UX-UI/02-design-system.md
 * All values aligned with the project design system specification.
 */

/* ─── Spacing (8px grid) ─── */
export const spacing = {
  xxs: '2px',
  xs: '4px',
  s: '8px',
  m: '16px',
  l: '24px',
  xl: '32px',
  xxl: '40px',
  xxxl: '48px',
} as const;

/* ─── Typography ─── */
export const fontFamily = "'Inter', 'Roboto', 'Segoe UI Variable', 'Segoe UI', system-ui, -apple-system, sans-serif";
export const fontFamilyNumeric = "'Inter', 'Roboto Mono', 'Segoe UI', monospace";

export const typography = {
  display:  { size: '32px', weight: '700', lineHeight: '1.2' },
  title1:   { size: '24px', weight: '600', lineHeight: '1.3' },
  title2:   { size: '20px', weight: '600', lineHeight: '1.4' },
  title3:   { size: '16px', weight: '600', lineHeight: '1.5' },
  body1:    { size: '14px', weight: '400', lineHeight: '1.5' },
  body2:    { size: '12px', weight: '400', lineHeight: '1.5' },
  caption:  { size: '10px', weight: '400', lineHeight: '1.4' },
} as const;

/* ─── Elevation (shadow levels) ─── */
export const elevation = {
  level0: 'none',
  level1: '0 1px 2px rgba(0, 0, 0, 0.06)',
  level2: '0 2px 8px rgba(0, 0, 0, 0.08)',
  level3: '0 4px 16px rgba(0, 0, 0, 0.12)',
  level4: '0 8px 32px rgba(0, 0, 0, 0.16)',
} as const;

export const elevationDark = {
  level0: 'none',
  level1: '0 1px 2px rgba(0, 0, 0, 0.12)',
  level2: '0 2px 8px rgba(0, 0, 0, 0.16)',
  level3: '0 4px 16px rgba(0, 0, 0, 0.24)',
  level4: '0 8px 32px rgba(0, 0, 0, 0.32)',
} as const;

/* ─── Border Radius ─── */
export const borderRadius = {
  none: '0px',
  sm: '4px',
  md: '8px',
  lg: '12px',
  xl: '16px',
  pill: '999px',
} as const;

/* ─── Motion / Animation ─── */
export const motion = {
  micro:  { duration: '100ms', easing: 'ease-out' },
  short:  { duration: '200ms', easing: 'ease-in-out' },
  medium: { duration: '300ms', easing: 'ease-in-out' },
  long:   { duration: '500ms', easing: 'ease-in-out' },
} as const;

/* ─── Layout ─── */
export const layout = {
  maxWidth: '1400px',
  columns: 12,
  gap: '24px',
  sidebarExpanded: '260px',
  sidebarCollapsed: '60px',
  topNavHeight: '64px',
} as const;

/* ─── Breakpoints ─── */
export const breakpoints = {
  xs: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
} as const;

export type BreakpointKey = keyof typeof breakpoints;
