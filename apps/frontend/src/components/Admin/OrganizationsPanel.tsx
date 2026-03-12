import React, { useState } from 'react';
import {
    Body1,
    Button,
    Spinner,
    Tree,
    TreeItem,
    TreeItemLayout,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Input,
    Label,
    Select,
    Option
} from '@fluentui/react-components';
import { useOrganizations, useCreateOrganization, useDeleteOrganization } from '../../hooks/useAdmin';
import type { OrganizationAdmin } from '@reportplatform/types';
import styles from './OrganizationsPanel.module.css';

const OrganizationsPanel: React.FC = () => {
    const { data: organizations, isLoading, error } = useOrganizations();
    const createOrg = useCreateOrganization();
    const deleteOrg = useDeleteOrganization();

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [newOrgName, setNewOrgName] = useState('');
    const [newOrgType, setNewOrgType] = useState<string>('COMPANY');

    if (isLoading) {
        return <Spinner label="Loading organizations..." />;
    }

    if (error) {
        return <Body1 className={styles.error}>Error loading organizations: {error.message}</Body1>;
    }

    const handleCreateOrg = async () => {
        if (!newOrgName.trim()) return;

        await createOrg.mutateAsync({
            name: newOrgName,
            type: newOrgType
        });

        setNewOrgName('');
        setNewOrgType('COMPANY');
        setIsDialogOpen(false);
    };

    const handleDeleteOrg = async (orgId: string) => {
        if (confirm('Are you sure you want to delete this organization?')) {
            await deleteOrg.mutateAsync(orgId);
        }
    };

    const renderTreeItems = (orgs: OrganizationAdmin[]): React.ReactNode => {
        return orgs.map((org) => (
            <TreeItem
                key={org.id}
                value={org.id}
                itemType={org.type === 'HOLDING' ? 'branch' : 'leaf'}
            >
                <TreeItemLayout
                    aside={
                        <Button
                            size="small"
                            appearance="subtle"
                            onClick={() => handleDeleteOrg(org.id)}
                        >
                            Delete
                        </Button>
                    }
                >
                    <span className={styles.orgItem}>
                        <span className={styles.orgName}>{org.name}</span>
                        <span className={styles.orgType}>{org.type}</span>
                    </span>
                </TreeItemLayout>
                {org.children && org.children.length > 0 && (
                    <Tree>
                        {renderTreeItems(org.children)}
                    </Tree>
                )}
            </TreeItem>
        ));
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Body1>Organization Hierarchy</Body1>
                <Dialog open={isDialogOpen} onOpenChange={(_: any, d: any) => setIsDialogOpen(d.open)}>
                    <DialogTrigger disableButtonEnhancement>
                        <Button appearance="primary">Add Organization</Button>
                    </DialogTrigger>
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
                                    <Select
                                        value={newOrgType}
                                        onChange={(_e: any, data: any) => setNewOrgType(data.value)}
                                    >
                                        <Option value="HOLDING">Holding</Option>
                                        <Option value="COMPANY">Company</Option>
                                        <Option value="DIVISION">Division</Option>
                                    </Select>
                                </div>
                            </DialogContent>
                            <DialogActions>
                                <Button appearance="secondary" onClick={() => setIsDialogOpen(false)}>
                                    Cancel
                                </Button>
                                <Button appearance="primary" onClick={handleCreateOrg}>
                                    Create
                                </Button>
                            </DialogActions>
                        </DialogBody>
                    </DialogSurface>
                </Dialog>
            </div>

            <div className={styles.tree}>
                {organizations && organizations.length > 0 ? (
                    <Tree>
                        {renderTreeItems(organizations)}
                    </Tree>
                ) : (
                    <Body1>No organizations found</Body1>
                )}
            </div>
        </div>
    );
};

export default OrganizationsPanel;
