/**
 * Health Dashboard Page - Admin Only
 *
 * Displays system health status including:
 * - Service status grid (green/red/yellow)
 * - Recent error log feed
 * - DLQ depth display
 * - Active workflow count
 * - Link to Grafana dashboards
 *
 * Part of P5-W4-002: Health Dashboard Page (Admin)
 */
import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
    Title3,
    Body1,
    Card,
    CardHeader,
    Badge,
    BadgeProps,
    Divider,
    makeStyles,
    tokens,
    Spinner,
    Button,
    Table,
    TableBody,
    TableCell,
    TableCellLayout,
    TableHeader,
    TableHeaderCell,
    TableRow,
} from '@fluentui/react-components';
import {
    CheckmarkCircle24Regular,
    DismissCircle24Regular,
    Warning24Regular,
    ArrowExportUp24Regular,
    DataTrending24Regular,
    DataPie24Regular,
    Settings24Regular,
    ChevronLeft24Regular,
} from '@fluentui/react-icons';
import { getHealthDashboard } from '../api/health';
import HealthServicesSettingsPanel from '../components/Admin/HealthServicesSettingsPanel';

const useStyles = makeStyles({
    page: {
        padding: tokens.spacingVerticalL,
    },
    header: {
        marginBottom: tokens.spacingVerticalL,
    },
    title: {
        marginBottom: tokens.spacingVerticalS,
    },
    subtitle: {
        color: tokens.colorNeutralForeground2,
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
        gap: tokens.spacingVerticalM,
        marginBottom: tokens.spacingVerticalL,
    },
    metricsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
        gap: tokens.spacingVerticalM,
        marginBottom: tokens.spacingVerticalL,
    },
    metricCard: {
        padding: tokens.spacingVerticalL,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    metricValue: {
        fontSize: tokens.fontSizeHero700,
        fontWeight: '600',
        color: tokens.colorBrandForeground1,
        marginBottom: tokens.spacingVerticalXS,
    },
    metricLabel: {
        color: tokens.colorNeutralForeground2,
        fontSize: tokens.fontSizeBase200,
    },
    section: {
        marginBottom: tokens.spacingVerticalL,
    },
    sectionTitle: {
        marginBottom: tokens.spacingVerticalM,
    },
    serviceName: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    statusIcon: {
        display: 'flex',
        alignItems: 'center',
    },
    errorLevel: {
        fontWeight: '600',
    },
    errorCritical: {
        color: tokens.colorPaletteRedForeground1,
    },
    errorError: {
        color: tokens.colorPaletteDarkOrangeForeground1,
    },
    errorWarning: {
        color: tokens.colorPaletteYellowForeground1,
    },
    grafanaLink: {
        marginTop: tokens.spacingVerticalM,
    },
    refreshButton: {
        marginLeft: 'auto',
    },
    headerRow: {
        display: 'flex',
        alignItems: 'center',
        marginBottom: tokens.spacingVerticalM,
    },
});

/**
 * Get badge appearance based on service status.
 */
function getStatusBadgeProps(status: string): BadgeProps {
    switch (status) {
        case 'healthy':
            return { appearance: 'filled', color: 'success' };
        case 'degraded':
            return { appearance: 'filled', 'aria-label': 'Degraded', color: 'warning' };
        default:
            return { appearance: 'filled', color: 'informative' };
    }
}

/**
 * Get status icon based on service status.
 */
function getStatusIcon(status: string): React.ReactNode {
    switch (status) {
        case 'healthy':
            return <CheckmarkCircle24Regular style={{ color: tokens.colorPaletteGreenForeground1 }} />;
        case 'degraded':
            return <Warning24Regular style={{ color: tokens.colorPaletteYellowForeground1 }} />;
        case 'down':
            return <DismissCircle24Regular style={{ color: tokens.colorPaletteRedForeground1 }} />;
        default:
            return <Warning24Regular />;
    }
}

/**
 * Format timestamp to relative time.
 */
function formatRelativeTime(timestamp: string): string {
    const now = new Date();
    const date = new Date(timestamp);
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return date.toLocaleDateString();
}

/**
 * Health Dashboard Page Component.
 */
