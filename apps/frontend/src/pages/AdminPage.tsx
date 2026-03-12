import { useState } from 'react';
import {
    Card,
    Tab,
    TabList,
    Body1,
    Subtitle1,
    Divider
} from '@fluentui/react-components';
import OrganizationsPanel from '../components/Admin/OrganizationsPanel';
import UsersPanel from '../components/Admin/UsersPanel';
import ApiKeysPanel from '../components/Admin/ApiKeysPanel';
import FailedJobsPanel from '../components/Admin/FailedJobsPanel';
import BatchesPanel from '../components/Admin/BatchesPanel';
import '../components/Admin/AdminPanel.css';

type AdminTab = 'organizations' | 'users' | 'apikeys' | 'failedjobs' | 'batches';

const AdminPage: React.FC = () => {
    const [selectedTab, setSelectedTab] = useState<AdminTab>('organizations');

    return (
        <div className="admin-page">
            <div className="admin-header">
                <Subtitle1>Administration</Subtitle1>
                <Body1>Manage organizations, users, roles, and system configuration</Body1>
            </div>

            <Divider className="admin-divider" />

            <TabList
                selectedValue={selectedTab}
                onTabSelect={(_, data) => setSelectedTab(data.value as AdminTab)}
                className="admin-tabs"
            >
                <Tab value="organizations">Organizations</Tab>
                <Tab value="users">Users</Tab>
                <Tab value="apikeys">API Keys</Tab>
                <Tab value="failedjobs">Failed Jobs</Tab>
                <Tab value="batches">Batches</Tab>
            </TabList>

            <Card className="admin-content">
                {selectedTab === 'organizations' && <OrganizationsPanel />}
                {selectedTab === 'users' && <UsersPanel />}
                {selectedTab === 'apikeys' && <ApiKeysPanel />}
                {selectedTab === 'failedjobs' && <FailedJobsPanel />}
                {selectedTab === 'batches' && <BatchesPanel />}
            </Card>
        </div>
    );
};

export default AdminPage;
