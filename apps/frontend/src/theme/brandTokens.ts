/**
 * ReportAutomatization Brand Tokens
 * Derived from theme-config.json — brand palette
 * Primary brand color: Crimson #C4314B (slot 90)
 */

import { type BrandVariants, createLightTheme, createDarkTheme } from '@fluentui/react-components';
import themeConfig from './theme-config.json';

/**
 * Build BrandVariants from JSON config.
 * BrandVariants requires slots 10–160 (16 values).
 */
export const reportBrand: BrandVariants = {
  10:  themeConfig.brand['10'],
  20:  themeConfig.brand['20'],
  30:  themeConfig.brand['30'],
  40:  themeConfig.brand['40'],
  50:  themeConfig.brand['50'],
  60:  themeConfig.brand['60'],
  70:  themeConfig.brand['70'],
  80:  themeConfig.brand['80'],
  90:  themeConfig.brand['90'],
  100: themeConfig.brand['100'],
  110: themeConfig.brand['110'],
  120: themeConfig.brand['120'],
  130: themeConfig.brand['130'],
  140: themeConfig.brand['140'],
  150: themeConfig.brand['150'],
  160: themeConfig.brand['160'],
};

export const lightTheme = createLightTheme(reportBrand);
export const darkTheme = createDarkTheme(reportBrand);

/**
 * Hero gradient per theme-config.json
 */
export const heroGradient = `linear-gradient(${themeConfig.heroGradient.angle}deg, ${themeConfig.heroGradient.from} 0%, ${themeConfig.heroGradient.to} 100%)`;
