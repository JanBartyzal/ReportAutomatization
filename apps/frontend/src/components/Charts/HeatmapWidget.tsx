import React from 'react';
import { ResponsiveHeatMap } from '@nivo/heatmap';
import { AggregatedData } from '@reportplatform/types';
import { nivoLightTheme } from './nivoTheme';

interface HeatmapWidgetProps {
    data: AggregatedData;
    title?: string;
}

export const HeatmapWidget: React.FC<HeatmapWidgetProps> = ({ data }) => {
    // Transform AggregatedData into Nivo Heatmap format
    // Nivo HeatMap expects: { id: 'rowId', data: [ { x: 'colId', y: value }, ... ] }
    
    // Assume columns[0] is rows, columns[1] is columns, columns[2] is value
    const rowKey = data.columns[0];
    const colKey = data.columns[1];
    const valKey = data.columns[2] || data.columns[1];

    // Group by rowId
    const groupedData: Record<string, any[]> = {};
    data.rows.forEach(row => {
        const rowId = String(row[rowKey]);
        if (!groupedData[rowId]) {
            groupedData[rowId] = [];
        }
        groupedData[rowId].push({
            x: String(row[colKey]),
            y: row[valKey]
        });
    });

    const nivoData = Object.keys(groupedData).map(id => ({
        id,
        data: groupedData[id]
    }));

    return (
        <div style={{ height: '350px', width: '100%' }}>
            <ResponsiveHeatMap
                data={nivoData}
                margin={{ top: 30, right: 30, bottom: 30, left: 30 }}
                valueFormat=">-.2s"
                axisTop={{
                    tickSize: 5,
                    tickPadding: 5,
                    tickRotation: -45,
                    legend: '',
                    legendOffset: 46
                }}
                axisLeft={{
                    tickSize: 5,
                    tickPadding: 5,
                    tickRotation: 0,
                    legend: '',
                    legendPosition: 'middle',
                    legendOffset: -40
                }}
                colors={{
                    type: 'diverging',
                    scheme: 'red_yellow_blue',
                    divergeAt: 0.5,
                    minValue: -100,
                    maxValue: 100
                }}
                emptyColor="#eeeeee"
                borderWidth={1}
                borderColor={{
                    from: 'color',
                    modifiers: [['darker', 0.4]]
                }}
                labelTextColor={{
                    from: 'color',
                    modifiers: [['darker', 1.8]]
                }}
                theme={nivoLightTheme}
            />
        </div>
    );
};
