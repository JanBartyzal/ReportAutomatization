import React, { useState } from 'react';
import {
    Body1,
    Button,
    Spinner,
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Input,
    Label,
    Badge,
    makeStyles,
    tokens,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Textarea,
} from '@fluentui/react-components';
import { Add24Regular, Delete24Regular, Attach24Regular, Dismiss24Regular } from '@fluentui/react-icons';
import { useOrganizations } from '../../hooks/useAdmin';
import {
    useBatches,
    useCreateBatch,
    useDeleteBatch,
    useBatchFiles,
    useAddFileToBatch,
    useRemoveFileFromBatch,
} from '../../hooks/useBatches';
import type { OrganizationAdmin } from '@reportplatform/types';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        marginTop: '8px',
    },
    filterRow: {
        display: 'flex',
        gap: '12px',
        alignItems: 'flex-end',
    },
    emptyState: {
        padding: '32px',
        textAlign: 'center' as const,
        color: tokens.colorNeutralForeground3,
    },
    error: {
        color: tokens.colorPaletteRedForeground1,
    },
    filesSection: {
        paddingLeft: '24px',
        paddingTop: '8px',
        paddingBottom: '8px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: '4px',
        marginTop: '4px',
    },
    fileRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '4px 0',
    },
    expandedRow: {
        padding: '8px 16px',
    },
});

const statusColor = (status: string) => {
    switch (status) {
        case 'OPEN': return 'success' as const;
        case 'COLLECTING': return 'warning' as const;
        case 'CLOSED': return 'subtle' as const;
        default: return 'informative' as const;
    }
};

function flattenHoldings(orgs: OrganizationAdmin[]): OrganizationAdmin[] {
    const result: OrganizationAdmin[] = [];
    const recurse = (list: OrganizationAdmin[]) => {
        for (const org of list) {
            if (org.type === 'HOLDING') result.push(org);
            if (org.children) recurse(org.children);
        }
    };
    recurse(orgs);
    return result;
}

const BatchFilesRow: React.FC<{ batchId: string }> = ({ batchId }) => {
    const styles = useStyles();
    const { data: files, isLoading } = useBatchFiles(batchId);
    const removeFile = useRemoveFileFromBatch();
    const addFile = useAddFileToBatch();
    const [showAddFile, setShowAddFile] = useState(false);
    const [newFileId, setNewFileId] = useState('');

    const handleAddFile = async () => {
        if (!newFileId.trim()) return;
        await addFile.mutateAsync({ batchId, fileId: newFileId.trim() });
        setNewFileId('');
        setShowAddFile(false);
    };

    if (isLoading) return <Spinner size="tiny" label="Loading files..." />;

    return (
        <div className={styles.filesSection}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <Body1><strong>Files ({files?.length || 0})</strong></Body1>
                <Button
                    size="small"
                    appearance="subtle"
                    icon={<Attach24Regular />}
                    onClick={() => setShowAddFile(true)}
                >
                    Add File
                </Button>
            </div>

            {showAddFile && (
                <div style={{ display: 'flex', gap: '8px', marginBottom: '8px', alignItems: 'flex-end' }}>
                    <Input
                        size="small"
                        value={newFileId}
                        onChange={(_e: any, data: any) => setNewFileId(data.value)}
                        placeholder="File ID (UUID)"
                        style={{ flex: 1 }}
                    />
                    <Button size="small" appearance="primary" onClick={handleAddFile} disabled={addFile.isPending}>
                        Add
                    </Button>
                    <Button size="small" appearance="subtle" onClick={() => setShowAddFile(false)}>
                        Cancel
                    </Button>
                </div>
            )}

            {(files || []).length === 0 ? (
                <Body1>No files assigned to this batch yet.</Body1>
            ) : (
                (files || []).map((f: any) => (
                    <div key={f.id} className={styles.fileRow}>
                        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{f.fileId}</span>
                        <span style={{ fontSize: '12px', color: tokens.colorNeutralForeground3 }}>
                            {new Date(f.addedAt).toLocaleDateString()}
                        </span>
                        <Button
                            size="small"
                            appearance="subtle"
                            icon={<Dismiss24Regular />}
                            onClick={() => removeFile.mutate({ batchId, fileId: f.fileId })}
                            title="Remove file"
                        />
                    </div>
                ))
            )}
        </div>
    );
};

