/**
 * Chart palette utilities — driven by theme-config.json
 */
import themeConfig from './theme-config.json';

const palette: readonly string[] = themeConfig.chart;

/**
 * Get chart color by index (wraps around if index > palette length)
 */
export function getChartColor(index: number): string {
  return palette[index % palette.length];
}

/**
 * Get entire chart palette array
 */
export function getChartPalette(): readonly string[] {
  return palette;
}

/**
 * Inject chart palette as CSS custom properties on :root
 * Called once at theme initialization.
 */
export function injectChartCssProperties(): void {
  const root = document.documentElement;
  palette.forEach((color, i) => {
    root.style.setProperty(`--chart-${i + 1}`, color);
  });
}
