import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Page,
    Title3,
    Subtitle1,
    Subtitle2,
    Body1,
    Caption1,
    Spinner,
    TabList,
    Tab,
    Card,
    CardHeader,
    Button,
    Badge,
    Divider,
    MessageBar,
    MessageBarTitle,
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

export const TemplateDetailPage: React.FC = () => {
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
            <Page>
                <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
                    <Spinner label="Loading template details..." />
                </div>
            </Page>
        );
    }

    if (error || !template) {
        return (
            <Page>
                <div style={{ padding: '24px' }}>
                    <MessageBar intent="error">
                        <MessageBarTitle>Error</MessageBarTitle>
                        Failed to load template: {(error as Error)?.message || 'Template not found'}
                    </MessageBar>
                    <Button
                        icon={<ArrowLeft24Regular />}
                        onClick={() => navigate('/templates')}
                        style={{ marginTop: '16px' }}
                    >
                        Back to Templates
                    </Button>
                </div>
            </Page>
        );
    }

    return (
        <Page>
            <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
                {/* Breadcrumbs / Back */}
                <Button
                    appearance="subtle"
                    icon={<ArrowLeft24Regular />}
                    onClick={() => navigate('/templates')}
                    style={{ marginBottom: '16px' }}
                >
                    Back to Templates
                </Button>

                {/* Header */}
                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    marginBottom: '24px'
                }}>
                    <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <Title3>{template.name}</Title3>
                            <Badge appearance="filled" color="brand">v{template.version}</Badge>
                            <Badge appearance="filled" color={template.scope === 'CENTRAL' ? 'success' : 'informative'}>
                                {template.scope}
                            </Badge>
                        </div>
                        <Subtitle1 style={{ color: '#666' }}>
                            ID: {template.id} • Created {new Date(template.createdAt).toLocaleDateString()}
                        </Subtitle1>
                    </div>
                </div>

                <TabList
                    selectedValue={selectedTab}
                    onTabSelect={(_, d) => setSelectedTab(d.value as any)}
                    style={{ marginBottom: '24px' }}
                >
                    <Tab value="mapping" icon={<Settings24Regular />}>Placeholder Mapping</Tab>
                    <Tab value="history" icon={<History24Regular />}>Version History</Tab>
                    <Tab value="preview" icon={<DocumentPdf24Regular />}>Template Preview</Tab>
                </TabList>

                {selectedTab === 'mapping' && (
                    <Card style={{ padding: '24px' }}>
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
                    <Card style={{ padding: '24px' }}>
                        <Subtitle2 style={{ marginBottom: '16px' }}>Version History</Subtitle2>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {template.versions?.map((v) => (
                                <div key={v.version} style={{
                                    padding: '12px',
                                    border: '1px solid #f0f0f0',
                                    borderRadius: '8px',
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center'
                                }}>
                                    <div>
                                        <Text weight="semibold">Version {v.version}</Text>
                                        <Caption1 style={{ display: 'block', color: '#666' }}>
                                            By {v.createdBy} on {new Date(v.createdAt).toLocaleString()}
                                        </Caption1>
                                        {v.changes && <Body1 style={{ marginTop: '4px' }}>{v.changes}</Body1>}
                                    </div>
                                    <Button size="small">Revert</Button>
                                </div>
                            ))}
                        </div>
                    </Card>
                )}

                {selectedTab === 'preview' && (
                    <Card style={{ padding: '24px', textAlign: 'center' }}>
                        <DocumentPdf24Regular style={{ fontSize: '64px', color: reportBrand[90], marginBottom: '16px' }} />
                        <Title3>PPTX Template Preview</Title3>
                        <Body1 style={{ display: 'block', marginBottom: '24px' }}>
                            Visual preview of template slides with extracted placeholders.
                        </Body1>
                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                            gap: '16px'
                        }}>
                            {[1, 2, 3].map(i => (
                                <div key={i} style={{
                                    height: '150px',
                                    backgroundColor: '#f5f5f5',
                                    borderRadius: '8px',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    border: '1px solid #e0e0e0'
                                }}>
                                    <Caption1>Slide {i}</Caption1>
                                    <div style={{ fontSize: '10px', color: '#999', marginTop: '8px' }}>
                                        Preview not available
                                    </div>
                                </div>
                            ))}
                        </div>
                    </Card>
                )}
            </div>
        </Page>
    );
};

export default TemplateDetailPage;
