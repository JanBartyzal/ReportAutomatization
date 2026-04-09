import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
    Title2,
    Title3,
    Body1,
    Body2,
    Card,
    CardHeader,
    Button,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    DocumentBulletList24Regular,
    ArrowUpload24Regular,
    CalendarLtr24Regular,
    Folder24Regular,
    DocumentCheckmark24Regular,
    DataPie24Regular,
} from '@fluentui/react-icons';
import { CountdownWidget } from '../components/Lifecycle/CountdownWidget';
import { listPeriods } from '../api/periods';
import { listFiles } from '../api/files';
import { listDashboards } from '../api/dashboards';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    title: {
        marginBottom: tokens.spacingHorizontalL,
    },
    section: {
        marginBottom: tokens.spacingHorizontalXL,
    },
    sectionHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalM,
    },
    periodsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    statsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    statCard: {
        padding: tokens.spacingHorizontalL,
        borderRadius: tokens.borderRadiusMedium,
        cursor: 'pointer',
        transition: 'box-shadow 0.2s ease-in-out',
        '&:hover': {
            boxShadow: tokens.shadow4,
        },
    },
    statValue: {
        fontSize: '28px',
        fontWeight: tokens.fontWeightBold,
        lineHeight: 1,
        marginBottom: tokens.spacingHorizontalXS,
    },
    statLabel: {
        color: tokens.colorNeutralForeground3,
    },
    statIcon: {
        color: tokens.colorBrandForeground1,
    },
    quickActions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        flexWrap: 'wrap',
    },
    emptyState: {
        textAlign: 'center' as const,
        padding: '40px 20px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
    },
    dashboardGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    dashboardCard: {
        cursor: 'pointer',
        padding: tokens.spacingHorizontalM,
        transition: 'box-shadow 0.2s ease-in-out',
        '&:hover': {
            boxShadow: tokens.shadow4,
        },
    },
});