const BatchesPanel: React.FC = () => {
    const { data: organizations } = useOrganizations();
    const [filterHoldingId, setFilterHoldingId] = useState<string>('');
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [expandedBatchId, setExpandedBatchId] = useState<string | null>(null);

    const [newName, setNewName] = useState('');
    const [newPeriod, setNewPeriod] = useState('');
    const [newDescription, setNewDescription] = useState('');
    const [newHoldingId, setNewHoldingId] = useState('');

    const styles = useStyles();

    const { data: batches, isLoading, error } = useBatches(filterHoldingId || undefined);
    const createBatch = useCreateBatch();
    const deleteBatch = useDeleteBatch();

    const holdings = flattenHoldings(organizations || []);

    if (isLoading) {
        return <Spinner label="Loading batches..." />;
    }

    if (error) {
        return <Body1 className={styles.error}>Error loading batches: {error.message}</Body1>;
    }

    const handleCreate = async () => {
        if (!newName.trim() || !newPeriod.trim() || !newHoldingId) return;

        await createBatch.mutateAsync({
            name: newName,
            period: newPeriod,
            description: newDescription || undefined,
            holding_id: newHoldingId,
        });

        setNewName('');
        setNewPeriod('');
        setNewDescription('');
        setNewHoldingId('');
        setIsCreateOpen(false);
    };

    const handleDelete = async (batchId: string) => {
        if (confirm('Are you sure you want to delete this batch?')) {
            await deleteBatch.mutateAsync(batchId);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Body1>Batch Management</Body1>
                <Button
                    appearance="primary"
                    icon={<Add24Regular />}
                    onClick={() => setIsCreateOpen(true)}
                >
                    Create Batch
                </Button>
            </div>

            <div className={styles.filterRow}>
                <div>
                    <Label>Filter by Holding</Label>
                    <select
                        value={filterHoldingId}
                        onChange={(e) => setFilterHoldingId(e.target.value)}
                        style={{
                            padding: '6px 8px',
                            borderRadius: '4px',
                            border: `1px solid ${tokens.colorNeutralStroke1}`,
                            backgroundColor: tokens.colorNeutralBackground1,
                            color: tokens.colorNeutralForeground1,
                            fontSize: '14px',
                            width: '100%',
                        }}
                    >
                        <option value="">All Holdings</option>
                        {holdings.map((org) => (
                            <option key={org.id} value={org.id}>
                                {org.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {(batches || []).length === 0 ? (
                <div className={styles.emptyState}>
                    <Body1>No batches found. Create a batch to start collecting files.</Body1>
                </div>
            ) : (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>Name</TableHeaderCell>
                            <TableHeaderCell>Period</TableHeaderCell>
                            <TableHeaderCell>Status</TableHeaderCell>
                            <TableHeaderCell>Created</TableHeaderCell>
                            <TableHeaderCell>Actions</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {(batches || []).map((batch: any) => (
                            <React.Fragment key={batch.id}>
                                <TableRow>
                                    <TableCell>
                                        <Button
                                            appearance="subtle"
                                            size="small"
                                            onClick={() => setExpandedBatchId(
                                                expandedBatchId === batch.id ? null : batch.id
                                            )}
                                        >
                                            {batch.name}
                                        </Button>
                                    </TableCell>
                                    <TableCell>{batch.period}</TableCell>
                                    <TableCell>
                                        <Badge color={statusColor(batch.status)} appearance="filled" size="small">
                                            {batch.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell>
                                        {new Date(batch.createdAt).toLocaleDateString()}
                                    </TableCell>
                                    <TableCell>
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<Delete24Regular />}
                                            onClick={() => handleDelete(batch.id)}
                                            title="Delete batch"
                                        />
                                    </TableCell>
                                </TableRow>
                                {expandedBatchId === batch.id && (
                                    <TableRow>
                                        <TableCell colSpan={5}>
                                            <div className={styles.expandedRow}>
                                                {batch.description && (
                                                    <Body1 style={{ marginBottom: '8px' }}>{batch.description}</Body1>
                                                )}
                                                <BatchFilesRow batchId={batch.id} />
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                )}
                            </React.Fragment>
                        ))}
                    </TableBody>
                </Table>
            )}

            <Dialog open={isCreateOpen} onOpenChange={(_: any, d: any) => setIsCreateOpen(d.open)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Create Batch</DialogTitle>
                        <DialogContent>
                            <div className={styles.form}>
                                <Label required>Batch Name</Label>
                                <Input
                                    value={newName}
                                    onChange={(_e: any, data: any) => setNewName(data.value)}
                                    placeholder="e.g. Q1-2026 OPEX Reports"
                                />
                                <Label required>Period</Label>
                                <Input
                                    value={newPeriod}
                                    onChange={(_e: any, data: any) => setNewPeriod(data.value)}
                                    placeholder="e.g. Q1-2026"
                                />
                                <Label required>Holding Organization</Label>
                                <select
                                    value={newHoldingId}
                                    onChange={(e) => setNewHoldingId(e.target.value)}
                                    style={{
                                        padding: '6px 8px',
                                        borderRadius: '4px',
                                        border: `1px solid ${tokens.colorNeutralStroke1}`,
                                        backgroundColor: tokens.colorNeutralBackground1,
                                        color: tokens.colorNeutralForeground1,
                                        fontSize: '14px',
                                        width: '100%',
                                    }}
                                >
                                    <option value="">-- Select holding --</option>
                                    {holdings.map((org) => (
                                        <option key={org.id} value={org.id}>
                                            {org.name}
                                        </option>
                                    ))}
                                </select>
                                <Label>Description</Label>
                                <Textarea
                                    value={newDescription}
                                    onChange={(_e: any, data: any) => setNewDescription(data.value)}
                                    placeholder="Optional description"
                                />
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <Button appearance="secondary" onClick={() => setIsCreateOpen(false)}>
                                Cancel
                            </Button>
                            <Button
                                appearance="primary"
                                onClick={handleCreate}
                                disabled={createBatch.isPending || !newName || !newPeriod || !newHoldingId}
                            >
                                {createBatch.isPending ? 'Creating...' : 'Create'}
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default BatchesPanel;
