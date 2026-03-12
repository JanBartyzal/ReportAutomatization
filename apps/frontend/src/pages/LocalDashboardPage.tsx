/**
 * LocalDashboardPage - CompanyAdmin Local Scope Dashboard
 * Shows local forms and templates for the current company
 * 
 * Per docs/UX-UI/04-figma-pages.md - CompanyAdmin dashboard
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Page,
    Title3,
    Subtitle1,
    Body1,
    Caption1,
    Button,
    Card,
    CardHeader,
    CardPreview,
    makeStyles,
    tokens,
    Spinner,
    Tab,
    TabList,
    Badge,
} from '@fluentui/react-components';
import {
    Add24Regular,
    DocumentPdf24Regular,
    Form24Regular,
    ArrowUpload24Regular,
} from '@fluentui/react-icons';
import { ShareDialog } from '../components/LocalScope/ShareDialog';
import { ReleaseDialog } from '../components/LocalScope/ReleaseDialog';
import { useForms } from '../hooks/useForms';
import { useTemplates } from '../hooks/useTemplates';
import { useLocalScope } from '../hooks/useFeatureFlags';
import { ScopeBadge } from '../components/ScopeBadge';
import LoadingSpinner from '../LoadingSpinner';
import { reportBrand } from '../theme/brandTokens';

// Styles per design system
const useStyles = makeStyles({
    page: {
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
    titleSection: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
    },
    tabs: {
        marginBottom: tokens.spacingHorizontalL,
    },
    cardsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
        gap: tokens.spacingHorizontalM,
    },
    card: {
        cursor: 'pointer',
        transition: 'box-shadow 0.2s ease',
        ':hover': {
            boxShadow: tokens.shadowLevel4,
        },
    },
    cardHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
    },
    cardDescription: {
        marginTop: tokens.spacingVerticalXS,
    },
    cardPreview: {
        height: '100px',
        backgroundColor: tokens.colorNeutralBackground3,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    emptyState: {
        textAlign: 'center',
        padding: '60px 20px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
    },
    sectionHeader: {
        marginBottom: tokens.spacingHorizontalM,
    },
    badgeGroup: {
        display: 'flex',
        gap: tokens.spacingHorizontalXS,
        alignItems: 'center',
    },
});

interface LocalForm {
    id: string;
    title: string;
    description?: string;
    status: string;
    version: number;
    scope: 'CENTRAL' | 'LOCAL' | 'SHARED';
    created_at: string;
}

interface LocalTemplate {
    id: string;
    name: string;
    version: number;
    scope: 'CENTRAL' | 'LOCAL';
    placeholderCount: number;
    updatedAt: string;
}

export const LocalDashboardPage: React.FC = () => {
    const navigate = useNavigate();
    const styles = useStyles();
    const enableLocalScope = useLocalScope();

    const [activeTab, setActiveTab] = useState<'forms' | 'templates'>('forms');

    // Fetch local forms (LOCAL scope only)
    const { data: formsData, isLoading: formsLoading } = useForms({
        scope: 'LOCAL',
    });

    // Fetch local templates
    const { data: templatesData, isLoading: templatesLoading } = useTemplates();

    // Filter to local templates only
    const localTemplates = templatesData?.filter(t => t.scope === 'LOCAL') || [];

    // Show loading state
    if (formsLoading || templatesLoading) {
        return <LoadingSpinner />;
    }

    // Redirect if local scope is not enabled
    if (!enableLocalScope) {
        return (
            <Page>
                <div className={styles.page}>
                    <div className={styles.emptyState}>
                        <Body1>Local scope feature is not enabled.</Body1>
                        <Button
                            appearance="primary"
                            style={{ marginTop: tokens.spacingHorizontalM }}
                            onClick={() => navigate('/forms')}
                        >
                            Go to Forms
                        </Button>
                    </div>
                </div>
            </Page>
        );
    }

    const forms = formsData?.content || [];

    return (
        <Page>
            <div className={styles.page}>
                {/* Header */}
                <div className={styles.header}>
                    <div className={styles.titleSection}>
                        <Title3 style={{ color: reportBrand[90] }}>
                            My Local Workspace
                        </Title3>
                        <Subtitle1>
                            Manage your company's local forms and templates
                        </Subtitle1>
                    </div>
                    <div className={styles.actions}>
                        <Button
                            appearance="primary"
                            icon={<Form24Regular />}
                            style={{ backgroundColor: reportBrand[90] }}
                            onClick={() => navigate('/forms/new?scope=LOCAL')}
                        >
                            Create Local Form
                        </Button>
                        <Button
                            appearance="secondary"
                            icon={<DocumentPdf24Regular />}
                            onClick={() => navigate('/templates/new?scope=LOCAL')}
                        >
                            Upload Local Template
                        </Button>
                    </div>
                </div>

                {/* Tabs */}
                <TabList
                    selectedValue={activeTab}
                    onTabSelect={(_, data) => setActiveTab(data.value as 'forms' | 'templates')}
                    className={styles.tabs}
                >
                    <Tab value="forms">
                        <Badge appearance="filled" color="informative" size="small">
                            {forms.length}
                        </Badge>
                        {' '}Local Forms
                    </Tab>
                    <Tab value="templates">
                        <Badge appearance="filled" color="informative" size="small">
                            {localTemplates.length}
                        </Badge>
                        {' '}Local Templates
                    </Tab>
                </TabList>

                {/* Content based on active tab */}
                {activeTab === 'forms' ? (
                    <div>
                        {forms.length > 0 ? (
                            <div className={styles.cardsGrid}>
                                {forms.map((form) => (
                                    <Card
                                        key={form.id}
                                        className={styles.card}
                                        onClick={() => navigate(`/forms/${form.id}`)}
                                    >
                                        <CardHeader
                                            header={
                                                <div className={styles.cardHeader}>
                                                    <Subtitle1>{form.title}</Subtitle1>
                                                    <div className={styles.badgeGroup}>
                                                        <Badge
                                                            appearance="filled"
                                                            color={form.status === 'PUBLISHED' ? 'success' : 'informative'}
                                                        >
                                                            {form.status}
                                                        </Badge>
                                                        <ScopeBadge scope={form.scope} />
                                                    </div>
                                                </div>
                                            }
                                            description={
                                                <div className={styles.cardDescription}>
                                                    <Caption1>{form.description || 'No description'}</Caption1>
                                                    <Caption1 style={{ marginTop: tokens.spacingVerticalXS }}>
                                                        Version {form.version || 1} • Created {new Date(form.created_at).toLocaleDateString()}
                                                    </Caption1>
                                                </div>
                                            }
                                        />
                                        <CardPreview className={styles.cardPreview}>
                                            <Form24Regular style={{ fontSize: '32px', color: reportBrand[80] }} />
                                        </CardPreview>
                                        <div style={{ padding: '8px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                                            <ShareDialog 
                                                itemId={form.id} 
                                                itemName={form.title} 
                                                itemType="FORM" 
                                            />
                                            <ReleaseDialog 
                                                dataId={form.id} 
                                                dataName={form.title} 
                                                period="2024-Q1" 
                                            />
                                        </div>
                                    </Card>
                                ))}
                            </div>
                        ) : (
                            <div className={styles.emptyState}>
                                <Form24Regular style={{ fontSize: '48px', color: '#ccc' }} />
                                <Title3 style={{ marginTop: tokens.spacingHorizontalM }}>
                                    No local forms yet
                                </Title3>
                                <Body1 style={{ color: tokens.colorNeutralForeground2, marginTop: tokens.spacingVerticalS }}>
                                    Create your first local form to start collecting data
                                </Body1>
                                <Button
                                    appearance="primary"
                                    icon={<Add24Regular />}
                                    style={{ marginTop: tokens.spacingHorizontalM, backgroundColor: reportBrand[90] }}
                                    onClick={() => navigate('/forms/new?scope=LOCAL')}
                                >
                                    Create Local Form
                                </Button>
                            </div>
                        )}
                    </div>
                ) : (
                    <div>
                        {localTemplates.length > 0 ? (
                            <div className={styles.cardsGrid}>
                                {localTemplates.map((template) => (
                                    <Card
                                        key={template.id}
                                        className={styles.card}
                                        onClick={() => navigate(`/templates/${template.id}`)}
                                    >
                                        <CardHeader
                                            header={
                                                <div className={styles.cardHeader}>
                                                    <Subtitle1>{template.name}</Subtitle1>
                                                    <div className={styles.badgeGroup}>
                                                        <Badge appearance="filled" color="brand">
                                                            v{template.version}
                                                        </Badge>
                                                        <ScopeBadge scope={template.scope} />
                                                    </div>
                                                </div>
                                            }
                                            description={
                                                <div className={styles.cardDescription}>
                                                    <Caption1>{template.placeholderCount} placeholders</Caption1>
                                                    <Caption1 style={{ marginTop: tokens.spacingVerticalXS }}>
                                                        Updated {new Date(template.updatedAt).toLocaleDateString()}
                                                    </Caption1>
                                                </div>
                                            }
                                        />
                                        <CardPreview className={styles.cardPreview}>
                                            <DocumentPdf24Regular style={{ fontSize: '32px', color: reportBrand[80] }} />
                                        </CardPreview>
                                        <div style={{ padding: '8px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                                            <ShareDialog 
                                                itemId={template.id} 
                                                itemName={template.name} 
                                                itemType="TEMPLATE" 
                                            />
                                        </div>
                                    </Card>
                                ))}
                            </div>
                        ) : (
                            <div className={styles.emptyState}>
                                <DocumentPdf24Regular style={{ fontSize: '48px', color: '#ccc' }} />
                                <Title3 style={{ marginTop: tokens.spacingHorizontalM }}>
                                    No local templates yet
                                </Title3>
                                <Body1 style={{ color: tokens.colorNeutralForeground2, marginTop: tokens.spacingVerticalS }}>
                                    Upload your first local template to start generating reports
                                </Body1>
                                <Button
                                    appearance="primary"
                                    icon={<ArrowUpload24Regular />}
                                    style={{ marginTop: tokens.spacingHorizontalM, backgroundColor: reportBrand[90] }}
                                    onClick={() => navigate('/templates/new?scope=LOCAL')}
                                >
                                    Upload Local Template
                                </Button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </Page>
    );
};

export default LocalDashboardPage;