/**
 * Service-Now Integration Admin Page
 * P7 - External Integrations & Data Optimization
 */

import React, { useState } from 'react';
import {
    Body1,
    Subtitle1,
    Divider,
    Tab,
    TabList,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridCell,
    DataGridBody,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
    Button,
    Spinner,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    DialogOpenChangeEventArgs,
    Input,
    Label,
    Select,
    Option,
    Toggle,
    Badge,
    MessageBar,
    MessageBarIntent,
} from '@fluentui/react-components';
import {
    AddRegular,
    DeleteRegular,
    EditRegular,
    PlayRegular,
    CheckmarkCircleRegular,
    DismissCircleRegular,
    ClockRegular,
} from '@fluentui/react-icons';
import {
    useConnections,
    useCreateConnection,
    useUpdateConnection,
    useDeleteConnection,
    useTestConnection,
    useAllSchedules,
    useCreateSchedule,
    useDeleteSchedule,
    useTriggerSync,
    useAllSyncHistory,
} from '../hooks/useIntegrations';
import type {
    ServiceNowConnection,
    CreateServiceNowConnectionRequest,
    TestConnectionRequest,
    ServiceNowAuthType,
    SyncSchedule,
    SyncJobHistory,
    ServiceNowTableConfig,
} from '@reportplatform/types';

type IntegrationTab = 'connections' | 'schedules' | 'history';

