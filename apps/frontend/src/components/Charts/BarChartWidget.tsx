import React from 'react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Cell,
} from 'recharts';
import { chartPalette } from '../../theme/brandTokens';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';

interface BarChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const BarChartWidget: React.FC<BarChartWidgetProps> = ({ data }) => {
    const chartData = data.rows;
    // Assume first column is X axis (dimension), others are series
    const xKey = data.columns[0];
    const seriesKeys = data.columns.slice(1);
    const colors = Object.values(chartPalette);

    return (
        <ChartWrapper>
            <BarChart
                data={chartData}
                margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
            >
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis 
                    dataKey={xKey} 
                    fontSize={12} 
                    tick={{ fill: 'var(--colorNeutralForeground3)' }}
                    axisLine={false}
                    tickLine={false}
                />
                <YAxis 
                    fontSize={12} 
                    tick={{ fill: 'var(--colorNeutralForeground3)' }}
                    axisLine={false}
                    tickLine={false}
                />
                <Tooltip 
                    contentStyle={{ 
                        borderRadius: '8px', 
                        border: 'none', 
                        boxShadow: '0 4px 12px rgba(0,0,0,0.1)' 
                    }}
                />
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
