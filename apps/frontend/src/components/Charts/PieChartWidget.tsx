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
import { getPieMapping } from './chartDataUtils';

interface PieChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const PieChartWidget: React.FC<PieChartWidgetProps> = ({ data }) => {
    const { nameKey, valueKey } = getPieMapping(data);
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
