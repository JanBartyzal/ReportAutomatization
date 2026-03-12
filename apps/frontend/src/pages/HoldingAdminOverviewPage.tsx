/**
 * HoldingAdminOverviewPage
 * Overview of all local/shared items across the entire holding
 */

import React, { useState } from 'react';
import {
    Page,
    Title3,
    Subtitle1,
    Body1,
    Caption1,
    Button,
    Card,
    CardHeader,
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
    Spinner,
} from '@fluentui/react-components';
import {
    ArrowDownload24Regular,
    Organization24Regular,
    Filter24Regular,
} from '@fluentui/react-icons';
import { ScopeBadge } from '../components/ScopeBadge';
import { reportBrand } from '../theme/brandTokens';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '1200px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalL,
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
        boxShadow: tokens.shadowLevel1,
        overflow: 'hidden',
    }
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

    // Mock data for holding items
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
        <Page>
            <div className={styles.container}>
                <div className={styles.header}>
                    <div>
                        <Title3 style={{ color: reportBrand[90] }}>Holding Administration</Title3>
                        <Subtitle1>Overview of all local and shared items across holding</Subtitle1>
                    </div>
                    <Button 
                        appearance="primary" 
                        icon={<ArrowDownload24Regular />}
                        style={{ backgroundColor: reportBrand[90] }}
                        onClick={handlePullData}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Pulling...' : 'Pull Released Data'}
                    </Button>
                </div>

                <div className={styles.filterSection}>
                    <TabList 
                        selectedValue={activeTab} 
                        onTabSelect={(_, data) => setActiveTab(data.value as any)}
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
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <strong>{item.name}</strong>
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
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
                                        <Button appearance="subtle" size="small">Import</Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
                
                {filteredItems.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '48px' }}>
                        <Body1>No items found for this filter.</Body1>
                    </div>
                )}
            </div>
        </Page>
    );
};

export default HoldingAdminOverviewPage;
