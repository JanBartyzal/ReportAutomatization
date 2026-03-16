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
    Select,
    Option,
    Badge,
    makeStyles,
    tokens,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
} from '@fluentui/react-components';
import { Add24Regular, Dismiss24Regular } from '@fluentui/react-icons';
import { useUsers, useAssignRole, useRemoveRole, useOrganizations } from '../../hooks/useAdmin';
import { Role } from '@reportplatform/types';

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
    roleList: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: '4px',
        alignItems: 'center',
    },
    roleChip: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
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
});

const ROLES = [
    Role.HOLDING_ADMIN,
    Role.ADMIN,
    Role.COMPANY_ADMIN,
    Role.EDITOR,
    Role.VIEWER,
];

const roleBadgeColor = (role: string) => {
    switch (role) {
        case 'HOLDING_ADMIN': return 'danger' as const;
        case 'ADMIN': return 'warning' as const;
        case 'COMPANY_ADMIN': return 'brand' as const;
        case 'EDITOR': return 'success' as const;
        case 'VIEWER': return 'informative' as const;
        default: return 'subtle' as const;
    }
};

const UsersPanel: React.FC = () => {
    const [filterOrgId, setFilterOrgId] = useState<string>('');
    const [isAssignOpen, setIsAssignOpen] = useState(false);
    const [assignUserId, setAssignUserId] = useState('');
    const [assignOrgId, setAssignOrgId] = useState('');
    const [assignRole, setAssignRole] = useState<string>(Role.VIEWER);

    const styles = useStyles();

    const { data: usersResponse, isLoading, error } = useUsers(
        filterOrgId ? { org_id: filterOrgId } : {}
    );
    const { data: organizations } = useOrganizations();
    const assignRoleMutation = useAssignRole();
    const removeRoleMutation = useRemoveRole();

    if (isLoading) {
        return <Spinner label="Loading users..." />;
    }

    if (error) {
        return <Body1 className={styles.error}>Error loading users: {error.message}</Body1>;
    }

    const users = usersResponse?.data || [];

    const handleAssignRole = async () => {
        if (!assignUserId.trim() || !assignOrgId || !assignRole) return;

        await assignRoleMutation.mutateAsync({
            userId: assignUserId,
            role: assignRole as Role,
            orgId: assignOrgId,
        });

        setAssignUserId('');
        setAssignOrgId('');
        setAssignRole(Role.VIEWER);
        setIsAssignOpen(false);
    };

    const handleRemoveRole = async (userId: string, role: string, orgId: string) => {
        if (confirm(`Remove role ${role} from user ${userId}?`)) {
            await removeRoleMutation.mutateAsync({
                userId,
                role: role as Role,
                orgId,
            });
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Body1>User Role Management</Body1>
                <Button
                    appearance="primary"
                    icon={<Add24Regular />}
                    onClick={() => setIsAssignOpen(true)}
                >
                    Assign Role
                </Button>
            </div>

            <div className={styles.filterRow}>
                <div>
                    <Label>Filter by Organization</Label>
                    <Select
                        value={filterOrgId}
                        onChange={(_e: any, data: any) => setFilterOrgId(data.value)}
                    >
                        <Option value="">All Organizations</Option>
                        {(organizations || []).map((org: any) => (
                            <Option key={org.id} value={org.id}>
                                {org.name}
                            </Option>
                        ))}
                    </Select>
                </div>
            </div>

            {users.length === 0 ? (
                <div className={styles.emptyState}>
                    <Body1>No users found. Assign a role to add users.</Body1>
                </div>
            ) : (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>User ID</TableHeaderCell>
                            <TableHeaderCell>Roles</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {users.map((user: any) => (
                            <TableRow key={user.user_id}>
                                <TableCell>{user.display_name || user.user_id}</TableCell>
                                <TableCell>
                                    <div className={styles.roleList}>
                                        {(user.roles || []).map((r: any, idx: number) => (
                                            <span key={idx} className={styles.roleChip}>
                                                <Badge
                                                    color={roleBadgeColor(r.role)}
                                                    appearance="filled"
                                                    size="small"
                                                >
                                                    {r.role} @ {r.org_name}
                                                </Badge>
                                                <Button
                                                    size="small"
                                                    appearance="subtle"
                                                    icon={<Dismiss24Regular />}
                                                    onClick={() => handleRemoveRole(user.user_id, r.role, r.org_id)}
                                                    title="Remove role"
                                                />
                                            </span>
                                        ))}
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}

            <Dialog open={isAssignOpen} onOpenChange={(_: any, d: any) => setIsAssignOpen(d.open)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Assign Role to User</DialogTitle>
                        <DialogContent>
                            <div className={styles.form}>
                                <Label required>User ID (Azure AD OID or email)</Label>
                                <Input
                                    value={assignUserId}
                                    onChange={(_e: any, data: any) => setAssignUserId(data.value)}
                                    placeholder="e.g. user@company.com or Azure AD Object ID"
                                />
                                <Label required>Organization</Label>
                                <Select
                                    value={assignOrgId}
                                    onChange={(_e: any, data: any) => setAssignOrgId(data.value)}
                                >
                                    <Option value="">Select organization...</Option>
                                    {(organizations || []).map((org: any) => (
                                        <Option key={org.id} value={org.id} text={`${org.name} (${org.type})`}>
                                            {org.name} ({org.type})
                                        </Option>
                                    ))}
                                </Select>
                                <Label required>Role</Label>
                                <Select
                                    value={assignRole}
                                    onChange={(_e: any, data: any) => setAssignRole(data.value)}
                                >
                                    {ROLES.map((role) => (
                                        <Option key={role} value={role}>
                                            {role}
                                        </Option>
                                    ))}
                                </Select>
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <Button appearance="secondary" onClick={() => setIsAssignOpen(false)}>
                                Cancel
                            </Button>
                            <Button
                                appearance="primary"
                                onClick={handleAssignRole}
                                disabled={assignRoleMutation.isPending || !assignUserId || !assignOrgId}
                            >
                                {assignRoleMutation.isPending ? 'Assigning...' : 'Assign'}
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default UsersPanel;
