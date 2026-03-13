import React from 'react';
import {
    makeStyles,
    tokens,
    Title3,
    Body2,
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
        marginBottom: tokens.spacingHorizontalM,
    },
    value: {
        fontSize: '32px',
        fontWeight: tokens.fontWeightBold,
        lineHeight: 1,
        marginBottom: tokens.spacingHorizontalXS,
    },
    subtitle: {
        color: tokens.colorNeutralForeground4,
        marginBottom: tokens.spacingHorizontalL,
    },
    progress: {
        marginTop: tokens.spacingHorizontalS,
    }
});

interface CountdownWidgetProps {
    daysRemaining: number;
    totalDays: number;
    label: string;
}

export const CountdownWidget: React.FC<CountdownWidgetProps> = ({
    daysRemaining,
    totalDays,
    label,
}) => {
    const styles = useStyles();
    const progress = Math.max(0, Math.min(1, daysRemaining / totalDays));
    
    // Determine color based on urgency
    const getProgressBarColor = () => {
        if (daysRemaining <= 2) return 'danger';
        if (daysRemaining <= 5) return 'warning';
        return 'brand';
    };

    return (
        <Card className={styles.card}>
            <div className={styles.header}>
                <Title3>{label}</Title3>
                <TimerRegular />
            </div>
            
            <div className={styles.value}>
                {daysRemaining} Days
            </div>
            
            <Body2 className={styles.subtitle}>
                remaining until submission deadline
            </Body2>

            <ProgressBar 
                value={progress} 
                color={getProgressBarColor() as any}
                className={styles.progress}
            />
            
            <Caption style={{ marginTop: tokens.spacingHorizontalXS }}>
                {Math.round(progress * 100)}% of period elapsed
            </Caption>
        </Card>
    );
};

const Caption = (props: any) => <Body2 style={{ color: tokens.colorNeutralForeground4, fontSize: '10px' }} {...props} />;

export default CountdownWidget;