const HealthDashboardPage: React.FC = () => {
    const styles = useStyles();
    const [showSettings, setShowSettings] = useState(false);

    const { data, isLoading, error, refetch, isRefetching } = useQuery({
        queryKey: ['health-dashboard'],
        queryFn: getHealthDashboard,
        refetchInterval: 30000, // Refresh every 30 seconds
        staleTime: 0, // Always refetch on invalidation (e.g. after service name change)
    });

    if (showSettings) {
        return (
            <div className={styles.page}>
                <div className={styles.headerRow}>
                    <div className={styles.header}>
                        <Title3 className={styles.title}>Health Service Settings</Title3>
                        <Body1 className={styles.subtitle}>
                            Manage monitored services and their health check URLs
                        </Body1>
                    </div>
                    <Button
                        appearance="subtle"
                        icon={<ChevronLeft24Regular />}
                        onClick={() => {
                            setShowSettings(false);
                            // Force refetch after settings changes so updated names/configs appear immediately
                            setTimeout(() => refetch(), 500);
                        }}
                    >
                        Back to Dashboard
                    </Button>
                </div>
                <HealthServicesSettingsPanel />
            </div>
        );
    }

    if (isLoading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
                <Spinner label="Loading health data..." />
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className={styles.page}>
                <div className={styles.header}>
                    <Title3 className={styles.title}>Health Dashboard</Title3>
                    <Body1 className={styles.subtitle}>Error loading health data</Body1>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            <div className={styles.headerRow}>
                <div className={styles.header}>
                    <Title3 className={styles.title}>Health Dashboard</Title3>
                    <Body1 className={styles.subtitle}>
                        System monitoring and service status • Last updated: {formatRelativeTime(data.lastUpdated)}
                    </Body1>
                </div>
                <Button
                    appearance="subtle"
                    icon={<Settings24Regular />}
                    onClick={() => setShowSettings(true)}
                    style={{ marginRight: tokens.spacingHorizontalS }}
                >
                    Settings
                </Button>
                <Button
                    appearance="subtle"
                    icon={isRefetching ? <Spinner size="tiny" /> : <ArrowExportUp24Regular />}
                    onClick={() => refetch()}
                    disabled={isRefetching}
                >
                    Refresh
                </Button>
            </div>

            {/* Metrics Cards */}
            <div className={styles.metricsGrid}>
                <Card className={styles.metricCard}>
                    <DataTrending24Regular style={{ fontSize: '32px', color: tokens.colorBrandForeground1 }} />
                    <div className={styles.metricValue}>{data.metrics.activeWorkflows}</div>
                    <div className={styles.metricLabel}>Active Workflows</div>
                </Card>

                <Card className={styles.metricCard}>
                    <DataPie24Regular style={{ fontSize: '32px', color: tokens.colorBrandForeground1 }} />
                    <div className={styles.metricValue} style={{ color: data.metrics.dlqDepth > 0 ? tokens.colorPaletteRedForeground1 : tokens.colorBrandForeground1 }}>
                        {data.metrics.dlqDepth}
                    </div>
                    <div className={styles.metricLabel}>DLQ Depth</div>
                </Card>

                <Card className={styles.metricCard}>
                    <div className={styles.metricValue}>{data.metrics.totalProcessed.toLocaleString()}</div>
                    <div className={styles.metricLabel}>Total Processed</div>
                </Card>

                <Card className={styles.metricCard}>
                    <div className={styles.metricValue} style={{ color: data.metrics.failedJobs > 0 ? tokens.colorPaletteRedForeground1 : tokens.colorBrandForeground1 }}>
                        {data.metrics.failedJobs}
                    </div>
                    <div className={styles.metricLabel}>Failed Jobs</div>
                </Card>
            </div>

            {/* Service Status Grid */}
            <div className={styles.section}>
                <Title3 className={styles.sectionTitle}>Service Status</Title3>
                <div className={styles.grid}>
                    {data.services.map((service) => (
                        <Card key={service.id}>
                            <CardHeader
                                header={
                                    <div className={styles.serviceName}>
                                        <span className={styles.statusIcon}>
                                            {getStatusIcon(service.status)}
                                        </span>
                                        <span>{service.name}</span>
                                    </div>
                                }
                                action={
                                    <Badge {...getStatusBadgeProps(service.status)}>
                                        {service.status}
                                    </Badge>
                                }
                            />
                            <Divider />
                            <div style={{ padding: tokens.spacingVerticalM }}>
                                <Body1>Response time: {service.responseTime}ms</Body1>
                                <Body1>Uptime: {service.uptime}%</Body1>
                                <Body1>Version: {service.version}</Body1>
                            </div>
                        </Card>
                    ))}
                </div>
            </div>

            {/* Recent Error Logs */}
            <div className={styles.section}>
                <Title3 className={styles.sectionTitle}>Recent Errors</Title3>
                <Card>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHeaderCell>Time</TableHeaderCell>
                                <TableHeaderCell>Service</TableHeaderCell>
                                <TableHeaderCell>Level</TableHeaderCell>
                                <TableHeaderCell>Message</TableHeaderCell>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {data.recentErrors.map((error) => (
                                <TableRow key={error.id}>
                                    <TableCell>
                                        <TableCellLayout>
                                            {formatRelativeTime(error.timestamp)}
                                        </TableCellLayout>
                                    </TableCell>
                                    <TableCell>
                                        <TableCellLayout>{error.service}</TableCellLayout>
                                    </TableCell>
                                    <TableCell>
                                        <TableCellLayout>
                                            <span className={`${styles.errorLevel} ${error.level === 'critical' ? styles.errorCritical :
                                                    error.level === 'error' ? styles.errorError :
                                                        styles.errorWarning
                                                }`}>
                                                {error.level.toUpperCase()}
                                            </span>
                                        </TableCellLayout>
                                    </TableCell>
                                    <TableCell>
                                        <TableCellLayout>
                                            {error.message}
                                            {error.details && (
                                                <Body1 style={{ color: tokens.colorNeutralForeground2, fontSize: tokens.fontSizeBase200 }}>
                                                    {error.details}
                                                </Body1>
                                            )}
                                        </TableCellLayout>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Card>
            </div>

            {/* Grafana Link */}
            <div className={styles.grafanaLink}>
                <Button
                    appearance="primary"
                    icon={<ArrowExportUp24Regular />}
                    onClick={() => window.open(data.grafanaUrl, '_blank')}
                >
                    Open Grafana Dashboard
                </Button>
            </div>
        </div>
    );
};

export default HealthDashboardPage;
