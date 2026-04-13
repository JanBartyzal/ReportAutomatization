/**
 * Export Flows List Page
 * FS27 – Live Excel Export & External Sync
 *
 * Features:
 *  - DataGrid: Name, Target Type, Sheet, Trigger, Last Execution Status, Actions
 *  - Filter by search / targetType / status
 *  - Auto-refresh every 10 s when any flow has a RUNNING last execution
 *  - "Export Now" confirmation dialog
 *  - Create / Edit via ExportFlowDialog
 *  - Execution history side-drawer
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
    Title2,
    Body1,
    Caption1,
    Divider,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridCell,
    DataGridBody,
    TableColumnDefinition,
    createTableColumn,
    Button,
    Spinner,
    Input,
    Select,
    Option,
    Dialog,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    Drawer,
    DrawerHeader,
    DrawerHeaderTitle,
    DrawerBody,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    AddRegular,
    EditRegular,
    DeleteRegular,
    PlayRegular,
    ArrowSyncRegular,
    DismissRegular,
    HistoryRegular,
    SearchRegular,
} from '@fluentui/react-icons';
import {
    useExportFlows,
    useDeleteExportFlow,
    useExecuteExportFlow,
} from '../hooks/useExportFlows';
import { ExportFlowDialog } from '../components/ExportFlowDialog';
import { ExportExecutionHistory } from '../components/ExportExecutionHistory';
import { getStatusColors, getStatusLabel } from '../theme/statusColors';
import type { ExportFlowDefinition } from '@reportplatform/types';

const useStyles = makeStyles({
    page: {
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '4px',
    },
    filterBar: {
        display: 'flex',
        gap: '12px',
        alignItems: 'center',
        margin: '16px 0',
        flexWrap: 'wrap',
    },
    searchInput: {
        flex: '1 1 220px',
        maxWidth: '320px',
    },
    statusBadge: {
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 600,
    },
    emptyState: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '64px 24px',
        gap: '12px',
        color: tokens.colorNeutralForeground3,
        textAlign: 'center',
    },
    actionsCell: {
        display: 'flex',
        gap: '4px',
    },
    refreshing: {
        display: 'flex',
        alignItems: 'center',
        gap: '6px',
        color: tokens.colorNeutralForeground3,
    },
    drawerContent: {
        padding: '16px',
    },
});

// =============================================================================
// Badge helpers
// =============================================================================

function StatusBadge({ status }: { status?: string }) {
    const styles = useStyles();
    if (!status) return <span style={{ color: tokens.colorNeutralForeground3 }}>—</span>;
    const colors = getStatusColors(status);
    return (
        <span
            className={styles.statusBadge}
            style={{ backgroundColor: colors.bg, color: colors.text }}
        >
            {getStatusLabel(status)}
        </span>
    );
}

function TargetBadge({ targetType }: { targetType: string }) {
    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                fontWeight: 600,
                backgroundColor: targetType === 'SHAREPOINT' ? '#EFF6FF' : '#F3F2F1',
                color: targetType === 'SHAREPOINT' ? '#1D4ED8' : '#616161',
            }}
        >
            {targetType === 'SHAREPOINT' ? 'SharePoint' : 'Local Path'}
        </span>
    );
}

function TriggerBadge({ triggerType }: { triggerType: string }) {
    const colors =
        triggerType === 'AUTO'
            ? { bg: '#F0FDF4', text: '#166534' }
            : { bg: '#F3F2F1', text: '#616161' };
    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                padding: '2px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                fontWeight: 600,
                backgroundColor: colors.bg,
                color: colors.text,
            }}
        >
            {triggerType}
        </span>
    );
}

function formatRelativeTime(dateStr?: string): string {
    if (!dateStr) return '—';
    const diffMs = Date.now() - new Date(dateStr).getTime();
    const diffMin = Math.floor(diffMs / 60000);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    if (diffDay > 0) return `${diffDay}d ago`;
    if (diffHour > 0) return `${diffHour}h ago`;
    if (diffMin > 0) return `${diffMin}m ago`;
    return 'just now';
}

// =============================================================================
// Execute Now confirmation dialog
// =============================================================================

interface ExecuteConfirmProps {
    flow: ExportFlowDefinition | null;
    onConfirm: () => void;
    onCancel: () => void;
    isPending: boolean;
}

function ExecuteConfirmDialog({ flow, onConfirm, onCancel, isPending }: ExecuteConfirmProps) {
    return (
        <Dialog open={!!flow} onOpenChange={(_, d) => !d.open && onCancel()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>Export Now</DialogTitle>
                    <DialogContent>
                        <Body1>
                            Run <strong>{flow?.name}</strong> immediately? A new execution will be
                            queued.
                        </Body1>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={onCancel} disabled={isPending}>
                            Cancel
                        </Button>
                        <Button
                            appearance="primary"
                            icon={isPending ? <Spinner size="tiny" /> : <PlayRegular />}
                            onClick={onConfirm}
                            disabled={isPending}
                        >
                            Export Now
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

// =============================================================================
// Page
// =============================================================================

const ExportFlowsPage: React.FC = () => {
    const styles = useStyles();

    const [search, setSearch] = useState('');
    const [targetTypeFilter, setTargetTypeFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState('');

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingFlow, setEditingFlow] = useState<ExportFlowDefinition | undefined>(undefined);
    const [executeFlow, setExecuteFlow] = useState<ExportFlowDefinition | null>(null);
    const [historyFlow, setHistoryFlow] = useState<ExportFlowDefinition | null>(null);

    const { data: flows, isLoading, refetch } = useExportFlows({
        search: search || undefined,
        targetType: targetTypeFilter || undefined,
        status: statusFilter || undefined,
    });

    const deleteFlow = useDeleteExportFlow();
    const executeMutation = useExecuteExportFlow();

    // Auto-refresh every 10 s when any flow has a RUNNING last execution
    const hasRunning = flows?.some((f) => f.lastExecution?.status === 'RUNNING');
    useEffect(() => {
        if (!hasRunning) return;
        const id = setInterval(() => refetch(), 10_000);
        return () => clearInterval(id);
    }, [hasRunning, refetch]);

    const handleEdit = useCallback((flow: ExportFlowDefinition) => {
        setEditingFlow(flow);
        setDialogOpen(true);
    }, []);

    const handleNew = useCallback(() => {
        setEditingFlow(undefined);
        setDialogOpen(true);
    }, []);

    const handleDialogClose = useCallback(() => {
        setDialogOpen(false);
        setEditingFlow(undefined);
    }, []);

    const handleDelete = useCallback(
        async (flow: ExportFlowDefinition) => {
            if (!confirm(`Delete export flow "${flow.name}"? This cannot be undone.`)) return;
            await deleteFlow.mutateAsync(flow.id);
        },
        [deleteFlow]
    );

    const handleExecuteConfirm = async () => {
        if (!executeFlow) return;
        await executeMutation.mutateAsync(executeFlow.id);
        setExecuteFlow(null);
    };

    const columns: TableColumnDefinition<ExportFlowDefinition>[] = [
        createTableColumn<ExportFlowDefinition>({
            columnId: 'name',
            compare: (a, b) => a.name.localeCompare(b.name),
            renderHeaderCell: () => 'Name',
            renderCell: (item) => (
                <div>
                    <Body1>{item.name}</Body1>
                    {item.description && (
                        <Caption1 style={{ color: tokens.colorNeutralForeground3, display: 'block' }}>
                            {item.description}
                        </Caption1>
                    )}
                </div>
            ),
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'targetType',
            renderHeaderCell: () => 'Target',
            renderCell: (item) => <TargetBadge targetType={item.targetType} />,
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'targetSheet',
            renderHeaderCell: () => 'Sheet',
            renderCell: (item) => item.targetSheet,
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'triggerType',
            renderHeaderCell: () => 'Trigger',
            renderCell: (item) => <TriggerBadge triggerType={item.triggerType} />,
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'lastExport',
            renderHeaderCell: () => 'Last Export',
            renderCell: (item) => (
                <span
                    title={
                        item.lastExecution?.startedAt
                            ? new Date(item.lastExecution.startedAt).toLocaleString()
                            : undefined
                    }
                >
                    {formatRelativeTime(item.lastExecution?.startedAt)}
                </span>
            ),
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'lastStatus',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => <StatusBadge status={item.lastExecution?.status} />,
        }),
        createTableColumn<ExportFlowDefinition>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <div className={styles.actionsCell}>
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<PlayRegular />}
                        onClick={() => setExecuteFlow(item)}
                        title="Export Now"
                        disabled={item.lastExecution?.status === 'RUNNING'}
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<HistoryRegular />}
                        onClick={() => setHistoryFlow(item)}
                        title="Execution History"
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<EditRegular />}
                        onClick={() => handleEdit(item)}
                        title="Edit"
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<DeleteRegular />}
                        onClick={() => handleDelete(item)}
                        title="Delete"
                    />
                </div>
            ),
        }),
    ];

    return (
        <div className={styles.page}>
            {/* Page header */}
            <div className={styles.header}>
                <div>
                    <Title2 block>Export Flows</Title2>
                    <Body1 style={{ color: tokens.colorNeutralForeground3 }}>
                        Manage automated Excel exports to local paths or SharePoint.
                    </Body1>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    {hasRunning && (
                        <span className={styles.refreshing}>
                            <ArrowSyncRegular />
                            <Caption1>Refreshing…</Caption1>
                        </span>
                    )}
                    <Button appearance="primary" icon={<AddRegular />} onClick={handleNew}>
                        New Export Flow
                    </Button>
                </div>
            </div>

            <Divider style={{ margin: '12px 0' }} />

            {/* Filters */}
            <div className={styles.filterBar}>
                <Input
                    className={styles.searchInput}
                    placeholder="Search by name…"
                    contentBefore={<SearchRegular />}
                    value={search}
                    onChange={(_, d) => setSearch(d.value)}
                />
                <Select
                    value={targetTypeFilter}
                    onChange={(_, d) => setTargetTypeFilter(d.value)}
                    style={{ minWidth: '160px' }}
                >
                    <Option value="">All Targets</Option>
                    <Option value="LOCAL_PATH">Local Path</Option>
                    <Option value="SHAREPOINT">SharePoint</Option>
                </Select>
                <Select
                    value={statusFilter}
                    onChange={(_, d) => setStatusFilter(d.value)}
                    style={{ minWidth: '160px' }}
                >
                    <Option value="">All Statuses</Option>
                    <Option value="PENDING">Pending</Option>
                    <Option value="RUNNING">Running</Option>
                    <Option value="SUCCESS">Success</Option>
                    <Option value="FAILED">Failed</Option>
                </Select>
                {(search || targetTypeFilter || statusFilter) && (
                    <Button
                        appearance="subtle"
                        size="small"
                        onClick={() => {
                            setSearch('');
                            setTargetTypeFilter('');
                            setStatusFilter('');
                        }}
                    >
                        Clear filters
                    </Button>
                )}
            </div>

            {/* Table */}
            {isLoading ? (
                <Spinner label="Loading export flows…" style={{ marginTop: '40px' }} />
            ) : !flows || flows.length === 0 ? (
                <div className={styles.emptyState}>
                    <Body1>No export flows found</Body1>
                    <Caption1>
                        {search || targetTypeFilter || statusFilter
                            ? 'Try clearing the filters.'
                            : 'Create your first export flow to get started.'}
                    </Caption1>
                    {!search && !targetTypeFilter && !statusFilter && (
                        <Button appearance="primary" icon={<AddRegular />} onClick={handleNew}>
                            New Export Flow
                        </Button>
                    )}
                </div>
            ) : (
                <DataGrid
                    items={flows}
                    columns={columns}
                    sortable
                    getRowId={(item) => item.id}
                    style={{ minWidth: '100%' }}
                >
                    <DataGridHeader>
                        <DataGridRow>
                            {({ renderHeaderCell }) => (
                                <DataGridCell>{renderHeaderCell()}</DataGridCell>
                            )}
                        </DataGridRow>
                    </DataGridHeader>
                    <DataGridBody<ExportFlowDefinition>>
                        {({ item, rowId }) => (
                            <DataGridRow<ExportFlowDefinition> key={rowId}>
                                {({ renderCell }) => (
                                    <DataGridCell>{renderCell(item)}</DataGridCell>
                                )}
                            </DataGridRow>
                        )}
                    </DataGridBody>
                </DataGrid>
            )}

            {/* Create / Edit dialog */}
            <ExportFlowDialog
                open={dialogOpen}
                onClose={handleDialogClose}
                editFlow={editingFlow}
            />

            {/* Execute Now confirm */}
            <ExecuteConfirmDialog
                flow={executeFlow}
                onConfirm={handleExecuteConfirm}
                onCancel={() => setExecuteFlow(null)}
                isPending={executeMutation.isPending}
            />

            {/* Execution History Drawer */}
            <Drawer
                type="overlay"
                position="end"
                size="medium"
                open={!!historyFlow}
                onOpenChange={(_, d) => !d.open && setHistoryFlow(null)}
            >
                <DrawerHeader>
                    <DrawerHeaderTitle
                        action={
                            <Button
                                appearance="subtle"
                                icon={<DismissRegular />}
                                onClick={() => setHistoryFlow(null)}
                            />
                        }
                    >
                        Execution History — {historyFlow?.name}
                    </DrawerHeaderTitle>
                </DrawerHeader>
                <DrawerBody className={styles.drawerContent}>
                    {historyFlow && <ExportExecutionHistory flowId={historyFlow.id} />}
                </DrawerBody>
            </Drawer>
        </div>
    );
};

export default ExportFlowsPage;
