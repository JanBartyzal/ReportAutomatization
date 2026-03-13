import { Title2, Body1, Card, CardHeader, makeStyles, tokens } from '@fluentui/react-components';
import { CountdownWidget } from '../components/Lifecycle/CountdownWidget';

/**
 * DashboardPage styles per docs/UX-UI/02-design-system.md
 * - Using Fluent tokens throughout
 * - KPI cards per section 10.2
 * - Widget cards per section 10.3
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    title: {
        marginBottom: tokens.spacingHorizontalL,
    },
    /**
     * Stats grid - responsive per breakpoints in section 4.3
     */
    statsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalXL,
    },
    /**
     * KPI Card per section 10.2
     * - Padding: spacingL (24px)
     * - Shadow: Level 1
     * - Hover: Level 2
     */
    statCard: {
        padding: tokens.spacingHorizontalL,
        borderRadius: tokens.borderRadiusMedium,
    },
});

export default function DashboardPage() {
    const styles = useStyles();

    return (
        <div className={styles.container}>
            <Title2 className={styles.title}>Dashboard</Title2>

            <div className={styles.statsGrid}>
                <CountdownWidget 
                    daysRemaining={4} 
                    totalDays={30} 
                    label="March Reporting" 
                />
                <Card className={styles.statCard}>
                    <CardHeader header={<Body1><strong>Total Files</strong></Body1>} />
                    <Body1>0</Body1>
                </Card>
                <Card className={styles.statCard}>
                    <CardHeader header={<Body1><strong>Processing</strong></Body1>} />
                    <Body1>0</Body1>
                </Card>
            </div>

            <Body1>Welcome to ReportAutomatization. Upload files to get started.</Body1>
        </div>
    );
}
