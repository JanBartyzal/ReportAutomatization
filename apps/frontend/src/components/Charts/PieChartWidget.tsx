import React from 'react';
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
} from 'recharts';
import { chartPalette } from '../../theme/brandTokens';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';

interface PieChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const PieChartWidget: React.FC<PieChartWidgetProps> = ({ data }) => {
    // Pie charts usually take two columns: name and value
    const nameKey = data.columns[0];
    const valueKey = data.columns[1] || data.columns[0];
    const colors = Object.values(chartPalette);

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
                <Tooltip 
                    contentStyle={{ 
                        borderRadius: '8px', 
                        border: 'none', 
                        boxShadow: '0 4px 12px rgba(0,0,0,0.1)' 
                    }}
                />
            </PieChart>
        </ChartWrapper>
    );
};
