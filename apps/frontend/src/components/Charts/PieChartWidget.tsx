import React from 'react';
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
} from 'recharts';
import { getChartPalette } from '../../theme/chartColors';
import { rechartsTooltipStyle } from './rechartsTheme';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';

interface PieChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const PieChartWidget: React.FC<PieChartWidgetProps> = ({ data }) => {
    const nameKey = data.columns[0];
    const valueKey = data.columns[1] || data.columns[0];
    const colors = [...getChartPalette()];

    return (
        <ChartWrapper height={350} showLegend={true} legendContent={<Legend />}>
            <PieChart>
                <Pie
                    data={data.rows}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey={valueKey}
                    nameKey={nameKey}
                    label
                >
                    {data.rows.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                    ))}
                </Pie>
                <Tooltip contentStyle={rechartsTooltipStyle} />
            </PieChart>
        </ChartWrapper>
    );
};
