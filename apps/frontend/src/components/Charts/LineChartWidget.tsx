import React from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
} from 'recharts';
import { chartPalette } from '../../theme/brandTokens';
import { ChartWrapper } from './ChartWrapper';
import { AggregatedData } from '@reportplatform/types';

interface LineChartWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const LineChartWidget: React.FC<LineChartWidgetProps> = ({ data }) => {
    const chartData = data.rows;
    const xKey = data.columns[0];
    const seriesKeys = data.columns.slice(1);
    const colors = Object.values(chartPalette);

    return (
        <ChartWrapper>
            <LineChart
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
