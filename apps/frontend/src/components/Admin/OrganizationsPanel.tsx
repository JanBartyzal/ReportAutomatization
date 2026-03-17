import React, { useState, useMemo } from 'react';
import {
    Body1,
    Button,
    Spinner,
    Tree,
    TreeItem,
    TreeItemLayout,
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Input,
    Label,
    Dropdown,
    Option,
    Badge,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Add24Regular, Delete24Regular } from '@fluentui/react-icons';
import { useOrganizations, useCreateOrganization, useDeleteOrganization } from '../../hooks/useAdmin';
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
    tree: {
        marginTop: '8px',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        marginTop: '8px',
    },
    orgItem: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    orgName: {
        fontWeight: tokens.fontWeightSemibold,
    },
    error: {
        color: tokens.colorPaletteRedForeground1,
    },
});

function flattenOrgs(orgs: OrganizationAdmin[]): OrganizationAdmin[] {
    const result: OrganizationAdmin[] = [];
    const recurse = (list: OrganizationAdmin[]) => {
        for (const org of list) {
            result.push(org);
            if (org.children && org.children.length > 0) {
                recurse(org.children);
            }
        }
    };
    recurse(orgs);
    return result;
}

const ORG_TYPES = [
    { value: 'HOLDING', label: 'Holding' },
    { value: 'COMPANY', label: 'Company' },
    { value: 'DIVISION', label: 'Division' },
];

const OrganizationsPanel: React.FC = () => {
    const { data: organizations, isLoading, error } = useOrganizations();
    const createOrg = useCreateOrganization();
    const deleteOrg = useDeleteOrganization();

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [newOrgName, setNewOrgName] = useState('');
    const [newOrgType, setNewOrgType] = useState<string>('COMPANY');
    const [newOrgParentId, setNewOrgParentId] = useState<string>('');

    const styles = useStyles();

    const flatOrgs = useMemo(() => flattenOrgs(organizations || []), [organizations]);

    if (isLoading) {
        return <Spinner label="Loading organizations..." />;
    }

    if (error) {
        return <Body1 className={styles.error}>Error loading organizations: {error.message}</Body1>;
    }

    const openCreateDialog = (parentId?: string) => {
        setNewOrgName('');
        setNewOrgType(parentId ? 'COMPANY' : 'HOLDING');
        setNewOrgParentId(parentId || '');
        setIsDialogOpen(true);
    };

    const handleCreateOrg = async () => {
        if (!newOrgName.trim()) return;

        await createOrg.mutateAsync({
            name: newOrgName,
            type: newOrgType,
            ...(newOrgParentId ? { parent_id: newOrgParentId } : {}),
        });

        setNewOrgName('');
        setNewOrgType('COMPANY');
        setNewOrgParentId('');
        setIsDialogOpen(false);
    };

    const handleDeleteOrg = async (orgId: string) => {
        if (confirm('Are you sure you want to delete this organization?')) {
            await deleteOrg.mutateAsync(orgId);
        }
    };

    const typeBadgeColor = (type: string) => {
        switch (type) {
            case 'HOLDING': return 'brand' as const;
            case 'COMPANY': return 'success' as const;
            case 'DIVISION': return 'informative' as const;
            default: return 'subtle' as const;
        }
    };

    const renderTreeItems = (orgs: OrganizationAdmin[]): React.ReactNode => {
        return orgs.map((org) => {
            const hasChildren = org.children && org.children.length > 0;
            const canHaveChildren = org.type === 'HOLDING' || org.type === 'COMPANY';

            return (
                <TreeItem
                    key={org.id}
                    value={org.id}
                    itemType={hasChildren || canHaveChildren ? 'branch' : 'leaf'}
                >
                    <TreeItemLayout
                        aside={
                            <span style={{ display: 'flex', gap: '4px' }}>
                                {canHaveChildren && (
                                    <Button
                                        size="small"
                                        appearance="subtle"
                                        icon={<Add24Regular />}
                                        onClick={() => openCreateDialog(org.id)}
                                        title="Add sub-organization"
                                    />
                                )}
                                <Button
                                    size="small"
                                    appearance="subtle"
                                    icon={<Delete24Regular />}
                                    onClick={() => handleDeleteOrg(org.id)}
                                    title="Delete"
                                />
                            </span>
                        }
                    >
                        <span className={styles.orgItem}>
                            <span className={styles.orgName}>{org.name}</span>
                            <Badge color={typeBadgeColor(org.type)} appearance="filled" size="small">
                                {org.type}
                            </Badge>
                        </span>
                    </TreeItemLayout>
                    {hasChildren && (
                        <Tree>
                            {renderTreeItems(org.children!)}
                        </Tree>
                    )}
                </TreeItem>
            );
        });
    };

    const typeDisplayText = ORG_TYPES.find(t => t.value === newOrgType)?.label ?? 'Company';
    const parentDisplayText = newOrgParentId
        ? flatOrgs.find(o => o.id === newOrgParentId)?.name ?? '— No parent (top-level) —'
        : '— No parent (top-level) —';

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Body1>Organization Hierarchy</Body1>
                <Button appearance="primary" icon={<Add24Regular />} onClick={() => openCreateDialog()}>
                    Add Organization
                </Button>
            </div>

            <Dialog open={isDialogOpen} onOpenChange={(_: any, d: any) => setIsDialogOpen(d.open)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Create Organization</DialogTitle>
                        <DialogContent>
                            <div className={styles.form}>
                                <Label required>Organization Name</Label>
                                <Input
                                    value={newOrgName}
                                    onChange={(_e: any, data: any) => setNewOrgName(data.value)}
                                    placeholder="Enter organization name"
                                />
                                <Label required>Type</Label>
                                <Dropdown
                                    value={typeDisplayText}
                                    selectedOptions={[newOrgType]}
                                    onOptionSelect={(_e: any, data: any) => setNewOrgType(data.optionValue ?? 'COMPANY')}
                                    listbox={{ style: { zIndex: 10000000 } }}
                                >
                                    {ORG_TYPES.map((t) => (
                                        <Option key={t.value} value={t.value}>
                                            {t.label}
                                        </Option>
                                    ))}
                                </Dropdown>
                                <Label>Parent Organization</Label>
                                <Dropdown
                                    value={parentDisplayText}
                                    selectedOptions={[newOrgParentId]}
                                    onOptionSelect={(_e: any, data: any) => setNewOrgParentId(data.optionValue ?? '')}
                                    listbox={{ style: { zIndex: 10000000 } }}
                                >
                                    <Option value="">— No parent (top-level) —</Option>
                                    {flatOrgs.map((org) => (
                                        <Option key={org.id} value={org.id} text={`${org.name} (${org.type})`}>
                                            {`${org.name} (${org.type})`}
                                        </Option>
                                    ))}
                                </Dropdown>
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <Button appearance="secondary" onClick={() => setIsDialogOpen(false)}>
                                Cancel
                            </Button>
                            <Button
                                appearance="primary"
                                onClick={handleCreateOrg}
                                disabled={createOrg.isPending}
                            >
                                {createOrg.isPending ? 'Creating...' : 'Create'}
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>

            <div className={styles.tree}>
                {organizations && organizations.length > 0 ? (
                    <Tree>
                        {renderTreeItems(organizations)}
                    </Tree>
                ) : (
                    <Body1>No organizations found. Create a Holding organization to get started.</Body1>
                )}
            </div>
        </div>
    );
};

export default OrganizationsPanel;
