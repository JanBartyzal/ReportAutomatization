/**
 * ChartWrapper — theme-aware chart container per design system
 * Injects chart palette from JSON config, handles dark mode,
 * consistent padding/border, fade-in + scale animation on mount (300ms)
 */
import React from 'react';
import { ResponsiveContainer } from 'recharts';
import { makeStyles, tokens } from '@fluentui/react-components';
import { elevation } from '../../theme/tokens';

const useStyles = makeStyles({
    wrapper: {
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
        backgroundColor: tokens.colorNeutralBackground1,
        borderRadius: '8px',
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        padding: '16px',
        boxShadow: elevation.level1,
        animationName: {
            from: { opacity: 0, transform: 'scale(0.98)' },
            to: { opacity: 1, transform: 'scale(1)' },
        },
        animationDuration: '300ms',
        animationTimingFunction: 'ease-out',
        animationFillMode: 'both',
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
    legendContent,
}) => {
    const styles = useStyles();

    return (
        <div 
            className={styles.wrapper} 
            style={{ height: typeof height === 'number' ? height + 48 : height }}
            data-testid="chart-wrapper"
        >
            <div className={styles.chartArea}>
                <ResponsiveContainer width="100%" height="100%">
                    {children as React.ReactElement}
                </ResponsiveContainer>
            </div>
            {showLegend && legendContent && (
                <div className={styles.legendArea}>{legendContent}</div>
            )}
        </div>
    );
};