export default function DashboardPage() {
    const styles = useStyles();
    const navigate = useNavigate();

    // Fetch open/collecting periods
    const { data: periodsData, isLoading: periodsLoading } = useQuery({
        queryKey: ['dashboard-periods'],
        queryFn: () => listPeriods({ page_size: 10 }),
        refetchInterval: 60_000,
    });

    // Fetch file stats (just 1 item to get total count from pagination)
    const { data: filesData, isLoading: filesLoading } = useQuery({
        queryKey: ['dashboard-files-count'],
        queryFn: () => listFiles({ page_size: 1 }),
        refetchInterval: 60_000,
    });

    // Fetch public dashboards
    const { data: dashboardsData, isLoading: dashboardsLoading } = useQuery({
        queryKey: ['dashboard-public-list'],
        queryFn: () => listDashboards(),
        refetchInterval: 60_000,
    });

    const publicDashboards = (dashboardsData ?? []).filter((d) => d.is_public);

    const periods = periodsData?.data ?? [];
    const openPeriods = periods.filter(
        (p) => ['OPEN', 'COLLECTING', 'REVIEWING'].includes((p.status ?? (p as any).state ?? '').toUpperCase())
    );
    const totalFiles = filesData?.pagination?.total_items ?? 0;

    return (
        <div className={styles.container}>
            <Title2 className={styles.title}>Dashboard</Title2>

            {/* Stats overview */}
            <div className={`${styles.section}`}>
                <div className={styles.statsGrid}>
                    <Card className={styles.statCard} onClick={() => navigate('/files')}>
                        <CardHeader
                            image={<Folder24Regular className={styles.statIcon} />}
                            header={<Body2 className={styles.statLabel}>Total Files</Body2>}
                        />
                        <div className={styles.statValue}>
                            {filesLoading ? <Spinner size="tiny" /> : totalFiles}
                        </div>
                    </Card>

                    <Card className={styles.statCard} onClick={() => navigate('/periods')}>
                        <CardHeader
                            image={<CalendarLtr24Regular className={styles.statIcon} />}
                            header={<Body2 className={styles.statLabel}>Open Periods</Body2>}
                        />
                        <div className={styles.statValue}>
                            {periodsLoading ? <Spinner size="tiny" /> : openPeriods.length}
                        </div>
                    </Card>

                    <Card className={styles.statCard} onClick={() => navigate('/reports')}>
                        <CardHeader
                            image={<DocumentCheckmark24Regular className={styles.statIcon} />}
                            header={<Body2 className={styles.statLabel}>Total Periods</Body2>}
                        />
                        <div className={styles.statValue}>
                            {periodsLoading ? <Spinner size="tiny" /> : periods.length}
                        </div>
                    </Card>
                </div>
            </div>

            {/* Active Reporting Periods */}
            <div className={styles.section}>
                <div className={styles.sectionHeader}>
                    <Title3>Active Reporting Periods</Title3>
                    <Button
                        appearance="subtle"
                        icon={<CalendarLtr24Regular />}
                        onClick={() => navigate('/periods')}
                    >
                        View All
                    </Button>
                </div>

                {periodsLoading ? (
                    <Spinner label="Loading periods..." />
                ) : openPeriods.length === 0 ? (
                    <div className={styles.emptyState}>
                        <CalendarLtr24Regular style={{ fontSize: '48px', color: tokens.colorNeutralForeground4 }} />
                        <Title3 style={{ marginTop: '12px' }}>No active periods</Title3>
                        <Body1 style={{ color: tokens.colorNeutralForeground3 }}>
                            Create a reporting period to start collecting data.
                        </Body1>
                        <Button
                            appearance="primary"
                            style={{ marginTop: '16px' }}
                            onClick={() => navigate('/periods')}
                        >
                            Manage Periods
                        </Button>
                    </div>
                ) : (
                    <div className={styles.periodsGrid}>
                        {openPeriods.map((period) => (
                            <CountdownWidget
                                key={period.id}
                                label={period.name}
                                periodCode={(period as any).periodCode ?? (period as any).period_code}
                                startDate={(period as any).startDate ?? (period as any).start_date}
                                endDate={(period as any).endDate ?? (period as any).end_date}
                                deadline={(period as any).submissionDeadline ?? (period as any).submission_deadline ?? (period as any).endDate ?? (period as any).end_date}
                                status={(period.status ?? (period as any).state ?? '').toUpperCase()}
                            />
                        ))}
                    </div>
                )}
            </div>

            {/* Public Dashboards */}
            <div className={styles.section}>
                <div className={styles.sectionHeader}>
                    <Title3>Public Dashboards</Title3>
                    <Button
                        appearance="subtle"
                        icon={<DataPie24Regular />}
                        onClick={() => navigate('/dashboards')}
                    >
                        View All
                    </Button>
                </div>

                {dashboardsLoading ? (
                    <Spinner label="Loading dashboards..." />
                ) : publicDashboards.length === 0 ? (
                    <div className={styles.emptyState}>
                        <DataPie24Regular style={{ fontSize: '48px', color: tokens.colorNeutralForeground4 }} />
                        <Title3 style={{ marginTop: '12px' }}>No public dashboards</Title3>
                        <Body1 style={{ color: tokens.colorNeutralForeground3 }}>
                            Create a dashboard and set it to Public to show it here for all users.
                        </Body1>
                        <Button
                            appearance="primary"
                            style={{ marginTop: '16px' }}
                            onClick={() => navigate('/dashboards/new')}
                        >
                            Create Dashboard
                        </Button>
                    </div>
                ) : (
                    <div className={styles.dashboardGrid}>
                        {publicDashboards.map((dashboard) => (
                            <Card
                                key={dashboard.id}
                                className={styles.dashboardCard}
                                onClick={() => navigate(`/dashboards/${dashboard.id}`)}
                            >
                                <CardHeader
                                    image={<DataPie24Regular className={styles.statIcon} />}
                                    header={<Body2><strong>{dashboard.name}</strong></Body2>}
                                />
                                <Body1 style={{ color: tokens.colorNeutralForeground3, marginTop: tokens.spacingVerticalXS }}>
                                    {dashboard.created_at ? new Date(dashboard.created_at).toLocaleDateString() : ''}
                                </Body1>
                            </Card>
                        ))}
                    </div>
                )}
            </div>

            {/* Quick Actions */}
            <div className={styles.section}>
                <Title3 style={{ marginBottom: tokens.spacingHorizontalM }}>Quick Actions</Title3>
                <div className={styles.quickActions}>
                    <Button
                        appearance="primary"
                        icon={<ArrowUpload24Regular />}
                        onClick={() => navigate('/upload')}
                    >
                        Upload Files
                    </Button>
                    <Button
                        appearance="secondary"
                        icon={<DocumentBulletList24Regular />}
                        onClick={() => navigate('/reports')}
                    >
                        View Reports
                    </Button>
                    <Button
                        appearance="secondary"
                        icon={<CalendarLtr24Regular />}
                        onClick={() => navigate('/periods')}
                    >
                        Manage Periods
                    </Button>
                </div>
            </div>
        </div>
    );
}
