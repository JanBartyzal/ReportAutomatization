/**
 * Theme exports
 * ReportAutomatization Design System
 */

// Brand & Fluent themes
export { reportBrand, lightTheme, darkTheme, heroGradient } from './brandTokens';

// Status colors
export { STATUS_COLORS, getStatusColors, getStatusLabel, STATUS_LABELS } from './statusColors';
export type { ReportStatusKey } from './statusColors';

// Chart colors
export { getChartColor, getChartPalette, injectChartCssProperties } from './chartColors';

// Design tokens
export { spacing, typography, fontFamily, fontFamilyNumeric, elevation, elevationDark, borderRadius, motion, layout, breakpoints } from './tokens';
export type { BreakpointKey } from './tokens';

// Theme provider & hook
export { ThemeProvider, useAppTheme } from './ThemeProvider';
export type { ThemeMode } from './ThemeProvider';

// CSS injection
export { injectBrandCssProperties } from './cssInjection';
