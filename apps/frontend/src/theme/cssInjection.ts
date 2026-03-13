/**
 * CSS custom property injection for non-React consumers.
 * Injects brand palette, semantic colors, and spacing tokens into :root.
 */
import themeConfig from './theme-config.json';
import { spacing, elevation, borderRadius } from './tokens';

/**
 * Inject brand palette and design tokens as CSS custom properties.
 * Called once at theme initialization.
 */
export function injectBrandCssProperties(): void {
  const root = document.documentElement;

  // Brand palette
  for (const [slot, color] of Object.entries(themeConfig.brand)) {
    root.style.setProperty(`--brand-${slot}`, color);
  }

  // Semantic colors
  for (const [name, colors] of Object.entries(themeConfig.semantic)) {
    root.style.setProperty(`--semantic-${name}-fg`, colors.foreground);
    root.style.setProperty(`--semantic-${name}-bg`, colors.background);
  }

  // Status colors
  for (const [status, colors] of Object.entries(themeConfig.status)) {
    root.style.setProperty(`--status-${status.toLowerCase()}-bg`, colors.bg);
    root.style.setProperty(`--status-${status.toLowerCase()}-text`, colors.text);
  }

  // Hero gradient
  const { from, to, angle } = themeConfig.heroGradient;
  root.style.setProperty('--hero-gradient', `linear-gradient(${angle}deg, ${from} 0%, ${to} 100%)`);

  // Spacing tokens
  for (const [name, value] of Object.entries(spacing)) {
    root.style.setProperty(`--spacing-${name}`, value);
  }

  // Elevation tokens
  for (const [name, value] of Object.entries(elevation)) {
    root.style.setProperty(`--elevation-${name}`, value);
  }

  // Border radius tokens
  for (const [name, value] of Object.entries(borderRadius)) {
    root.style.setProperty(`--radius-${name}`, value);
  }
}
