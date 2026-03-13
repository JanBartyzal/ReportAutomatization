import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Tab,
    TabList,
    Card,
    Button,
    Badge,
    MessageBar,
    MessageBarTitle,
    makeStyles,
    tokens,
    Text,
    Title3,
    Subtitle1,
    Subtitle2,
    Body1,
    Caption1,
    Spinner,
} from '@fluentui/react-components';
import {
    ArrowLeft24Regular,
    DocumentPdf24Regular,
    History24Regular,
    Settings24Regular,
} from '@fluentui/react-icons';
import { useTemplate, useSaveTemplateMapping } from '../hooks/useTemplates';
import { PlaceholderMappingItem } from '../api/templates';
import PlaceholderMapper from '../components/Templates/PlaceholderMapper';
import { reportBrand } from '../theme/brandTokens';

const useStyles = makeStyles({
    spinnerContainer: {
        display: 'flex',
        justifyContent: 'center',
        padding: '100px',
    },
    errorContainer: {
        padding: '24px',
    },
    backButton: {
        marginBottom: '16px',
    },
    page: {
        padding: '24px',
        maxWidth: '1200px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '24px',
    },
    titleRow: {
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
    },
    subtitleMuted: {
        color: tokens.colorNeutralForeground3,
    },
    tabList: {
        marginBottom: '24px',
    },
    cardPadded: {
        padding: '24px',
    },
    versionList: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
    },
    versionItem: {
        padding: '12px',
        border: `1px solid ${tokens.colorNeutralStroke2}`,
        borderRadius: '8px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    versionDate: {
        display: 'block',
        color: tokens.colorNeutralForeground3,
    },
    versionChanges: {
        marginTop: '4px',
    },
    previewCenter: {
        padding: '24px',
        textAlign: 'center',
    },
    previewBody: {
        display: 'block',
        marginBottom: '24px',
    },
    slideGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
        gap: '16px',
    },
    slideThumbnail: {
        height: '150px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        border: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    slideCaption: {
        fontSize: '10px',
        color: tokens.colorNeutralForeground4,
        marginTop: '8px',
    },
});

export const TemplateDetailPage: React.FC = () => {
    const styles = useStyles();
    const { templateId } = useParams<{ templateId: string }>();
    const navigate = useNavigate();
    const [selectedTab, setSelectedTab] = useState<'mapping' | 'history' | 'preview'>('mapping');

    const { data: template, isLoading, error, refetch } = useTemplate(templateId || '');
    const saveMappingMutation = useSaveTemplateMapping();

    const handleSaveMapping = async (mappings: PlaceholderMappingItem[]) => {
        if (!templateId) return;
        try {
            await saveMappingMutation.mutateAsync({ id: templateId, mappings });
            refetch();
        } catch (err) {
            console.error('Failed to save mapping:', err);
        }
    };

    if (isLoading) {
        return (
            <div className={styles.page}>
                <div className={styles.spinnerContainer}>
                    <Spinner label="Loading template details..." />
                </div>
            </div>
        );
    }

    if (error || !template) {
        return (
            <div className={styles.page}>
                <div className={styles.errorContainer}>
                    <MessageBar intent="error">
                        <MessageBarTitle>Error</MessageBarTitle>
                        Failed to load template: {(error as Error)?.message || 'Template not found'}
                    </MessageBar>
                    <Button
                        icon={<ArrowLeft24Regular />}
                        onClick={() => navigate('/templates')}
                        className={styles.backButton}
                    >
                        Back to Templates
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
                {/* Breadcrumbs / Back */}
                <Button
                    appearance="subtle"
                    icon={<ArrowLeft24Regular />}
                    onClick={() => navigate('/templates')}
                    className={styles.backButton}
                >
                    Back to Templates
                </Button>

                {/* Header */}
                <div className={styles.header}>
                    <div>
                        <div className={styles.titleRow}>
                            <Title3>{template.name}</Title3>
                            <Badge appearance="filled" color="brand">v{template.version}</Badge>
                            <Badge appearance="filled" color={template.scope === 'CENTRAL' ? 'success' : 'informative'}>
                                {template.scope}
                            </Badge>
                        </div>
                        <Subtitle1 className={styles.subtitleMuted}>
                            ID: {template.id} • Created {new Date(template.createdAt).toLocaleDateString()}
                        </Subtitle1>
                    </div>
                </div>

                <TabList
                    selectedValue={selectedTab}
                    onTabSelect={(_, d) => setSelectedTab(d.value as any)}
                    className={styles.tabList}
                >
                    <Tab value="mapping" icon={<Settings24Regular />}>Placeholder Mapping</Tab>
                    <Tab value="history" icon={<History24Regular />}>Version History</Tab>
                    <Tab value="preview" icon={<DocumentPdf24Regular />}>Template Preview</Tab>
                </TabList>

                {selectedTab === 'mapping' && (
                    <Card className={styles.cardPadded}>
                        <PlaceholderMapper
                            templateId={template.id}
                            placeholders={template.placeholders}
                            initialMappings={template.mapping?.mappings}
                            onSave={handleSaveMapping}
                            isSaving={saveMappingMutation.isPending}
                        />
                    </Card>
                )}

                {selectedTab === 'history' && (
                    <Card className={styles.cardPadded}>
                        <Subtitle2 style={{ marginBottom: '16px' }}>Version History</Subtitle2>
                        <div className={styles.versionList}>
                            {template.versions?.map((v) => (
                                <div key={v.version} className={styles.versionItem}>
                                    <div>
                                        <Text weight="semibold">Version {v.version}</Text>
                                        <Caption1 className={styles.versionDate}>
                                            By {v.createdBy} on {new Date(v.createdAt).toLocaleString()}
                                        </Caption1>
                                        {v.changes && <Body1 className={styles.versionChanges}>{v.changes}</Body1>}
                                    </div>
                                    <Button size="small">Revert</Button>
                                </div>
                            ))}
                        </div>
                    </Card>
                )}

                {selectedTab === 'preview' && (
                    <Card className={styles.previewCenter}>
                        <DocumentPdf24Regular style={{ fontSize: '64px', color: reportBrand[90], marginBottom: '16px' }} />
                        <Title3>PPTX Template Preview</Title3>
                        <Body1 className={styles.previewBody}>
                            Visual preview of template slides with extracted placeholders.
                        </Body1>
                        <div className={styles.slideGrid}>
                            {[1, 2, 3].map(i => (
                                <div key={i} className={styles.slideThumbnail}>
                                    <Caption1>Slide {i}</Caption1>
                                    <div className={styles.slideCaption}>
                                        Preview not available
                                    </div>
                                </div>
                            ))}
                        </div>
                    </Card>
                )}
        </div>
    );
};

export default TemplateDetailPage;
