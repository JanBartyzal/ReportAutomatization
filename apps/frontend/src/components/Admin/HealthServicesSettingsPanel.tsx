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
    Switch,
    Table,
    TableBody,
    TableCell,
    TableCellLayout,
    TableHeader,
    TableHeaderCell,
    TableRow,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Add24Regular, Edit24Regular, Delete24Regular } from '@fluentui/react-icons';
import {
    useHealthServices,
    useCreateHealthService,
    useUpdateHealthService,
    useDeleteHealthService,
} from '../../hooks/useHealthServices';
import type { HealthServiceConfig, CreateHealthServiceRequest } from '../../api/health';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
        marginTop: tokens.spacingVerticalS,
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
    },
});

const emptyForm: CreateHealthServiceRequest = {
    serviceId: '',
    displayName: '',
    healthUrl: '',
    enabled: true,
    sortOrder: 0,
};

const HealthServicesSettingsPanel: React.FC = () => {
    const styles = useStyles();
    const { data: services, isLoading, error } = useHealthServices();
    const createMutation = useCreateHealthService();
    const updateMutation = useUpdateHealthService();
    const deleteMutation = useDeleteHealthService();

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [form, setForm] = useState<CreateHealthServiceRequest>(emptyForm);
    const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

    const openCreate = () => {
        setEditingId(null);
        setForm(emptyForm);
        setDialogOpen(true);
    };

    const openEdit = (svc: HealthServiceConfig) => {
        setEditingId(svc.id);
        setForm({
            serviceId: svc.serviceId,
            displayName: svc.displayName,
            healthUrl: svc.healthUrl,
            enabled: svc.enabled,
            sortOrder: svc.sortOrder,
        });
        setDialogOpen(true);
    };

    const handleSave = () => {
        if (editingId) {
            updateMutation.mutate({ id: editingId, request: form }, {
                onSuccess: () => setDialogOpen(false),
            });
        } else {
            createMutation.mutate(form, {
                onSuccess: () => setDialogOpen(false),
            });
        }
    };

    const handleDelete = (id: string) => {
        deleteMutation.mutate(id, {
            onSuccess: () => setDeleteConfirmId(null),
        });
    };

    const handleToggleEnabled = (svc: HealthServiceConfig) => {
        updateMutation.mutate({
            id: svc.id,
            request: {
                serviceId: svc.serviceId,
                displayName: svc.displayName,
                healthUrl: svc.healthUrl,
                enabled: !svc.enabled,
                sortOrder: svc.sortOrder,
            },
        });
    };

    if (isLoading) {
        return <Spinner label="Loading services..." />;
    }

    if (error) {
        return <Body1>Error loading health services configuration.</Body1>;
    }

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Body1>Manage monitored services. Changes take effect immediately.</Body1>
                <Button appearance="primary" icon={<Add24Regular />} onClick={openCreate}>
                    Add Service
                </Button>
            </div>

            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHeaderCell>Order</TableHeaderCell>
                        <TableHeaderCell>Service ID</TableHeaderCell>
                        <TableHeaderCell>Display Name</TableHeaderCell>
                        <TableHeaderCell>Health URL</TableHeaderCell>
                        <TableHeaderCell>Enabled</TableHeaderCell>
                        <TableHeaderCell>Actions</TableHeaderCell>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {(services ?? []).map((svc) => (
                        <TableRow key={svc.id}>
                            <TableCell>
                                <TableCellLayout>{svc.sortOrder}</TableCellLayout>
                            </TableCell>
                            <TableCell>
                                <TableCellLayout>{svc.serviceId}</TableCellLayout>
                            </TableCell>
                            <TableCell>
                                <TableCellLayout>{svc.displayName}</TableCellLayout>
                            </TableCell>
                            <TableCell>
                                <TableCellLayout>
                                    <code style={{ fontSize: tokens.fontSizeBase200 }}>{svc.healthUrl}</code>
                                </TableCellLayout>
                            </TableCell>
                            <TableCell>
                                <Switch
                                    checked={svc.enabled}
                                    onChange={() => handleToggleEnabled(svc)}
                                />
                            </TableCell>
                            <TableCell>
                                <div className={styles.actions}>
                                    <Button
                                        appearance="subtle"
                                        icon={<Edit24Regular />}
                                        onClick={() => openEdit(svc)}
                                        size="small"
                                    />
                                    <Button
                                        appearance="subtle"
                                        icon={<Delete24Regular />}
                                        onClick={() => setDeleteConfirmId(svc.id)}
                                        size="small"
                                    />
                                </div>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>

            {/* Create/Edit Dialog */}
            <Dialog open={dialogOpen} onOpenChange={(_, data) => setDialogOpen(data.open)}>
                <DialogSurface>
                    <DialogTitle>{editingId ? 'Edit Service' : 'Add Service'}</DialogTitle>
                    <DialogBody>
                        <DialogContent>
                            <div className={styles.form}>
                                <div>
                                    <Label htmlFor="serviceId">Service ID</Label>
                                    <Input
                                        id="serviceId"
                                        value={form.serviceId}
                                        onChange={(_, data) => setForm({ ...form, serviceId: data.value })}
                                        placeholder="e.g. engine-core"
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="displayName">Display Name</Label>
                                    <Input
                                        id="displayName"
                                        value={form.displayName}
                                        onChange={(_, data) => setForm({ ...form, displayName: data.value })}
                                        placeholder="e.g. Engine Core (Auth/Admin)"
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="healthUrl">Health URL</Label>
                                    <Input
                                        id="healthUrl"
                                        value={form.healthUrl}
                                        onChange={(_, data) => setForm({ ...form, healthUrl: data.value })}
                                        placeholder="e.g. http://engine-core:8081/actuator/health"
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="sortOrder">Sort Order</Label>
                                    <Input
                                        id="sortOrder"
                                        type="number"
                                        value={String(form.sortOrder)}
                                        onChange={(_, data) => setForm({ ...form, sortOrder: Number(data.value) || 0 })}
                                    />
                                </div>
                                <Switch
                                    label="Enabled"
                                    checked={form.enabled}
                                    onChange={(_, data) => setForm({ ...form, enabled: data.checked })}
                                />
                            </div>
                        </DialogContent>
                    </DialogBody>
                    <DialogActions>
                        <Button appearance="secondary" onClick={() => setDialogOpen(false)}>Cancel</Button>
                        <Button
                            appearance="primary"
                            onClick={handleSave}
                            disabled={!form.serviceId || !form.displayName || !form.healthUrl}
                        >
                            {editingId ? 'Save' : 'Create'}
                        </Button>
                    </DialogActions>
                </DialogSurface>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <Dialog open={!!deleteConfirmId} onOpenChange={() => setDeleteConfirmId(null)}>
                <DialogSurface>
                    <DialogTitle>Delete Service</DialogTitle>
                    <DialogBody>
                        <DialogContent>
                            Are you sure you want to remove this service from health monitoring?
                        </DialogContent>
                    </DialogBody>
                    <DialogActions>
                        <Button appearance="secondary" onClick={() => setDeleteConfirmId(null)}>Cancel</Button>
                        <Button
                            appearance="primary"
                            onClick={() => deleteConfirmId && handleDelete(deleteConfirmId)}
                        >
                            Delete
                        </Button>
                    </DialogActions>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default HealthServicesSettingsPanel;
