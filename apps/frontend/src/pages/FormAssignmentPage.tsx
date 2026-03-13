import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle2,
    Body1,
    Button,
    Tree,
    TreeItem,
    TreeItemLayout,
    Checkbox,
    Card,
    makeStyles,
    shorthands,
    tokens,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    SaveRegular,
} from '@fluentui/react-icons';
import { useForm, useFormAssignments, useAssignOrganizations } from '../hooks/useForms';
import { useOrganizations } from '../hooks/useAdmin';
import type { OrganizationAdmin } from '@reportplatform/types';
import LoadingSpinner from '../components/LoadingSpinner';

const useStyles = makeStyles({
    container: {
        padding: '24px',
        maxWidth: '1000px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '24px',
    },
    treeContainer: {
        ...shorthands.padding('16px'),
        backgroundColor: tokens.colorNeutralBackground1,
        ...shorthands.borderRadius(tokens.borderRadiusMedium),
        minHeight: '400px',
    },
    orgItem: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    }
});

export const FormAssignmentPage: React.FC = () => {
    const styles = useStyles();
    const { formId } = useParams<{ formId: string }>();
    const navigate = useNavigate();

    const { data: form, isLoading: formLoading } = useForm(formId!);
    const { data: assignments, isLoading: assignmentsLoading } = useFormAssignments(formId!);
    const { data: organizations, isLoading: orgsLoading } = useOrganizations();
    const assignOrgs = useAssignOrganizations(formId!);

    const [selectedOrgIds, setSelectedOrgIds] = useState<Set<string>>(new Set());

    useEffect(() => {
        if (assignments?.org_ids) {
            setSelectedOrgIds(new Set(assignments.org_ids));
        }
    }, [assignments]);

    const handleToggleOrg = (orgId: string, checked: boolean) => {
        const newSelected = new Set(selectedOrgIds);
        if (checked) {
            newSelected.add(orgId);
        } else {
            newSelected.delete(orgId);
        }
        setSelectedOrgIds(newSelected);
    };

    const handleSave = async () => {
        await assignOrgs.mutateAsync(Array.from(selectedOrgIds));
        navigate('/forms');
    };

    if (formLoading || assignmentsLoading || orgsLoading) return <LoadingSpinner />;

    const renderTreeItems = (orgs: OrganizationAdmin[]): React.ReactNode => {
        return orgs.map((org) => (
            <TreeItem
                key={org.id}
                value={org.id}
                itemType={org.children && org.children.length > 0 ? 'branch' : 'leaf'}
            >
                <TreeItemLayout>
                    <div className={styles.orgItem}>
                        <Checkbox 
                            checked={selectedOrgIds.has(org.id)}
                            onChange={(_, d) => handleToggleOrg(org.id, !!d.checked)}
                            onClick={(e) => e.stopPropagation()}
                        />
                        <Body1>{org.name}</Body1>
                        <Subtitle2 style={{ color: tokens.colorNeutralForeground4 }}>({org.type})</Subtitle2>
                    </div>
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
                <div>
                    <Button
                        appearance="transparent"
                        icon={<ArrowLeftRegular />}
                        onClick={() => navigate('/forms')}
                        style={{ marginBottom: '8px' }}
                    >
                        Back to List
                    </Button>
                    <Title3>Form Assignment: {form?.name}</Title3>
                    <Subtitle2 style={{ color: tokens.colorNeutralForeground2 }}>
                        Select organizations that should fill out this form
                    </Subtitle2>
                </div>
                <Button 
                    appearance="primary" 
                    icon={<SaveRegular />} 
                    onClick={handleSave}
                    disabled={assignOrgs.isPending}
                >
                    Save Assignments
                </Button>
            </div>

            <Card className={styles.treeContainer}>
                {organizations && organizations.length > 0 ? (
                    <Tree aria-label="Organization Hierarchy">
                        {renderTreeItems(organizations)}
                    </Tree>
                ) : (
                    <Body1>No organizations found in the system.</Body1>
                )}
            </Card>
        </div>
    );
};

export default FormAssignmentPage;
