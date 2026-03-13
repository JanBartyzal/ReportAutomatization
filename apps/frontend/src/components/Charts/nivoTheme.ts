/**
 * Nivo theme configuration — derived from JSON theme config
 * Provides light/dark theme objects for @nivo components.
 */
import type { Theme as NivoTheme } from '@nivo/core';
import { getChartPalette } from '../../theme/chartColors';
import { fontFamily } from '../../theme/tokens';

export function getNivoPalette(): string[] {
  return [...getChartPalette()];
}

export const nivoLightTheme: NivoTheme = {
  text: {
    fontFamily,
    fontSize: 12,
    fill: '#616161',
  },
  axis: {
    ticks: {
      text: { fontSize: 11, fill: '#616161', fontFamily },
      line: { stroke: '#E1DFDD', strokeWidth: 1 },
    },
    legend: {
      text: { fontSize: 12, fill: '#242424', fontFamily },
    },
  },
  grid: {
    line: { stroke: '#E1DFDD', strokeWidth: 1, strokeDasharray: '3 3' },
  },
  tooltip: {
    container: {
      background: '#FFFFFF',
      borderRadius: '8px',
      boxShadow: '0 4px 16px rgba(0, 0, 0, 0.12)',
      fontFamily,
      fontSize: 12,
    },
  },
  labels: {
    text: { fontFamily, fontSize: 12 },
  },
};

export const nivoDarkTheme: NivoTheme = {
  text: {
    fontFamily,
    fontSize: 12,
    fill: '#D6D6D6',
  },
  axis: {
    ticks: {
      text: { fontSize: 11, fill: '#ADADAD', fontFamily },
      line: { stroke: '#484644', strokeWidth: 1 },
    },
    legend: {
      text: { fontSize: 12, fill: '#FFFFFF', fontFamily },
    },
  },
  grid: {
    line: { stroke: '#484644', strokeWidth: 1, strokeDasharray: '3 3' },
  },
  tooltip: {
    container: {
      background: '#252423',
      color: '#FFFFFF',
      borderRadius: '8px',
      boxShadow: '0 4px 16px rgba(0, 0, 0, 0.24)',
      fontFamily,
      fontSize: 12,
    },
  },
  labels: {
    text: { fontFamily, fontSize: 12, fill: '#FFFFFF' },
  },
};
