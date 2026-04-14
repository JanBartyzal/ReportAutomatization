/**
 * Export Execution History Panel
 * FS27 – Live Excel Export & External Sync
 */

import { useState } from 'react';
import {
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridCell,
    DataGridBody,
    TableColumnDefinition,
    createTableColumn,
    Badge,
    Button,
    Spinner,
    Body1,
    Caption1,
    Text,
    tokens,
    makeStyles,
} from '@fluentui/react-components';
import { ChevronDownRegular, ChevronRightRegular } from '@fluentui/react-icons';
import { useExecutionHistory } from '../hooks/useExportFlows';
import { getStatusColors, getStatusLabel } from '../theme/statusColors';
import type { ExportFlowExecution } from '@reportplatform/types';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
    },
    emptyState: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '32px',
        gap: '8px',
        color: tokens.colorNeutralForeground3,
    },
    statusBadge: {
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 600,
    },
    errorRow: {
        padding: '8px 12px',
        backgroundColor: tokens.colorPaletteRedBackground1,
        borderRadius: '4px',
        marginTop: '4px',
    },
    errorText: {
        color: tokens.colorPaletteRedForeground1,
        fontFamily: 'monospace',
        fontSize: '12px',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-all',
    },
    pagination: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: '8px',
        paddingTop: '8px',
    },
    expandButton: {
        minWidth: 'unset',
        padding: '2px',
    },
});

function StatusCell({ status }: { status: string }) {
    const styles = useStyles();
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

function formatRelativeTime(dateStr?: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    if (diffDay > 0) return `${diffDay}d ago`;
    if (diffHour > 0) return `${diffHour}h ago`;
    if (diffMin > 0) return `${diffMin}m ago`;
    return 'just now';
}

function formatDuration(startStr?: string, endStr?: string): string {
    if (!startStr || !endStr) return '—';
    const ms = new Date(endStr).getTime() - new Date(startStr).getTime();
    if (ms < 1000) return `${ms}ms`;
    const sec = Math.floor(ms / 1000);
    if (sec < 60) return `${sec}s`;
    return `${Math.floor(sec / 60)}m ${sec % 60}s`;
}

// =============================================================================
// Expandable row (shows error detail inline)
// =============================================================================

interface ExpandableRowProps {
    item: ExportFlowExecution;
    columnCount: number;
}

function ExpandableRow({ item, columnCount }: ExpandableRowProps) {
    const styles = useStyles();
    const [expanded, setExpanded] = useState(false);
    const hasError = !!item.errorMessage;

    return (
        <>
            <DataGridRow<ExportFlowExecution> key={item.id}>
                {({ renderCell, columnId }) => {
                    if (columnId === 'expand') {
                        return (
                            <DataGridCell style={{ width: '32px', paddingRight: 0 }}>
                                {hasError ? (
                                    <Button
                                        appearance="subtle"
                                        size="small"
                                        className={styles.expandButton}
                                        icon={expanded ? <ChevronDownRegular /> : <ChevronRightRegular />}
                                        onClick={() => setExpanded((v) => !v)}
                                        aria-label={expanded ? 'Collapse error' : 'Show error'}
                                    />
                                ) : null}
                            </DataGridCell>
                        );
                    }
                    return <DataGridCell>{renderCell(item)}</DataGridCell>;
                }}
            </DataGridRow>
            {expanded && hasError && (
                <DataGridRow<ExportFlowExecution> key={`${item.id}-err`}>
                    {({ columnId }) => {
                        if (columnId === 'expand') return <DataGridCell />;
                        if (columnId === 'status') {
                            return (
                                <DataGridCell colSpan={columnCount - 1}>
                                    <div className={styles.errorRow}>
                                        <Text className={styles.errorText}>{item.errorMessage}</Text>
                                    </div>
                                </DataGridCell>
                            );
                        }
                        return <DataGridCell />;
                    }}
                </DataGridRow>
            )}
        </>
    );
}

// =============================================================================
// Panel component
// =============================================================================

interface ExportExecutionHistoryProps {
    flowId: string;
}

export function ExportExecutionHistory({ flowId }: ExportExecutionHistoryProps) {
    const styles = useStyles();
    const [page, setPage] = useState(0);
    const pageSize = 10;

    const { data, isLoading } = useExecutionHistory(flowId, { page, size: pageSize });

    const columns: TableColumnDefinition<ExportFlowExecution>[] = [
        createTableColumn<ExportFlowExecution>({
            columnId: 'expand',
            renderHeaderCell: () => '',
            renderCell: () => null,
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => <StatusCell status={item.status} />,
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'startedAt',
            renderHeaderCell: () => 'Started',
            renderCell: (item) => (
                <span title={item.startedAt ? new Date(item.startedAt).toLocaleString() : undefined}>
                    {formatRelativeTime(item.startedAt)}
                </span>
            ),
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'duration',
            renderHeaderCell: () => 'Duration',
            renderCell: (item) => formatDuration(item.startedAt, item.completedAt),
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'rowsExported',
            renderHeaderCell: () => 'Rows',
            renderCell: (item) =>
                item.rowsExported != null ? item.rowsExported.toLocaleString() : '—',
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'targetPathUsed',
            renderHeaderCell: () => 'Output',
            renderCell: (item) =>
                item.targetPathUsed ? (
                    <Caption1 title={item.targetPathUsed}>
                        {item.targetPathUsed.split('/').pop() ?? item.targetPathUsed}
                    </Caption1>
                ) : (
                    '—'
                ),
        }),
        createTableColumn<ExportFlowExecution>({
            columnId: 'triggerSource',
            renderHeaderCell: () => 'Trigger',
            renderCell: (item) => (
                <Badge appearance="outline" size="small">
                    {item.triggerSource ?? 'MANUAL'}
                </Badge>
            ),
        }),
    ];

    if (isLoading) {
        return <Spinner size="small" label="Loading execution history..." />;
    }

    const executions = data?.content ?? [];

    if (executions.length === 0) {
        return (
            <div className={styles.emptyState}>
                <Body1>No executions yet</Body1>
                <Caption1>Run this flow to see execution history here.</Caption1>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <DataGrid
                items={executions}
                columns={columns}
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
                <DataGridBody<ExportFlowExecution>>
                    {({ item }) => (
                        <ExpandableRow
                            key={item.id}
                            item={item}
                            columnCount={columns.length}
                        />
                    )}
                </DataGridBody>
            </DataGrid>

            {(data?.totalPages ?? 0) > 1 && (
                <div className={styles.pagination}>
                    <Caption1>
                        Page {page + 1} of {data?.totalPages}
                    </Caption1>
                    <Button
                        size="small"
                        appearance="subtle"
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                    >
                        Previous
                    </Button>
                    <Button
                        size="small"
                        appearance="subtle"
                        disabled={page >= (data?.totalPages ?? 1) - 1}
                        onClick={() => setPage((p) => p + 1)}
                    >
                        Next
                    </Button>
                </div>
            )}
        </div>
    );
}

export default ExportExecutionHistory;
