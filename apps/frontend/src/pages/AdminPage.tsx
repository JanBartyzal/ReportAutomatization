/**
 * AdminPage — migrated per P9-W2-004
 * Removed AdminPanel.css dependency; replaced CSS class names with makeStyles.
 * Tab content wrapped in ContentCard.
 */
import React, { useState } from 'react';
import {
    Tab,
    TabList,
    Divider,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { ContentCard } from '../components/Layout/ContentCard';
import { PageHeader } from '../components/shared/PageHeader';
import OrganizationsPanel from '../components/Admin/OrganizationsPanel';
import UsersPanel from '../components/Admin/UsersPanel';
import ApiKeysPanel from '../components/Admin/ApiKeysPanel';
import FailedJobsPanel from '../components/Admin/FailedJobsPanel';
import BatchesPanel from '../components/Admin/BatchesPanel';
import { StorageRoutingPanel } from '../components/Admin/StorageRoutingPanel';

type AdminTab = 'organizations' | 'users' | 'apikeys' | 'failedjobs' | 'batches' | 'storagerouting';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    tabs: {
        marginBottom: tokens.spacingHorizontalL,
    },
    divider: {
        marginBottom: tokens.spacingHorizontalL,
    },
});

const AdminPage: React.FC = () => {
    const styles = useStyles();
    const [selectedTab, setSelectedTab] = useState<AdminTab>('organizations');

    return (
        <div className={styles.container}>
            <PageHeader
                title="Administration"
                subtitle="Manage organizations, users, roles, and system configuration"
            />

            <Divider className={styles.divider} />

            <TabList
                selectedValue={selectedTab}
                onTabSelect={(_, data) => setSelectedTab(data.value as AdminTab)}
                className={styles.tabs}
            >
                <Tab value="organizations">Organizations</Tab>
                <Tab value="users">Users</Tab>
                <Tab value="apikeys">API Keys</Tab>
                <Tab value="failedjobs">Failed Jobs</Tab>
                <Tab value="batches">Batches</Tab>
                <Tab value="storagerouting">Storage Routing</Tab>
            </TabList>

            <ContentCard>
                {selectedTab === 'organizations' && <OrganizationsPanel />}
                {selectedTab === 'users' && <UsersPanel />}
                {selectedTab === 'apikeys' && <ApiKeysPanel />}
                {selectedTab === 'failedjobs' && <FailedJobsPanel />}
                {selectedTab === 'batches' && <BatchesPanel />}
                {selectedTab === 'storagerouting' && <StorageRoutingPanel />}
            </ContentCard>
        </div>
    );
};

export default AdminPage;
