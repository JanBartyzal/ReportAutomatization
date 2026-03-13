import { useNavigate } from 'react-router-dom';
import {
    Title3,
    Card,
    CardHeader,
    makeStyles,
    tokens,
    Button,
    Body1,
    Badge,
} from '@fluentui/react-components';
import { Add24Regular, Globe24Regular, LockClosed24Regular } from '@fluentui/react-icons';
import { useDashboards } from '../hooks/useDashboards';
import LoadingSpinner from '../components/LoadingSpinner';
import { PageHeader } from '../components/shared/PageHeader';

/**
 * DashboardListPage styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalL,
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    card: {
        cursor: 'pointer',
        transition: 'box-shadow 0.2s ease',
        ':hover': {
            boxShadow: tokens.shadow8,
        },
    },
    emptyState: {
        textAlign: 'center',
        padding: tokens.spacingHorizontalXL,
        color: tokens.colorNeutralForeground2,
    },
    emptyStateButton: {
        marginTop: tokens.spacingVerticalM,
    },
    cardPreview: {
        height: '120px',
        backgroundColor: tokens.colorNeutralBackground3,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    cardContent: {
        padding: tokens.spacingHorizontalM,
    },
    cardActions: {
        display: 'flex',
        gap: tokens.spacingVerticalS,
        marginTop: tokens.spacingVerticalM,
    },
    visibilityBadge: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: tokens.spacingVerticalXS,
    },
    badgeIcon: {
        width: '14px',
        height: '14px',
    },
    badgeLabel: {
        marginLeft: tokens.spacingHorizontalXS,
    },
    cardPreviewText: {
        color: tokens.colorNeutralForeground3,
    },
    cardDate: {
        color: tokens.colorNeutralForeground2,
    },
});

export default function DashboardListPage() {
    const styles = useStyles();
    const navigate = useNavigate();
    const { data: dashboards, isLoading } = useDashboards();

    if (isLoading) {
        return <LoadingSpinner label="Loading dashboards..." />;
    }

    const dashboardList = dashboards || [];

    return (
        <div className={styles.container}>
            <PageHeader
                title="Dashboards"
                actions={
                    <Button
                        appearance="primary"
                        icon={<Add24Regular />}
                        onClick={() => navigate('/dashboards/new')}
                    >
                        New Dashboard
                    </Button>
                }
            />

            {dashboardList.length === 0 ? (
                <div className={styles.emptyState}>
                    <Title3>No dashboards yet</Title3>
                    <Body1>Create your first dashboard to start visualizing data.</Body1>
                    <Button
                        appearance="primary"
                        icon={<Add24Regular />}
                        onClick={() => navigate('/dashboards/new')}
                        className={styles.emptyStateButton}
                    >
                        Create Dashboard
                    </Button>
                </div>
            ) : (
                <div className={styles.grid}>
                    {dashboardList.map((dashboard) => (
                        <Card
                            key={dashboard.id}
                            className={styles.card}
                            onClick={() => navigate(`/dashboards/${dashboard.id}`)}
                        >
                            <div className={styles.cardPreview}>
                                <Body1 className={styles.cardPreviewText}>
                                    Dashboard Preview
                                </Body1>
                            </div>
                            <div className={styles.cardContent}>
                                <CardHeader
                                    header={<Title3>{dashboard.name}</Title3>}
                                />
                                <div className={styles.visibilityBadge}>
                                    {dashboard.is_public ? (
                                        <Badge appearance="filled" color="success">
                                            <Globe24Regular className={styles.badgeIcon} />
                                            <span className={styles.badgeLabel}>Public</span>
                                        </Badge>
                                    ) : (
                                        <Badge appearance="filled" color="informative">
                                            <LockClosed24Regular className={styles.badgeIcon} />
                                            <span className={styles.badgeLabel}>Private</span>
                                        </Badge>
                                    )}
                                </div>
                                <div className={styles.cardActions}>
                                    <Body1 className={styles.cardDate}>
                                        Created: {new Date(dashboard.created_at).toLocaleDateString()}
                                    </Body1>
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}
