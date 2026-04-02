import type { AggregatedData } from '@reportplatform/types';

export interface ChartDataMapping {
    xKey: string;
    seriesKeys: string[];
}

export function getChartMapping(data: AggregatedData): ChartDataMapping {
    const hasLabelX = data.columns.includes('LabelX');
    const hasLabelY = data.columns.includes('LabelY');

    if (hasLabelX && hasLabelY) {
        const seriesKeys = data.columns.filter(col => col !== 'LabelX' && col !== 'LabelY');
        if (seriesKeys.length === 0) {
            seriesKeys.push('LabelY');
        }
        return {
            xKey: 'LabelX',
            seriesKeys,
        };
    }

    const xKey = data.columns[0];
    const seriesKeys = data.columns.slice(1);
    return { xKey, seriesKeys };
}

export function getPieMapping(data: AggregatedData): { nameKey: string; valueKey: string } {
    const hasLabelX = data.columns.includes('LabelX');
    const hasLabelY = data.columns.includes('LabelY');

    if (hasLabelX && hasLabelY) {
        return {
            nameKey: 'LabelX',
            valueKey: 'LabelY',
        };
    }

    return {
        nameKey: data.columns[0],
        valueKey: data.columns[1] || data.columns[0],
    };
}