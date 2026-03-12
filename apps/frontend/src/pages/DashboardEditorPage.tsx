import { useState } from 'react';
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
import { ArrowLeft24Regular, Save24Regular, Add24Regular, Delete24Regular, Share24Regular, Copy24Regular } from '@fluentui/react-icons';
import { useDashboard, useCreateDashboard, useUpdateDashboard } from '../hooks/useDashboards';
import LoadingSpinner from '../components/LoadingSpinner';
import DashboardSqlEditor from '../components/Dashboard/DashboardSqlEditor';
import type { DashboardConfig, WidgetConfig, WidgetType } from '@reportplatform/types';

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
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalL,
    },
    tabList: {
        marginBottom: tokens.spacingHorizontalL,
    },
});

const WIDGET_TYPES: { value: WidgetType; label: string }[] = [
    { value: 'TABLE', label: 'Table' },
    { value: 'BAR_CHART', label: 'Bar Chart' },
    { value: 'LINE_CHART', label: 'Line Chart' },
    { value: 'PIE_CHART', label: 'Pie Chart' },
    { value: 'HEATMAP', label: 'Heatmap' },
];

const DATA_SOURCES = [
    { value: 'opex_by_organization', label: 'OPEX by Organization' },
    { value: 'opex_by_category', label: 'OPEX by Category' },
    { value: 'parsed_files', label: 'Parsed Files' },
    { value: 'processing_logs', label: 'Processing Logs' },
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
    const isNew = dashboardId === 'new';

    const { data: existingDashboard, isLoading } = useDashboard(dashboardId || '');
    const createDashboard = useCreateDashboard();
    const updateDashboard = useUpdateDashboard();

    const [name, setName] = useState(existingDashboard?.name || '');
    const [description, setDescription] = useState(existingDashboard?.description || '');
    const [isPublic, setIsPublic] = useState(existingDashboard?.is_public || false);
    const [widgets, setWidgets] = useState<WidgetConfig[]>(existingDashboard?.widgets || []);
    const [activeTab, setActiveTab] = useState<string>('basic');

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
                navigate(`/dashboards/${result.id}`);
            } else {
                await updateDashboard.mutateAsync({ dashboardId: dashboardId!, config });
                navigate(`/dashboards/${dashboardId}`);
            }
        } catch (error) {
            console.error('Failed to save dashboard:', error);
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
                {!isNew && (
                    <>
                        <Button
                            appearance="subtle"
                            icon={<Share24Regular />}
                            onClick={handleShare}
                            style={{ marginLeft: 'auto' }}
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
                            appearance="primary"
                            icon={<Save24Regular />}
                            onClick={handleSave}
                            disabled={!name || createDashboard.isPending || updateDashboard.isPending}
                        >
                            {isNew ? 'Create Dashboard' : 'Save Changes'}
                        </Button>
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
                            <Button
                                appearance="primary"
                                icon={<Add24Regular />}
                                onClick={addWidget}
                            >
                                Add Widget
                            </Button>
                        </div>

                        <div className={styles.widgetList}>
                            {widgets.map((widget: WidgetConfig, index: number) => (
                                <Card key={index} className={styles.widgetCard}>
                                    <div className={styles.widgetHeader}>
                                        <Body1><strong>Widget {index + 1}</strong></Body1>
                                        <Button
                                            appearance="subtle"
                                            icon={<Delete24Regular />}
                                            onClick={() => removeWidget(index)}
                                        >
                                            Remove
                                        </Button>
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
                                                value={WIDGET_TYPES.find((t: any) => t.value === widget.type)?.label}
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
                                                value={DATA_SOURCES.find((s: any) => s.value === widget.data_source)?.label}
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

                                        <div className={styles.field}>
                                            <Body1>Group By</Body1>
                                            <Dropdown
                                                value={GROUP_BY_OPTIONS.find((g: any) => g.value === widget.config?.group_by?.[0])?.label}
                                                onOptionSelect={(_ev: any, d: any) =>
                                                    updateWidget(index, {
                                                        config: { ...widget.config, group_by: [d.optionValue as string] },
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

                                        <div className={styles.field}>
                                            <Body1>Width (1-12)</Body1>
                                            <SpinButton
                                                value={widget.config?.width || 6}
                                                min={1}
                                                max={12}
                                                onChange={(_ev: any, data: any) =>
                                                    updateWidget(index, {
                                                        config: { ...widget.config, width: data.value },
                                                    })
                                                }
                                            />
                                        </div>

                                        <div className={styles.field}>
                                            <Body1>Height (px)</Body1>
                                            <SpinButton
                                                value={widget.config?.height || 300}
                                                min={100}
                                                max={800}
                                                step={50}
                                                onChange={(_ev: any, data: any) =>
                                                    updateWidget(index, {
                                                        config: { ...widget.config, height: data.value },
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
                        <Title3>SQL Query Editor</Title3>
                        <Body1 style={{ color: tokens.colorNeutralForeground2, marginBottom: '16px' }}>
                            Write custom SQL queries to create dynamic widgets. Use "Insert as Widget" to add results to your dashboard.
                        </Body1>
                        <DashboardSqlEditor onInsertQuery={handleSqlQueryInsert} />
                    </div>
                </div>
            )}
        </div>
    );
}
