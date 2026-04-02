import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
} from 'recharts';
import { getChartPalette } from '../../theme/chartColors';
import { rechartsAxisProps, rechartsGridProps, rechartsTooltipStyle } from './rechartsTheme';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';
import { getChartMapping } from './chartDataUtils';

interface LineChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const LineChartWidget: React.FC<LineChartWidgetProps> = ({ data }) => {
    const chartData = data.rows;
    const { xKey, seriesKeys } = getChartMapping(data);
    const colors = [...getChartPalette()];

    return (
        <ChartWrapper>
            <LineChart
                data={chartData}
                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
            >
                <CartesianGrid {...rechartsGridProps} />
                <XAxis dataKey={xKey} {...rechartsAxisProps} />
                <YAxis {...rechartsAxisProps} />
                <Tooltip contentStyle={rechartsTooltipStyle} />
                {seriesKeys.map((key, index) => (
                    <Line
                        key={key}
                        type="monotone"
                        dataKey={key}
                        stroke={colors[index % colors.length]}
                        strokeWidth={2}
                        dot={{ r: 4 }}
                        activeDot={{ r: 6 }}
                    />
                ))}
            </LineChart>
        </ChartWrapper>
    );
};
