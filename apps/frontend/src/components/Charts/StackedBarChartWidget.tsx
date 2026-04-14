import React from 'react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
} from 'recharts';
import { getChartPalette } from '../../theme/chartColors';
import { rechartsAxisProps, rechartsGridProps, rechartsTooltipStyle } from './rechartsTheme';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';
import { getChartMapping } from './chartDataUtils';

interface StackedBarChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const StackedBarChartWidget: React.FC<StackedBarChartWidgetProps> = ({ data }) => {
    const chartData = data.rows;
    const { xKey, seriesKeys } = getChartMapping(data);
    const colors = [...getChartPalette()];

    return (
        <ChartWrapper>
            <BarChart
                data={chartData}
                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
            >
                <CartesianGrid {...rechartsGridProps} />
                <XAxis dataKey={xKey} {...rechartsAxisProps} />
                <YAxis {...rechartsAxisProps} />
                <Tooltip contentStyle={rechartsTooltipStyle} />
                <Legend />
                {seriesKeys.map((key, index) => (
                    <Bar
                        key={key}
                        dataKey={key}
                        stackId="stack"
                        fill={colors[index % colors.length]}
                    />
                ))}
            </BarChart>
        </ChartWrapper>
    );
};