const IntegrationPage: React.FC = () => {
    const [selectedTab, setSelectedTab] = useState<IntegrationTab>('connections');
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingConnection, setEditingConnection] = useState<ServiceNowConnection | null>(null);
    const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

    // Form state
    const [formData, setFormData] = useState<CreateServiceNowConnectionRequest>({
        instance_url: '',
        auth_type: 'oauth2' as ServiceNowAuthType,
        credentials_ref: '',
        tables: [],
    });

    const [isScheduleDialogOpen, setIsScheduleDialogOpen] = useState(false);
    const [editingSchedule, setEditingSchedule] = useState<any>(null);
    const [scheduleFormData, setScheduleFormData] = useState({
        integration_id: '',
        cron_expression: '0 0 * * *',
        enabled: true,
    });

    // Queries
    const { data: connections, isLoading: loadingConnections } = useConnections();
    const { data: schedules } = useAllSchedules();
    const { data: syncHistory } = useAllSyncHistory();

    // Mutations
    const createConnection = useCreateConnection();
    const updateConnection = useUpdateConnection();
    const deleteConnection = useDeleteConnection();
    const testConnection = useTestConnection();
    const createSchedule = useCreateSchedule();
    const deleteSchedule = useDeleteSchedule();
    const triggerSync = useTriggerSync();

    const handleOpenDialog = (connection?: ServiceNowConnection) => {
        if (connection) {
            setEditingConnection(connection);
            setFormData({
                instance_url: connection.instance_url,
                auth_type: connection.auth_type,
                credentials_ref: connection.credentials_ref,
                tables: connection.tables,
            });
        } else {
            setEditingConnection(null);
            setFormData({
                instance_url: '',
                auth_type: 'oauth2',
                credentials_ref: '',
                tables: [],
            });
        }
        setTestResult(null);
        setIsDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setIsDialogOpen(false);
        setEditingConnection(null);
        setTestResult(null);
    };

    const handleTestConnection = async () => {
        const result = await testConnection.mutateAsync({
            instance_url: formData.instance_url,
            auth_type: formData.auth_type,
            credentials_ref: formData.credentials_ref,
        });
        setTestResult({
            success: result.success,
            message: result.message,
        });
    };

    const handleSaveConnection = async () => {
        if (editingConnection) {
            await updateConnection.mutateAsync({
                connectionId: editingConnection.id,
                connection: formData,
            });
        } else {
            await createConnection.mutateAsync(formData);
        }
        handleCloseDialog();
    };

    const handleDeleteConnection = async (id: string) => {
        if (confirm('Are you sure you want to delete this connection?')) {
            await deleteConnection.mutateAsync(id);
        }
    };

    const handleOpenScheduleDialog = (connectionId: string, schedule?: SyncSchedule) => {
        if (schedule) {
            setEditingSchedule(schedule);
            setScheduleFormData({
                integration_id: schedule.integration_id,
                cron_expression: schedule.cron_expression,
                enabled: schedule.enabled,
            });
        } else {
            setEditingSchedule(null);
            setScheduleFormData({
                integration_id: connectionId,
                cron_expression: '0 0 * * *',
                enabled: true,
            });
        }
        setIsScheduleDialogOpen(true);
    };

    const handleSaveSchedule = async () => {
        try {
            if (editingSchedule) {
                // await updateSchedule.mutateAsync({ scheduleId: editingSchedule.id, schedule: scheduleFormData });
            } else {
                await createSchedule.mutateAsync(scheduleFormData);
            }
            setIsScheduleDialogOpen(false);
        } catch (error) {
            console.error('Failed to save schedule:', error);
        }
    };

    const handleTriggerSync = async (scheduleId: string) => {
        await triggerSync.mutateAsync(scheduleId);
    };

    // Table columns
    const connectionColumns: TableColumnDefinition<ServiceNowConnection>[] = [
        createTableColumn<ServiceNowConnection>({
            columnId: 'instance_url',
            compare: (a, b) => a.instance_url.localeCompare(b.instance_url),
            renderHeaderCell: () => 'Instance URL',
        }),
        createTableColumn<ServiceNowConnection>({
            columnId: 'auth_type',
            renderHeaderCell: () => 'Auth Type',
            renderCell: (item) => item.auth_type.toUpperCase(),
        }),
        createTableColumn<ServiceNowConnection>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => {
                const statusColors: Record<string, MessageBarIntent> = {
                    active: 'success',
                    inactive: 'warning',
                    error: 'error',
                    testing: 'info',
                };
                return <Badge appearance="filled" color={statusColors[item.status] || 'neutral'}>{item.status}</Badge>;
            },
        }),
        createTableColumn<ServiceNowConnection>({
            columnId: 'last_sync',
            renderHeaderCell: () => 'Last Sync',
            renderCell: (item) => item.last_sync ? new Date(item.last_sync).toLocaleString() : 'Never',
        }),
        createTableColumn<ServiceNowConnection>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<EditRegular />}
                        onClick={() => handleOpenDialog(item)}
                        title="Edit Connection"
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<AddRegular />}
                        onClick={() => handleOpenScheduleDialog(item.id)}
                        title="Add Schedule"
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<DeleteRegular />}
                        onClick={() => handleDeleteConnection(item.id)}
                        title="Delete Connection"
                    />
                </div>
            ),
        }),
    ];

    return (
        <div className="integration-page">
            <div className="page-header">
                <Subtitle1>Service-Now Integration</Subtitle1>
                <Body1>Manage Service-Now connections, sync schedules, and data retrieval</Body1>
            </div>

            <Divider className="divider" />

            <TabList
                selectedValue={selectedTab}
                onTabSelect={(_, data) => setSelectedTab(data.value as IntegrationTab)}
            >
                <Tab value="connections">Connections</Tab>
                <Tab value="schedules">Schedules</Tab>
                <Tab value="history">Sync History</Tab>
            </TabList>

            {selectedTab === 'connections' && (
                <div className="tab-content">
                    <div className="action-bar">
                        <Dialog open={isDialogOpen} onOpenChange={(_: unknown, data: DialogOpenChangeEventArgs) => !data.open && handleCloseDialog()}>
                            <DialogTrigger disableButtonEnhancement>
                                <Button
                                    appearance="primary"
                                    icon={<AddRegular />}
                                    onClick={() => handleOpenDialog()}
                                >
                                    Add Connection
                                </Button>
                            </DialogTrigger>
                            <DialogSurface>
                                <DialogBody>
                                    <DialogTitle>
                                        {editingConnection ? 'Edit Connection' : 'New Connection'}
                                    </DialogTitle>
                                    <DialogContent>
                                        <div className="form-fields">
                                            <Label required>Instance URL</Label>
                                            <Input
                                                value={formData.instance_url}
                                                onChange={(_, data) => setFormData({ ...formData, instance_url: data.value })}
                                                placeholder="https://instance.service-now.com"
                                            />

                                            <Label required>Authentication Type</Label>
                                            <Select
                                                value={formData.auth_type}
                                                onChange={(_, data) => setFormData({ ...formData, auth_type: data.value as ServiceNowAuthType })}
                                            >
                                                <Option value="oauth2">OAuth 2.0</Option>
                                                <Option value="basic">Basic Auth</Option>
                                            </Select>

                                            <Label required>Credentials Reference (KeyVault)</Label>
                                            <Input
                                                value={formData.credentials_ref}
                                                onChange={(_, data) => setFormData({ ...formData, credentials_ref: data.value })}
                                                placeholder="servicenow-credentials"
                                            />

                                            <Label>Tables to Sync</Label>
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                                {formData.tables.map((table, index) => (
                                                    <div key={index} style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
                                                        <div style={{ flex: 1 }}>
                                                            <Label size="small">Table Name</Label>
                                                            <Input
                                                                size="small"
                                                                value={table.table_name}
                                                                onChange={(_, data) => {
                                                                    const newTables = [...formData.tables];
                                                                    newTables[index] = { ...table, table_name: data.value };
                                                                    setFormData({ ...formData, tables: newTables });
                                                                }}
                                                            />
                                                        </div>
                                                        <div style={{ flex: 1 }}>
                                                            <Label size="small">Mapping Template ID</Label>
                                                            <Input
                                                                size="small"
                                                                value={table.mapping_template_id || ''}
                                                                onChange={(_, data) => {
                                                                    const newTables = [...formData.tables];
                                                                    newTables[index] = { ...table, mapping_template_id: data.value };
                                                                    setFormData({ ...formData, tables: newTables });
                                                                }}
                                                            />
                                                        </div>
                                                        <Button
                                                            size="small"
                                                            icon={<DeleteRegular />}
                                                            onClick={() => {
                                                                const newTables = formData.tables.filter((_, i) => i !== index);
                                                                setFormData({ ...formData, tables: newTables });
                                                            }}
                                                        />
                                                    </div>
                                                ))}
                                                <Button
                                                    size="small"
                                                    icon={<AddRegular />}
                                                    onClick={() => setFormData({ ...formData, tables: [...formData.tables, { table_name: '', mapping_template_id: '' }] })}
                                                >
                                                    Add Table
                                                </Button>
                                            </div>

                                            {testResult && (
                                                <MessageBar intent={testResult.success ? 'success' : 'error'}>
                                                    {testResult.message}
                                                </MessageBar>
                                            )}
                                        </div>
                                    </DialogContent>
                                    <DialogActions>
                                        <Button appearance="secondary" onClick={handleTestConnection}>
                                            Test Connection
                                        </Button>
                                        <Button appearance="secondary" onClick={handleCloseDialog}>
                                            Cancel
                                        </Button>
                                        <Button appearance="primary" onClick={handleSaveConnection}>
                                            {editingConnection ? 'Update' : 'Create'}
                                        </Button>
                                    </DialogActions>
                                </DialogBody>
                            </DialogSurface>
                        </Dialog>
                    </div>

                    {loadingConnections ? (
                        <Spinner label="Loading connections..." />
                    ) : (
                        <DataGrid
                            items={connections || []}
                            columns={connectionColumns}
                            sortable
                            style={{ minWidth: '100%' }}
                        >
                            <DataGridHeader>
                                <DataGridRow>
                                    {({ renderHeaderCell }) => (
                                        <DataGridCell>{renderHeaderCell()}</DataGridCell>
                                    )}
                                </DataGridRow>
                            </DataGridHeader>
                        <DataGridBody<ServiceNowConnection>>
                            {({ item, rowId }: { item: ServiceNowConnection; rowId: string }) => (
                                <DataGridRow<ServiceNowConnection> key={rowId}>
                                    {({ renderCell }: { renderCell: (item: ServiceNowConnection) => React.ReactNode }) => (
                                        <DataGridCell>{renderCell(item)}</DataGridCell>
                                    )}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                    </DataGrid>
                    )}
                </div>
            )}

            {selectedTab === 'schedules' && (
                <div className="tab-content">
                    <DataGrid
                        items={schedules || []}
                        columns={[
                            createTableColumn({
                                columnId: 'integration_id',
                                renderHeaderCell: () => 'Connection',
                                renderCell: (item) => connections?.find(c => c.id === item.integration_id)?.instance_url || item.integration_id,
                            }),
                            createTableColumn({
                                columnId: 'cron_expression',
                                renderHeaderCell: () => 'Schedule',
                                renderCell: (item) => item.cron_expression,
                            }),
                            createTableColumn({
                                columnId: 'enabled',
                                renderHeaderCell: () => 'Enabled',
                                renderCell: (item) => <Badge appearance="filled" color={item.enabled ? 'success' : 'warning'}>{item.enabled ? 'Yes' : 'No'}</Badge>,
                            }),
                            createTableColumn({
                                columnId: 'next_run',
                                renderHeaderCell: () => 'Next Run',
                                renderCell: (item) => item.next_run ? new Date(item.next_run).toLocaleString() : 'N/A',
                            }),
                            createTableColumn({
                                columnId: 'actions',
                                renderHeaderCell: () => 'Actions',
                                renderCell: (item) => (
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<PlayRegular />}
                                            onClick={() => handleTriggerSync(item.id)}
                                            title="Run Now"
                                        />
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<DeleteRegular />}
                                            onClick={() => deleteSchedule.mutate(item.id)}
                                            title="Delete"
                                        />
                                    </div>
                                ),
                            }),
                        ]}
                        style={{ minWidth: '100%' }}
                    >
                        <DataGridHeader>
                            <DataGridRow>
                                {({ renderHeaderCell }) => (
                                    <DataGridCell>{renderHeaderCell()}</DataGridCell>
                                )}
                            </DataGridRow>
                        </DataGridHeader>
                        <DataGridBody<SyncJobHistory>>
                            {({ item, rowId }: { item: SyncJobHistory; rowId: string }) => (
                                <DataGridRow<SyncJobHistory> key={rowId}>
                                    {({ renderCell }: { renderCell: (item: SyncJobHistory) => React.ReactNode }) => (
                                        <DataGridCell>{renderCell(item)}</DataGridCell>
                                    )}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                    </DataGrid>
                </div>
            )}

            {selectedTab === 'history' && (
                <div className="tab-content">
                    <DataGrid
                        items={syncHistory?.items || []}
                        columns={[
                            createTableColumn({
                                columnId: 'schedule_id',
                                renderHeaderCell: () => 'Schedule',
                                renderCell: (item) => schedules?.find(s => s.id === item.schedule_id)?.cron_expression || item.schedule_id,
                            }),
                            createTableColumn({
                                columnId: 'start_time',
                                renderHeaderCell: () => 'Start Time',
                                renderCell: (item) => new Date(item.start_time).toLocaleString(),
                            }),
                            createTableColumn({
                                columnId: 'records_fetched',
                                renderHeaderCell: () => 'Records',
                                renderCell: (item) => `${item.records_fetched} fetched, ${item.records_stored} stored`,
                            }),
                            createTableColumn({
                                columnId: 'status',
                                renderHeaderCell: () => 'Status',
                                renderCell: (item) => (
                                    <Badge appearance="filled" color={item.status === 'completed' ? 'success' : item.status === 'failed' ? 'error' : 'info'}>
                                        {item.status}
                                    </Badge>
                                ),
                            }),
                            createTableColumn({
                                columnId: 'error_detail',
                                renderHeaderCell: () => 'Error',
                                renderCell: (item) => item.error_detail || '-',
                            }),
                        ]}
                        style={{ minWidth: '100%' }}
                    >
                        <DataGridHeader>
                            <DataGridRow>
                                {({ renderHeaderCell }) => (
                                    <DataGridCell>{renderHeaderCell()}</DataGridCell>
                                )}
                            </DataGridRow>
                        </DataGridHeader>
                        <DataGridBody<SyncJobHistory>>
                            {({ item, rowId }: { item: SyncJobHistory; rowId: string }) => (
                                <DataGridRow<SyncJobHistory> key={rowId}>
                                    {({ renderCell }: { renderCell: (item: SyncJobHistory) => React.ReactNode }) => (
                                        <DataGridCell>{renderCell(item)}</DataGridCell>
                                    )}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                    </DataGrid>
                </div>
            )}

        {/* Schedule Configuration Dialog */}
        <Dialog open={isScheduleDialogOpen} onOpenChange={(_: unknown, data: any) => setIsScheduleDialogOpen(data.open)}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>{editingSchedule ? 'Edit Sync Schedule' : 'New Sync Schedule'}</DialogTitle>
                    <DialogContent>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px' }}>
                            <Label required>Cron Expression</Label>
                            <Input
                                value={scheduleFormData.cron_expression}
                                onChange={(_, data) => setScheduleFormData({ ...scheduleFormData, cron_expression: data.value })}
                                placeholder="0 0 * * *"
                            />
                            <Body1 size={200} italic>Format: minute hour day-of-month month day-of-week</Body1>
                            
                            <Toggle
                                label="Enabled"
                                checked={scheduleFormData.enabled}
                                onChange={(_, data) => setScheduleFormData({ ...scheduleFormData, enabled: data.checked })}
                            />
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={() => setIsScheduleDialogOpen(false)}>Cancel</Button>
                        <Button appearance="primary" onClick={handleSaveSchedule}>Save</Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    </div>
);
};

export default IntegrationPage;
