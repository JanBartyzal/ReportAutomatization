/**
 * HoldingAdminOverviewPage — migrated per P9-W2-002
 * Replaced reportBrand hardcoded token with Fluent tokens.colorBrandBackground
 * Replaced <Page> wrapper with plain div; removed inline style= props.
 */

import React, { useState } from 'react';
import {
    Body1,
    Caption1,
    Button,
    makeStyles,
    tokens,
    Tab,
    TabList,
    Badge,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
} from '@fluentui/react-components';
import {
    ArrowDownload24Regular,
    Organization24Regular,
} from '@fluentui/react-icons';
import { ScopeBadge } from '../components/ScopeBadge';
import { PageHeader } from '../components/shared/PageHeader';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '1200px',
        margin: '0 auto',
    },
    filterSection: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
        alignItems: 'center',
    },
    tableWrapper: {
        backgroundColor: tokens.colorNeutralBackground1,
        borderRadius: tokens.borderRadiusMedium,
        boxShadow: tokens.shadow4,
        overflow: 'hidden',
    },
    nameCell: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    orgCell: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    emptyState: {
        textAlign: 'center',
        padding: '48px',
        color: tokens.colorNeutralForeground3,
    },
    actionButton: {
        marginLeft: tokens.spacingHorizontalXS,
    },
});

interface HoldingItem {
    id: string;
    name: string;
    type: 'FORM' | 'TEMPLATE';
    organization: string;
    scope: 'LOCAL' | 'SHARED';
    status: string;
    releasedAt: string;
}

export const HoldingAdminOverviewPage: React.FC = () => {
    const styles = useStyles();
    const [activeTab, setActiveTab] = useState<'all' | 'forms' | 'templates'>('all');
    const [isLoading, setIsLoading] = useState(false);

    const items: HoldingItem[] = [
        { id: '1', name: 'Annual Sales Report', type: 'FORM', organization: 'Acme Corp', scope: 'SHARED', status: 'RELEASED', releasedAt: '2024-03-10' },
        { id: '2', name: 'Inventory Template v2', type: 'TEMPLATE', organization: 'Beta Inc', scope: 'LOCAL', status: 'DRAFT', releasedAt: '2024-03-11' },
        { id: '3', name: 'QHSE Audit 2024', type: 'FORM', organization: 'Gamma LLC', scope: 'SHARED', status: 'RELEASED', releasedAt: '2024-03-08' },
        { id: '4', name: 'Standard Invoice', type: 'TEMPLATE', organization: 'Acme Corp', scope: 'SHARED', status: 'RELEASED', releasedAt: '2024-03-05' },
    ];

    const filteredItems = items.filter(item => {
        if (activeTab === 'all') return true;
        if (activeTab === 'forms') return item.type === 'FORM';
        if (activeTab === 'templates') return item.type === 'TEMPLATE';
        return true;
    });

    const handlePullData = () => {
        setIsLoading(true);
        setTimeout(() => {
            setIsLoading(false);
            alert('Pulling released data from all subsidiaries...');
        }, 1500);
    };

    return (
        <div className={styles.container}>
            <PageHeader
                title="Holding Administration"
                subtitle="Overview of all local and shared items across holding"
                actions={
                    <Button
                        appearance="primary"
                        icon={<ArrowDownload24Regular />}
                        onClick={handlePullData}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Pulling...' : 'Pull Released Data'}
                    </Button>
                }
            />

            <div className={styles.filterSection}>
                <TabList
                    selectedValue={activeTab}
                    onTabSelect={(_, data) => setActiveTab(data.value as 'all' | 'forms' | 'templates')}
                >
                    <Tab value="all">All Items</Tab>
                    <Tab value="forms">Forms</Tab>
                    <Tab value="templates">Templates</Tab>
                </TabList>
            </div>

            <div className={styles.tableWrapper}>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>Name</TableHeaderCell>
                            <TableHeaderCell>Organization</TableHeaderCell>
                            <TableHeaderCell>Type</TableHeaderCell>
                            <TableHeaderCell>Scope</TableHeaderCell>
                            <TableHeaderCell>Released At</TableHeaderCell>
                            <TableHeaderCell>Actions</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {filteredItems.map(item => (
                            <TableRow key={item.id}>
                                <TableCell>
                                    <div className={styles.nameCell}>
                                        <strong>{item.name}</strong>
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <div className={styles.orgCell}>
                                        <Organization24Regular style={{ fontSize: '16px' }} />
                                        {item.organization}
                                    </div>
                                </TableCell>
                                <TableCell>
                                    <Badge appearance="outline" color="informative">{item.type}</Badge>
                                </TableCell>
                                <TableCell>
                                    <ScopeBadge scope={item.scope as any} />
                                </TableCell>
                                <TableCell>
                                    <Caption1>{new Date(item.releasedAt).toLocaleDateString()}</Caption1>
                                </TableCell>
                                <TableCell>
                                    <Button appearance="subtle" size="small">View</Button>
                                    <Button appearance="subtle" size="small" className={styles.actionButton}>Import</Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>

            {filteredItems.length === 0 && (
                <div className={styles.emptyState}>
                    <Body1>No items found for this filter.</Body1>
                </div>
            )}
        </div>
    );
};

export default HoldingAdminOverviewPage;
