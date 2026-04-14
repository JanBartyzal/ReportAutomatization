import { useParams, useNavigate } from 'react-router-dom';
import { useState, useEffect, useRef } from 'react';
import {
    Title2,
    Title3,
    Body1,
    Card,
    CardHeader,
    makeStyles,
    tokens,
    Button,
} from '@fluentui/react-components';
import { ArrowLeft24Regular, Edit24Regular, FullScreenMaximize24Regular, ArrowSync24Regular } from '@fluentui/react-icons';
import { useDashboard, useDashboardQuery } from '../hooks/useDashboards';
import LoadingSpinner from '../components/LoadingSpinner';
import { UnifiedTableView } from '../components/UnifiedTableView/UnifiedTableView';
import { PeriodComparison } from '../components/PeriodComparison/PeriodComparison';
import {
    BarChartWidget,
    StackedBarChartWidget,
    LineChartWidget,
    PieChartWidget,
    HeatmapWidget
} from '../components/Charts';
import { executeRawSql } from '../api/dashboards';
import { FileContentType, type WidgetConfig, type AggregatedData } from '@reportplatform/types';

/**
 * DashboardViewerPage styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: tokens.spacingHorizontalL,
    },
    headerActions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
    },
    description: {
        marginBottom: tokens.spacingHorizontalL,
        color: tokens.colorNeutralForeground2,
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(12, 1fr)',
        gap: tokens.spacingHorizontalM,
    },
    widget: {
        minHeight: '200px',
    },
    widgetContent: {
        padding: tokens.spacingHorizontalM,
        height: '100%',
    },
    loadingOverlay: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '200px',
    },
    noData: {
        textAlign: 'center',
        padding: tokens.spacingHorizontalL,
        color: tokens.colorNeutralForeground2,
    },
});

export default function DashboardViewerPage() {
    const styles = useStyles();
    const { dashboardId } = useParams<{ dashboardId: string }>();
    const navigate = useNavigate();
    const { data: dashboard, isLoading } = useDashboard(dashboardId || '');
    const queryDashboard = useDashboardQuery(dashboardId || '');
    const [widgetData, setWidgetData] = useState<Record<number, AggregatedData>>({});
    const [showPeriodComparison, setShowPeriodComparison] = useState(false);
    const loadedRef = useRef(false);

    const handleRefresh = async () => {
        if (!dashboardId || !dashboard?.widgets?.length) return;

        const newData: Record<number, AggregatedData> = {};

        for (let i = 0; i < dashboard.widgets.length; i++) {
            const widget = dashboard.widgets[i];
            try {
                if (widget.data_source === 'custom_sql') {
                    const sql = widget.config.sql as string;
                    if (!sql) {
                        console.warn(`Widget ${i} has no SQL query defined`);
                        continue;
                    }
                    const rawResult = await executeRawSql(sql);
                    // Transform rows from arrays to keyed objects using column names
                    const mappedRows = rawResult.rows.map((row: unknown[]) => {
                        const obj: Record<string, unknown> = {};
                        rawResult.columns.forEach((col, idx) => {
                            obj[col] = row[idx];
                        });
                        return obj;
                    });
                    newData[i] = {
                        columns: rawResult.columns,
                        rows: mappedRows,
                        total_rows: rawResult.totalRows,
                    };
                } else {
                    const result = await queryDashboard.mutateAsync({
                        group_by: widget.config.group_by as string[] || [],
                        order_by: widget.config.order_by as string,
                        filters: widget.config.filters as Record<string, unknown>,
                    });
                    newData[i] = result;
                }
            } catch (error) {
                console.error(`Failed to fetch data for widget ${i}:`, error);
            }
        }

        setWidgetData(newData);
    };

    // Auto-load widget data once when dashboard loads
    useEffect(() => {
        if (dashboard?.widgets?.length && !loadedRef.current) {
            loadedRef.current = true;
            handleRefresh();
        }
    }, [dashboard]);

    if (isLoading) {
        return <LoadingSpinner label="Loading dashboard..." />;
    }

    if (!dashboard) {
        return (
            <div className={styles.container}>
                <Body1>Dashboard not found</Body1>
            </div>
        );
    }

    const renderWidgetContent = (widget: WidgetConfig, index: number) => {
        const data = widgetData[index];

        if (!data) {
            return (
                <div className={styles.loadingOverlay}>
                    <Body1>Click refresh to load data</Body1>
                </div>
            );
        }

        if (data.rows.length === 0) {
            return (
                <div className={styles.noData}>
                    <Body1>No data available for this widget.</Body1>
                </div>
            );
        }

        switch (widget.type) {
            case 'TABLE':
                return (
                    <UnifiedTableView
                        tables={[
                            {
                                table_id: `widget-${index}`,
                                source_type: FileContentType.CSV,
                                headers: data.columns,
                                rows: data.rows,
                                row_count: data.total_rows,
                            },
                        ]}
                        title=""
                    />
                );
            case 'BAR_CHART':
                return <BarChartWidget data={data} />;
            case 'STACKED_BAR_CHART':
                return <StackedBarChartWidget data={data} />;
            case 'LINE_CHART':
                return <LineChartWidget data={data} />;
            case 'PIE_CHART':
                return <PieChartWidget data={data} />;
            case 'HEATMAP':
                return <HeatmapWidget data={data} />;
            default:
                return <Body1>Unknown widget type</Body1>;
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div style={{ display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalM }}>
                    <Button
                        appearance="subtle"
                        icon={<ArrowLeft24Regular />}
                        onClick={() => navigate('/dashboards')}
                    >
                        Back
                    </Button>
                    <Title2>{dashboard.name}</Title2>
                </div>
                <div className={styles.headerActions}>
                    <Button
                        appearance="subtle"
                        icon={<ArrowSync24Regular />}
                        onClick={handleRefresh}
                        disabled={queryDashboard.isPending}
                    >
                        Refresh
                    </Button>
                    <Button
                        appearance="subtle"
                        icon={<FullScreenMaximize24Regular />}
                        onClick={() => setShowPeriodComparison(true)}
                    >
                        Compare Periods
                    </Button>
                    <Button
                        appearance="subtle"
                        icon={<Edit24Regular />}
                        onClick={() => navigate(`/dashboards/${dashboardId}/edit`)}
                    >
                        Edit
                    </Button>
                </div>
            </div>

            {dashboard.description && (
                <Body1 className={styles.description}>{dashboard.description}</Body1>
            )}

            <div className={styles.grid}>
                {dashboard.widgets.map((widget: WidgetConfig, index: number) => {
                    const colSpan = Math.min(Math.max((widget.config?.width as number) || 6, 1), 12);
                    const height = (widget.config?.height as number) || 300;
                    return (
                        <Card
                            key={index}
                            className={styles.widget}
                            style={{
                                gridColumn: `span ${colSpan}`,
                                minHeight: `${height}px`,
                            }}
                        >
                            <CardHeader header={<Title3>{widget.title}</Title3>} />
                            <div className={styles.widgetContent}>
                                {renderWidgetContent(widget, index)}
                            </div>
                        </Card>
                    );
                })}
            </div>

            {dashboard.widgets.length === 0 && (
                <div className={styles.noData}>
                    <Title3 block>No widgets configured</Title3>
                    <Body1 block>Edit this dashboard to add widgets.</Body1>
                    <Button
                        appearance="primary"
                        onClick={() => navigate(`/dashboards/${dashboardId}/edit`)}
                    >
                        Add Widgets
                    </Button>
                </div>
            )}

            {showPeriodComparison && (
                <PeriodComparison onClose={() => setShowPeriodComparison(false)} />
            )}
        </div>
    );
}
