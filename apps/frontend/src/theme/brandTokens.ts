/**
 * ReportAutomatization Brand Tokens
 * Crimson Red brand palette per docs/UX-UI/00-project-color-overrides.md
 * 
 * Primary brand color: Crimson #C4314B (slot 90)
 */

import { BrandVariants, createLightTheme, createDarkTheme } from '@fluentui/react-components';

export const reportBrand: BrandVariants = {
    10: '#0D0206',
    20: '#23080F',
    30: '#3A0D19',
    40: '#521324',
    50: '#6B1830',
    60: '#841E3C',
    70: '#9D2444',
    80: '#B32A48',
    90: '#C4314B',  // PRIMARY — buttons, links, focus rings
    100: '#CF4D62',
    110: '#D96A79',
    120: '#E38790',
    130: '#EDA4A9',
    140: '#F5BFC2',
    150: '#F9D8DA',
    160: '#FDF0F1',
};

export const lightTheme = createLightTheme(reportBrand);
export const darkTheme = createDarkTheme(reportBrand);

/**
 * Chart Palette per docs/UX-UI/00-project-color-overrides.md
 * CSS Custom Properties for data visualization
 */
export const chartPalette = {
    chart1: '#C4314B',  // Crimson (primary series)
    chart2: '#107C10',  // Green (success/savings)
    chart3: '#0078D4',  // Azure Blue (category 3)
    chart4: '#D83B01',  // Orange (warning)
    chart5: '#6D28D9',  // Purple (category 5)
    chart6: '#008272',  // Teal (category 6)
    chart7: '#E3008C',  // Magenta (category 7)
    chart8: '#986F0B',  // Gold (category 8)
} as const;

/**
 * Hero gradient per 00-project-color-overrides.md
 * linear-gradient(135deg, Brand50 0%, Brand20 100%)
 */
export const heroGradient = 'linear-gradient(135deg, #6B1830 0%, #23080F 100%)';
