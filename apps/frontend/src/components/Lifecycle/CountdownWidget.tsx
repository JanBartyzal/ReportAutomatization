import React, { useMemo } from 'react';
import {
    makeStyles,
    tokens,
    Title3,
    Body1,
    Body2,
    Badge,
    Card,
    ProgressBar,
} from '@fluentui/react-components';
import { TimerRegular } from '@fluentui/react-icons';

const useStyles = makeStyles({
    card: {
        padding: tokens.spacingHorizontalL,
        borderRadius: tokens.borderRadiusMedium,
        boxShadow: tokens.shadow2,
        height: 'fit-content',
        transition: 'box-shadow 0.2s ease-in-out',
        '&:hover': {
            boxShadow: tokens.shadow4,
        }
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalS,
    },
    periodCode: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
    },
    value: {
        fontSize: '32px',
        fontWeight: tokens.fontWeightBold,
        lineHeight: 1,
        marginBottom: tokens.spacingHorizontalXS,
    },
    subtitle: {
        color: tokens.colorNeutralForeground4,
        marginBottom: tokens.spacingHorizontalM,
    },
    dateRange: {
        display: 'flex',
        justifyContent: 'space-between',
        marginBottom: tokens.spacingHorizontalS,
    },
    progress: {
        marginTop: tokens.spacingHorizontalS,
    },
    caption: {
        color: tokens.colorNeutralForeground4,
        fontSize: '11px',
        marginTop: tokens.spacingHorizontalXS,
    },
});

interface CountdownWidgetProps {
    /** Period display name */
    label: string;
    /** Optional period code (e.g. "Q1-2026") */
    periodCode?: string;
    /** Period start date (ISO string or Date) */
    startDate: string | Date;
    /** Period end date (ISO string or Date) */
    endDate: string | Date;
    /** Submission deadline (ISO string or Date) */
    deadline: string | Date;
    /** Period status */
    status?: string;
}

// Legacy props for backward compatibility
interface LegacyCountdownWidgetProps {
    daysRemaining: number;
    totalDays: number;
    label: string;
}

function isLegacyProps(props: CountdownWidgetProps | LegacyCountdownWidgetProps): props is LegacyCountdownWidgetProps {
    return 'daysRemaining' in props && 'totalDays' in props;
}

export const CountdownWidget: React.FC<CountdownWidgetProps | LegacyCountdownWidgetProps> = (props) => {
    const styles = useStyles();

    const computed = useMemo(() => {
        if (isLegacyProps(props)) {
            const elapsed = props.totalDays - props.daysRemaining;
            return {
                daysRemaining: props.daysRemaining,
                totalDays: props.totalDays,
                progress: Math.max(0, Math.min(1, elapsed / props.totalDays)),
                label: props.label,
                periodCode: undefined as string | undefined,
                startStr: '',
                endStr: '',
                deadlineStr: '',
                status: undefined as string | undefined,
            };
        }

        const now = new Date();
        const start = new Date(props.startDate);
        const end = new Date(props.endDate);
        const deadline = new Date(props.deadline);

        const totalMs = end.getTime() - start.getTime();
        const elapsedMs = now.getTime() - start.getTime();
        const remainingMs = deadline.getTime() - now.getTime();

        const totalDays = Math.max(1, Math.ceil(totalMs / (1000 * 60 * 60 * 24)));
        const daysRemaining = Math.max(0, Math.ceil(remainingMs / (1000 * 60 * 60 * 24)));
        const progress = Math.max(0, Math.min(1, elapsedMs / totalMs));

        const fmt = (d: Date) => d.toLocaleDateString('cs-CZ', { day: 'numeric', month: 'short', year: 'numeric' });

        return {
            daysRemaining,
            totalDays,
            progress,
            label: props.label,
            periodCode: props.periodCode,
            startStr: fmt(start),
            endStr: fmt(end),
            deadlineStr: fmt(deadline),
            status: props.status,
        };
    }, [props]);

    const getProgressBarColor = () => {
        if (computed.daysRemaining <= 2) return 'error';
        if (computed.daysRemaining <= 5) return 'warning';
        return 'brand';
    };

    const getStatusColor = (status?: string): 'informative' | 'success' | 'warning' | 'danger' => {
        switch (status?.toUpperCase()) {
            case 'OPEN': return 'informative';
            case 'COLLECTING': return 'success';
            case 'REVIEWING': return 'warning';
            case 'CLOSED': return 'danger';
            default: return 'informative';
        }
    };

    return (
        <Card className={styles.card}>
            <div className={styles.header}>
                <div>
                    <Title3>{computed.label}</Title3>
                    {computed.periodCode && (
                        <Body2 className={styles.periodCode}>{computed.periodCode}</Body2>
                    )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {computed.status && (
                        <Badge appearance="filled" color={getStatusColor(computed.status)}>
                            {computed.status}
                        </Badge>
                    )}
                    <TimerRegular />
                </div>
            </div>

            <div className={styles.value}>
                {computed.daysRemaining} Days
            </div>

            <Body2 className={styles.subtitle}>
                remaining until submission deadline
                {computed.deadlineStr && ` (${computed.deadlineStr})`}
            </Body2>

            {computed.startStr && computed.endStr && (
                <div className={styles.dateRange}>
                    <Body1>{computed.startStr}</Body1>
                    <Body1>{computed.endStr}</Body1>
                </div>
            )}

            <ProgressBar
                value={computed.progress}
                color={getProgressBarColor() as any}
                className={styles.progress}
            />

            <div className={styles.caption}>
                {Math.round(computed.progress * 100)}% of period elapsed
            </div>
        </Card>
    );
};

export default CountdownWidget;
