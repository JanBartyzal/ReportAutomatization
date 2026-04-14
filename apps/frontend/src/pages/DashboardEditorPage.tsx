import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title2,
    Title3,
    Body1,
    makeStyles,
    tokens,
    Button,
    Input,
    Switch,
    Card,
    Dropdown,
    Option,
    SpinButton,
    Tab,
    TabList,
} from '@fluentui/react-components';
import { ArrowLeft24Regular, Save24Regular, Add24Regular, Delete24Regular, Share24Regular, Copy24Regular, QuestionCircle24Regular, ArrowDownload24Regular, ArrowUpload24Regular } from '@fluentui/react-icons';
import { useDashboard, useCreateDashboard, useUpdateDashboard } from '../hooks/useDashboards';
import LoadingSpinner from '../components/LoadingSpinner';
import { useToast } from '../components/NotificationCenter/ToastContainer';
import DashboardSqlEditor from '../components/Dashboard/DashboardSqlEditor';
import SqlQueryHelperDialog from '../components/Dashboard/SqlQueryHelperDialog';
import { WidgetExportDialog, WidgetImportDialog } from '../components/Dashboard/WidgetImportExportDialog';
import type { DashboardConfig, WidgetConfig } from '@reportplatform/types';
import { WidgetType } from '@reportplatform/types';

