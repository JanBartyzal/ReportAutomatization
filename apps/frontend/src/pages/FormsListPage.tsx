import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle2,
    Body1,
    Caption1,
    Button,
    Tab,
    TabList,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Badge,
    Input,
    Field,
    // Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    AddRegular,
    EditRegular,
    EyeRegular,
} from '@fluentui/react-icons';
import { useForms } from '../hooks/useForms';
import { useLocalScope } from '../hooks/useFeatureFlags';
import { ScopeBadge } from '../components/ScopeBadge';
import { FormStatus } from '@reportplatform/types';
import LoadingSpinner from '../components/LoadingSpinner';

const statusColors: Record<FormStatus, 'informative' | 'success' | 'danger' | 'warning'> = {
    DRAFT: 'informative',
    PUBLISHED: 'success',
    CLOSED: 'danger',
};

// Styles
const useStyles = makeStyles({
    scopeFilter: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginBottom: tokens.spacingVerticalM,
    },
    filterButton: {
        borderRadius: tokens.borderRadiusCircular,
    },
});

export const FormsListPage: React.FC = () => {
    const navigate = useNavigate();
    const styles = useStyles();
    const enableLocalScope = useLocalScope();

    const [statusFilter, setStatusFilter] = useState<FormStatus | 'ALL'>('ALL');
    const [scopeFilter, setScopeFilter] = useState<'CENTRAL' | 'LOCAL' | 'SHARED' | 'ALL'>('ALL');

    const { data: formsData, isLoading } = useForms({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        scope: scopeFilter === 'ALL' ? undefined : scopeFilter,
    });

    /*
    const handleDelete = async (formId: string) => {
        if (confirm('Are you sure you want to delete this form?')) {
            await deleteForm.mutateAsync(formId);
        }
    };
    */

    if (isLoading) {
        return <LoadingSpinner />;
    }

    return (
        <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <Title3 block>Forms</Title3>
                    <Subtitle2 block style={{ color: 'var(--colorNeutralForeground2)' }}>
                        Create and manage data collection forms
                    </Subtitle2>
                </div>
                <Button
                    appearance="primary"
                    icon={<AddRegular />}
                    onClick={() => navigate('/forms/new')}
                >
                    Create Form
                </Button>
            </div>

            <TabList
                selectedValue={statusFilter}
                onTabSelect={(_, data) => setStatusFilter(data.value as FormStatus | 'ALL')}
                style={{ marginBottom: '16px' }}
            >
                <Tab value="ALL">All</Tab>
                <Tab value="DRAFT">Draft</Tab>
                <Tab value="PUBLISHED">Published</Tab>
                <Tab value="CLOSED">Closed</Tab>
            </TabList>

            {/* Scope Filter (only when local scope is enabled) */}
            {enableLocalScope && (
                <div className={styles.scopeFilter}>
                    <Body1 style={{ fontWeight: tokens.fontWeightSemibold, marginRight: tokens.spacingHorizontalS }}>
                        Scope:
                    </Body1>
                    <Button
                        appearance={scopeFilter === 'ALL' ? 'primary' : 'subtle'}
                        size="small"
                        className={styles.filterButton}
                        onClick={() => setScopeFilter('ALL')}
                    >
                        All
                    </Button>
                    <Button
                        appearance={scopeFilter === 'CENTRAL' ? 'primary' : 'subtle'}
                        size="small"
                        className={styles.filterButton}
                        onClick={() => setScopeFilter('CENTRAL')}
                    >
                        Central
                    </Button>
                    <Button
                        appearance={scopeFilter === 'LOCAL' ? 'primary' : 'subtle'}
                        size="small"
                        className={styles.filterButton}
                        onClick={() => setScopeFilter('LOCAL')}
                    >
                        Local
                    </Button>
                    <Button
                        appearance={scopeFilter === 'SHARED' ? 'primary' : 'subtle'}
                        size="small"
                        className={styles.filterButton}
                        onClick={() => setScopeFilter('SHARED')}
                    >
                        Shared
                    </Button>
                </div>
            )}

            <Field style={{ marginBottom: '16px' }}>
                <Input
                    placeholder="Search forms..."
                    style={{ maxWidth: '300px' }}
                />
            </Field>

            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHeaderCell>Form Name</TableHeaderCell>
                        {enableLocalScope && <TableHeaderCell>Scope</TableHeaderCell>}
                        <TableHeaderCell>Status</TableHeaderCell>
                        <TableHeaderCell>Created</TableHeaderCell>
                        <TableHeaderCell>Actions</TableHeaderCell>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {formsData?.data?.map((form) => (
                        <TableRow key={form.id}>
                            <TableCell>
                                <Body1><strong>{form.name}</strong></Body1>
                                <Caption1>{form.description}</Caption1>
                            </TableCell>
                            {enableLocalScope && (
                                <TableCell>
                                    <ScopeBadge scope={(form.scope as 'CENTRAL' | 'LOCAL' | 'SHARED') || 'CENTRAL'} />
                                </TableCell>
                            )}
                            <TableCell>
                                <Badge appearance="filled" color={statusColors[form.status as FormStatus] || 'informative'}>
                                    {form.status}
                                </Badge>
                            </TableCell>
                            <TableCell>
                                <Caption1>{form.created_at ? new Date(form.created_at).toLocaleDateString() : 'N/A'}</Caption1>
                            </TableCell>
                            <TableCell>
                                <Button
                                    appearance="transparent"
                                    icon={<EyeRegular />}
                                    size="small"
                                    onClick={() => navigate(`/forms/${form.id}`)}
                                >
                                    View
                                </Button>
                                {form.status !== 'CLOSED' && (
                                    <Button
                                        appearance="transparent"
                                        icon={<EditRegular />}
                                        size="small"
                                        onClick={() => navigate(`/forms/${form.id}/edit`)}
                                    >
                                        Edit
                                    </Button>
                                )}
                            </TableCell>
                        </TableRow>
                    ))}
                    {(!formsData?.data || formsData.data.length === 0) && (
                        <TableRow>
                            <TableCell colSpan={enableLocalScope ? 7 : 6} style={{ textAlign: 'center', padding: '32px' }}>
                                <Body1>No forms found. Create your first form to get started.</Body1>
                            </TableCell>
                        </TableRow>
                    )}
                </TableBody>
            </Table>
        </div>
    );
};

export default FormsListPage;
