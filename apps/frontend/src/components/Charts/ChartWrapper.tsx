import React from 'react';
import { ResponsiveContainer } from 'recharts';
import { makeStyles, tokens, Body2 } from '@fluentui/react-components';

const useStyles = makeStyles({
    wrapper: {
        width: '100%',
        height: '300px',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    chartArea: {
        flex: 1,
        width: '100%',
        minHeight: 0,
    },
    legendArea: {
        display: 'flex',
        justifyContent: 'center',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalM,
        paddingTop: tokens.spacingVerticalS,
    },
});

interface ChartWrapperProps {
    children: React.ReactNode;
    height?: number | string;
    showLegend?: boolean;
    legendContent?: React.ReactNode;
}

export const ChartWrapper: React.FC<ChartWrapperProps> = ({ 
    children, 
    height = 300, 
    showLegend = false,
    legendContent 
}) => {
    const styles = useStyles();

    return (
        <div className={styles.wrapper} style={{ height }}>
            <div className={styles.chartArea}>
                <ResponsiveContainer width="100%" height="100%">
                    {children as any}
                </ResponsiveContainer>
            </div>
            {showLegend && legendContent && (
                <div className={styles.legendArea}>
                    {legendContent}
                </div>
            )}
        </div>
    );
};
