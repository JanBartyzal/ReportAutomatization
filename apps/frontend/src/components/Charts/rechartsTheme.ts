/**
 * Recharts theme configuration — derived from JSON theme config
 * Provides consistent tooltip, grid, axis, and font styling.
 */
import { getChartPalette } from '../../theme/chartColors';
import { fontFamily, fontFamilyNumeric } from '../../theme/tokens';

export function getRechartsColors(): string[] {
  return [...getChartPalette()];
}

export const rechartsAxisProps = {
  fontSize: 12,
  fontFamily,
  axisLine: false,
  tickLine: false,
} as const;

export const rechartsGridProps = {
  strokeDasharray: '3 3',
  vertical: false,
  stroke: '#E1DFDD',
} as const;

export const rechartsTooltipStyle = {
  borderRadius: '8px',
  border: 'none',
  boxShadow: '0 4px 16px rgba(0, 0, 0, 0.12)',
  fontFamily,
  fontSize: '12px',
} as const;

export const rechartsTooltipStyleDark = {
  ...rechartsTooltipStyle,
  backgroundColor: '#252423',
  color: '#FFFFFF',
  boxShadow: '0 4px 16px rgba(0, 0, 0, 0.24)',
} as const;

/**
 * Numeric label formatter using tabular figures font
 */
export const numericLabelStyle = {
  fontFamily: fontFamilyNumeric,
  fontVariantNumeric: 'tabular-nums',
} as const;
