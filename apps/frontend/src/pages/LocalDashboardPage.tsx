/**
 * LocalDashboardPage - CompanyAdmin Local Scope Dashboard
 * Shows local forms and templates for the current company
 * 
 * Per docs/UX-UI/04-figma-pages.md - CompanyAdmin dashboard
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
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
import LoadingSpinner from '../components/LoadingSpinner';

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
            boxShadow: tokens.shadow4,
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
    brandTitle: {
        color: tokens.colorBrandForeground1,
    },
    cardIcon: {
        fontSize: '32px',
        color: tokens.colorBrandForeground2,
    },
    emptyIcon: {
        fontSize: '48px',
        color: tokens.colorNeutralForeground4,
    },
    emptyBody: {
        color: tokens.colorNeutralForeground2,
        marginTop: tokens.spacingVerticalS,
    },
    marginTop: {
        marginTop: tokens.spacingHorizontalM,
    },
    cardActions: {
        padding: tokens.spacingVerticalS,
        display: 'flex',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalS,
    },
});

/* Unused local interfaces removed to favor shared types */

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
            <div className={styles.page}>
                <div className={styles.emptyState}>
                    <Body1>Local scope feature is not enabled.</Body1>
                    <Button
                        appearance="primary"
                        className={styles.marginTop}
                        onClick={() => navigate('/forms')}
                    >
                        Go to Forms
                    </Button>
                </div>
            </div>
        );
    }

    const forms = formsData?.data || [];

    return (
        <div>
            <div className={styles.page}>
                {/* Header */}
                <div className={styles.header}>
                    <div className={styles.titleSection}>
                        <Title3 className={styles.brandTitle}>
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
                                                    <Subtitle1>{form.name}</Subtitle1>
                                                    <div className={styles.badgeGroup}>
                                                        <Badge
                                                            appearance="filled"
                                                            color={form.status === 'PUBLISHED' ? 'success' : 'informative'}
                                                        >
                                                            {form.status}
                                                        </Badge>
                                                        <ScopeBadge scope={form.scope === 'SHARED_WITHIN_HOLDING' ? 'SHARED' : form.scope as any} />
                                                    </div>
                                                </div>
                                            }
                                            description={
                                                <div className={styles.cardDescription}>
                                                    <Caption1>{form.description || 'No description'}</Caption1>
                                                        Created {form.created_at ? new Date(form.created_at).toLocaleDateString() : 'N/A'}
                                                </div>
                                            }
                                        />
                                        <CardPreview className={styles.cardPreview}>
                                            <Form24Regular className={styles.cardIcon} />
                                        </CardPreview>
                                        <div className={styles.cardActions}>
                                            <ShareDialog 
                                                itemId={form.id || ''} 
                                                itemName={form.name} 
                                                itemType="FORM" 
                                            />
                                            <ReleaseDialog 
                                                dataId={form.id || ''} 
                                                dataName={form.name} 
                                                period="2024-Q1" 
                                            />
                                        </div>
                                    </Card>
                                ))}
                            </div>
                        ) : (
                            <div className={styles.emptyState}>
                                <Form24Regular className={styles.emptyIcon} />
                                <Title3 className={styles.marginTop}>
                                    No local forms yet
                                </Title3>
                                <Body1 className={styles.emptyBody}>
                                    Create your first local form to start collecting data
                                </Body1>
                                <Button
                                    appearance="primary"
                                    icon={<Add24Regular />}
                                    className={styles.marginTop}
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
                                                    <Caption1>
                                                        Updated {new Date(template.updatedAt).toLocaleDateString()}
                                                    </Caption1>
                                                </div>
                                            }
                                        />
                                        <CardPreview className={styles.cardPreview}>
                                            <DocumentPdf24Regular className={styles.cardIcon} />
                                        </CardPreview>
                                        <div className={styles.cardActions}>
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
                                <DocumentPdf24Regular className={styles.emptyIcon} />
                                <Title3 className={styles.marginTop}>
                                    No local templates yet
                                </Title3>
                                <Body1 className={styles.emptyBody}>
                                    Upload your first local template to start generating reports
                                </Body1>
                                <Button
                                    appearance="primary"
                                    icon={<ArrowUpload24Regular />}
                                    className={styles.marginTop}
                                    onClick={() => navigate('/templates/new?scope=LOCAL')}
                                >
                                    Upload Local Template
                                </Button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default LocalDashboardPage;