/**
 * DashboardEditorPage styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalL,
        maxWidth: '800px',
    },
    section: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    widgetList: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    widgetCard: {
        padding: tokens.spacingHorizontalM,
    },
    widgetHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    widgetFields: {
        display: 'grid',
        gridTemplateColumns: 'repeat(2, 1fr)',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalM,
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    headerActions: {
        marginLeft: 'auto',
        display: 'flex',
        gap: tokens.spacingHorizontalS,
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalL,
    },
    tabList: {
        marginBottom: tokens.spacingHorizontalL,
    },
    sizePresetsRow: {
        gridColumn: '1 / -1',
    },
    sizePresetsButtons: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        flexWrap: 'wrap' as const,
    },
});

const WIDGET_TYPES: { value: WidgetType; label: string }[] = [
    { value: WidgetType.TABLE, label: 'Table' },
    { value: WidgetType.BAR_CHART, label: 'Bar Chart' },
    { value: WidgetType.STACKED_BAR_CHART, label: 'Stacked Bar Chart' },
    { value: WidgetType.LINE_CHART, label: 'Line Chart' },
    { value: WidgetType.PIE_CHART, label: 'Pie Chart' },
    { value: WidgetType.HEATMAP, label: 'Heatmap' },
];

const DATA_SOURCES = [
    { value: 'opex_by_organization', label: 'OPEX by Organization' },
    { value: 'opex_by_category', label: 'OPEX by Category' },
    { value: 'parsed_files', label: 'Parsed Files' },
    { value: 'processing_logs', label: 'Processing Logs' },
    { value: 'custom_sql', label: 'Custom SQL Query' },
];

const GROUP_BY_OPTIONS = [
    { value: 'org_id', label: 'Organization' },
    { value: 'period_id', label: 'Period' },
    { value: 'category', label: 'Category' },
    { value: 'file_type', label: 'File Type' },
    { value: 'status', label: 'Status' },
];

export default function DashboardEditorPage() {
    const styles = useStyles();
    const { dashboardId } = useParams<{ dashboardId: string }>();
    const navigate = useNavigate();
    const isNew = !dashboardId;

    const { data: existingDashboard, isLoading } = useDashboard(dashboardId || '');
    const createDashboard = useCreateDashboard();
    const updateDashboard = useUpdateDashboard();

    const [name, setName] = useState(existingDashboard?.name || '');
    const [description, setDescription] = useState(existingDashboard?.description || '');
    const [isPublic, setIsPublic] = useState(existingDashboard?.is_public || false);
    const [widgets, setWidgets] = useState<WidgetConfig[]>(existingDashboard?.widgets || []);
    const [activeTab, setActiveTab] = useState<string>('basic');
    const [showSqlHelper, setShowSqlHelper] = useState(false);
    const [exportWidget, setExportWidget] = useState<WidgetConfig | null>(null);
    const [showImportDialog, setShowImportDialog] = useState(false);
    const toast = useToast();

    // Sync local state when dashboard data loads (useState initializer only runs once)
    useEffect(() => {
        if (existingDashboard) {
            setName(existingDashboard.name || '');
            setDescription(existingDashboard.description || '');
            setIsPublic(existingDashboard.is_public || false);
            setWidgets(existingDashboard.widgets || []);
        }
    }, [existingDashboard]);

    if (!isNew && isLoading) {
        return <LoadingSpinner label="Loading dashboard..." />;
    }

    const handleSave = async () => {
        const config: DashboardConfig = {
            name,
            description,
            is_public: isPublic,
            widgets,
        };

        try {
            if (isNew) {
                const result = await createDashboard.mutateAsync(config);
                toast('success', 'Dashboard created', `"${name}" was created successfully.`);
                navigate(`/dashboards/${result.id}`);
            } else {
                await updateDashboard.mutateAsync({ dashboardId: dashboardId!, config });
                toast('success', 'Dashboard saved', `"${name}" was saved successfully.`);
                navigate(`/dashboards/${dashboardId}`);
            }
        } catch (error: any) {
            console.error('Failed to save dashboard:', error);
            const detail = error?.response?.data?.detail
                || error?.response?.data?.message
                || error?.response?.data?.errors?.join(', ')
                || error?.message
                || 'Unknown error';
            const status = error?.response?.status;
            toast('error', 'Save failed', `${status ? `[${status}] ` : ''}${detail}`);
        }
    };

    const addWidget = () => {
        setWidgets([
            ...widgets,
            {
                type: 'TABLE' as WidgetType,
                title: 'New Widget',
                data_source: 'opex_by_organization',
                config: {
                    group_by: ['org_id'],
                    order_by: 'value',
                    width: 6,
                    height: 300,
                },
            },
        ]);
    };

    const removeWidget = (index: number) => {
        setWidgets(widgets.filter((_: WidgetConfig, i: number) => i !== index));
    };

    const updateWidget = (index: number, updates: Partial<WidgetConfig>) => {
        setWidgets(
            widgets.map((widget: WidgetConfig, i: number) => (i === index ? { ...widget, ...updates } : widget))
        );
    };

    const handleShare = () => {
        const url = `${window.location.origin}/dashboards/${dashboardId}?shared=true`;
        navigator.clipboard.writeText(url);
        alert('Share link copied to clipboard!');
    };

    const handleClone = async () => {
        if (!dashboardId) return;
        try {
            const { cloneDashboard } = await import('../api/dashboards');
            const cloned = await cloneDashboard(dashboardId, `${name} (Copy)`);
            navigate(`/dashboards/${cloned.id}/edit`);
        } catch (error) {
            console.error('Failed to clone dashboard:', error);
        }
    };

    const handleSqlQueryInsert = (sql: string) => {
        setWidgets([
            ...widgets,
            {
                type: 'TABLE' as WidgetType,
                title: 'SQL Query Result',
                data_source: 'custom_sql',
                config: {
                    sql,
                    group_by: [],
                    order_by: 'value',
                    width: 12,
                    height: 300,
                },
            },
        ]);
        setActiveTab('widgets');
    };

    const handleWidgetImport = (widget: WidgetConfig) => {
        setWidgets([...widgets, { ...widget }]);
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Button
                    appearance="subtle"
                    icon={<ArrowLeft24Regular />}
                    onClick={() => navigate('/dashboards')}
                >
                    Back
                </Button>
                <Title2>{isNew ? 'New Dashboard' : 'Edit Dashboard'}</Title2>
                <div className={styles.headerActions}>
                    {!isNew && (
                        <>
                            <Button
                                appearance="subtle"
                                icon={<Share24Regular />}
                                onClick={handleShare}
                            >
                                Share
                            </Button>
                            <Button
                                appearance="subtle"
                                icon={<Copy24Regular />}
                                onClick={handleClone}
                            >
                                Clone
                            </Button>
                        </>
                    )}
                    <Button
                        appearance="primary"
                        icon={<Save24Regular />}
                        onClick={handleSave}
                        disabled={!name || createDashboard.isPending || updateDashboard.isPending}
                    >
                        {createDashboard.isPending || updateDashboard.isPending
                            ? 'Saving...'
                            : isNew ? 'Create Dashboard' : 'Save Changes'}
                    </Button>
                </div>
            </div>

            <TabList
                selectedValue={activeTab}
                onTabSelect={(_ev: any, data: any) => setActiveTab(data.value as string)}
                className={styles.tabList}
            >
                <Tab value="basic">Basic Settings</Tab>
                <Tab value="widgets">Widgets</Tab>
                <Tab value="sql">SQL Editor (Advanced)</Tab>
            </TabList>

            {activeTab === 'basic' && (
                <div className={styles.form}>
                    <div className={styles.section}>
                        <Title3>Dashboard Settings</Title3>

                        <div className={styles.field}>
                            <Body1><strong>Name</strong></Body1>
                            <Input
                                value={name}
                                onChange={(_ev: any, d: any) => setName(d.value)}
                                placeholder="Enter dashboard name"
                            />
                        </div>

                        <div className={styles.field}>
                            <Body1><strong>Description</strong></Body1>
                            <Input
                                value={description}
                                onChange={(_ev: any, d: any) => setDescription(d.value)}
                                placeholder="Enter dashboard description"
                            />
                        </div>

                        <div className={styles.field}>
                            <Switch
                                checked={isPublic}
                                onChange={(_ev: any, d: any) => setIsPublic(d.checked)}
                                label="Public (visible to all users)"
                            />
                        </div>
                    </div>

                    <div className={styles.actions}>
                        <Button
                            appearance="subtle"
                            onClick={() => navigate('/dashboards')}
                        >
                            Cancel
                        </Button>
                    </div>
                </div>
            )}

            {activeTab === 'widgets' && (
                <div className={styles.form}>
                    <div className={styles.section}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Title3>Widgets</Title3>
                            <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
                                <Button
                                    appearance="primary"
                                    icon={<Add24Regular />}
                                    onClick={addWidget}
                                >
                                    Add Widget
                                </Button>
                                <Button
                                    appearance="subtle"
                                    icon={<ArrowUpload24Regular />}
                                    onClick={() => setShowImportDialog(true)}
                                >
                                    Import Widget
                                </Button>
                            </div>
                        </div>

                        <div className={styles.widgetList}>
                            {widgets.map((widget: WidgetConfig, index: number) => (
                                <Card key={index} className={styles.widgetCard}>
                                    <div className={styles.widgetHeader}>
                                        <Body1><strong>Widget {index + 1}</strong></Body1>
                                        <div style={{ display: 'flex', gap: tokens.spacingHorizontalXS }}>
                                            <Button
                                                appearance="subtle"
                                                icon={<ArrowDownload24Regular />}
                                                onClick={() => setExportWidget(widget)}
                                                size="small"
                                            >
                                                Export
                                            </Button>
                                            <Button
                                                appearance="subtle"
                                                icon={<Delete24Regular />}
                                                onClick={() => removeWidget(index)}
                                                size="small"
                                            >
                                                Remove
                                            </Button>
                                        </div>
                                    </div>

                                    <div className={styles.widgetFields}>
                                        <div className={styles.field}>
                                            <Body1>Title</Body1>
                                            <Input
                                                value={widget.title}
                                                onChange={(_ev: any, d: any) => updateWidget(index, { title: d.value })}
                                            />
                                        </div>

                                        <div className={styles.field}>
                                            <Body1>Widget Type</Body1>
                                            <Dropdown
                                                value={WIDGET_TYPES.find((t: any) => t.value === widget.type)?.label || 'Select Type'}
                                                onOptionSelect={(_ev: any, d: any) =>
                                                    updateWidget(index, { type: d.optionValue as WidgetType })
                                                }
                                            >
                                                {WIDGET_TYPES.map((type) => (
                                                    <Option key={type.value} value={type.value}>
                                                        {type.label}
                                                    </Option>
                                                ))}
                                            </Dropdown>
                                        </div>

                                        <div className={styles.field}>
                                            <Body1>Data Source</Body1>
                                            <Dropdown
                                                value={DATA_SOURCES.find((s: any) => s.value === widget.data_source)?.label || 'Select Data Source'}
                                                onOptionSelect={(_ev: any, d: any) =>
                                                    updateWidget(index, { data_source: d.optionValue as string })
                                                }
                                            >
                                                {DATA_SOURCES.map((source) => (
                                                    <Option key={source.value} value={source.value}>
                                                        {source.label}
                                                    </Option>
                                                ))}
                                            </Dropdown>
                                        </div>

                                        {widget.data_source === 'custom_sql' && (
                                            <div className={styles.field} style={{ gridColumn: '1 / -1' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                                    <Body1>SQL Query</Body1>
                                                    <Button
                                                        appearance="subtle"
                                                        icon={<QuestionCircle24Regular />}
                                                        onClick={() => setShowSqlHelper(true)}
                                                        size="small"
                                                    >
                                                        Help
                                                    </Button>
                                                </div>
                                                <textarea
                                                    style={{
                                                        width: '100%',
                                                        minHeight: '120px',
                                                        padding: tokens.spacingHorizontalM,
                                                        fontFamily: 'monospace',
                                                        fontSize: tokens.fontSizeBase200,
                                                        border: `1px solid ${tokens.colorNeutralStroke1}`,
                                                        borderRadius: tokens.borderRadiusMedium,
                                                        backgroundColor: tokens.colorNeutralBackground2,
                                                        resize: 'vertical',
                                                        outline: 'none',
                                                        color: tokens.colorNeutralForeground1,
                                                    }}
                                                    value={(widget.config as any)?.sql || ''}
                                                    onChange={(e) =>
                                                        updateWidget(index, {
                                                            config: { ...(widget.config as any), sql: e.target.value },
                                                        })
                                                    }
                                                    placeholder="SELECT category AS LabelX, SUM(amount) AS LabelY FROM parsed_tables GROUP BY category"
                                                />
                                            </div>
                                        )}

                                        {widget.data_source !== 'custom_sql' && (
                                            <div className={styles.field}>
                                                <Body1>Group By</Body1>
                                                <Dropdown
                                                    value={GROUP_BY_OPTIONS.find((g: any) => g.value === (widget.config as any)?.group_by?.[0])?.label || 'Select Grouping'}
                                                    onOptionSelect={(_ev: any, d: any) =>
                                                        updateWidget(index, {
                                                            config: { ...(widget.config as any), group_by: [d.optionValue as string] },
                                                        })
                                                    }
                                                >
                                                    {GROUP_BY_OPTIONS.map((option) => (
                                                        <Option key={option.value} value={option.value}>
                                                            {option.label}
                                                        </Option>
                                                    ))}
                                                </Dropdown>
                                            </div>
                                        )}

                                        <div className={`${styles.field} ${styles.sizePresetsRow}`}>
                                            <Body1><strong>Size Presets</strong></Body1>
                                            <div className={styles.sizePresetsButtons}>
                                                <Button size="small" appearance={(widget.config as any)?.width === 4 ? 'primary' : 'secondary'}
                                                    onClick={() => updateWidget(index, { config: { ...(widget.config as any), width: 4, height: 300 } })}>
                                                    1/3 Width
                                                </Button>
                                                <Button size="small" appearance={(widget.config as any)?.width === 6 ? 'primary' : 'secondary'}
                                                    onClick={() => updateWidget(index, { config: { ...(widget.config as any), width: 6, height: 300 } })}>
                                                    Half
                                                </Button>
                                                <Button size="small" appearance={(widget.config as any)?.width === 12 && ((widget.config as any)?.height || 300) <= 400 ? 'primary' : 'secondary'}
                                                    onClick={() => updateWidget(index, { config: { ...(widget.config as any), width: 12, height: 400 } })}>
                                                    Full Width
                                                </Button>
                                                <Button size="small" appearance={(widget.config as any)?.width === 12 && ((widget.config as any)?.height || 300) >= 600 ? 'primary' : 'secondary'}
                                                    onClick={() => updateWidget(index, { config: { ...(widget.config as any), width: 12, height: 700 } })}>
                                                    Full Width Large
                                                </Button>
                                                <Button size="small" appearance={(widget.config as any)?.width === 12 && ((widget.config as any)?.height || 300) >= 900 ? 'primary' : 'secondary'}
                                                    onClick={() => updateWidget(index, { config: { ...(widget.config as any), width: 12, height: 1000 } })}>
                                                    Presentation
                                                </Button>
                                            </div>
                                        </div>

                                        <div className={styles.field}>
                                            <Body1>Width (1-12 columns)</Body1>
                                            <SpinButton
                                                value={(widget.config as any)?.width || 6}
                                                min={1}
                                                max={12}
                                                onChange={(_ev: any, data: any) =>
                                                    updateWidget(index, {
                                                        config: { ...(widget.config as any), width: data.value },
                                                    })
                                                }
                                            />
                                        </div>

                                        <div className={styles.field}>
                                            <Body1>Height (px)</Body1>
                                            <SpinButton
                                                value={(widget.config as any)?.height || 300}
                                                min={100}
                                                max={1200}
                                                step={50}
                                                onChange={(_ev: any, data: any) =>
                                                    updateWidget(index, {
                                                        config: { ...(widget.config as any), height: data.value },
                                                    })
                                                }
                                            />
                                        </div>
                                    </div>
                                </Card>
                            ))}

                            {widgets.length === 0 && (
                                <Body1 style={{ textAlign: 'center', color: tokens.colorNeutralForeground2 }}>
                                    No widgets added yet. Click "Add Widget" to create one.
                                </Body1>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'sql' && (
                <div className={styles.form}>
                    <div className={styles.section}>
                        <Title3 block>SQL Query Editor</Title3>
                        <Body1 block style={{ color: tokens.colorNeutralForeground2, marginBottom: '16px' }}>
                            Write custom SQL queries to create dynamic widgets. Use "Insert as Widget" to add results to your dashboard.
                        </Body1>
                        <DashboardSqlEditor onInsertQuery={handleSqlQueryInsert} />
                    </div>
                </div>
            )}

            <SqlQueryHelperDialog open={showSqlHelper} onClose={() => setShowSqlHelper(false)} />
            <WidgetExportDialog
                open={exportWidget !== null}
                onClose={() => setExportWidget(null)}
                widget={exportWidget}
            />
            <WidgetImportDialog
                open={showImportDialog}
                onClose={() => setShowImportDialog(false)}
                onImport={handleWidgetImport}
            />
        </div>
    );
}
