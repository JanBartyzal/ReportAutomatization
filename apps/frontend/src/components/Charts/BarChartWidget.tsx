import React from 'react';
import {
    BarChart,
    Bar,
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

interface BarChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const BarChartWidget: React.FC<BarChartWidgetProps> = ({ data }) => {
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
                {seriesKeys.map((key, index) => (
                    <Bar
                        key={key}
                        dataKey={key}
                        fill={colors[index % colors.length]}
                        radius={[4, 4, 0, 0]}
                    />
                ))}
            </BarChart>
        </ChartWrapper>
    );
};
