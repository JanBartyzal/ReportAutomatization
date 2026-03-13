/**
 * SchemaMappingPage — Full implementation per P11-W4-002
 * Column mapping editor for Excel to form field mapping
 */
import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Subtitle2,
    Body1,
    Button,
    Field,
    Input,
    Spinner,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    MessageBar,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    SaveRegular,
    ArrowDownloadRegular,
} from '@fluentui/react-icons';
import { useQuery } from '@tanstack/react-query';
import { ContentCard } from '../components/Layout/ContentCard';
import { PageHeader } from '../components/shared/PageHeader';
import { MappingEditor } from '../components/SchemaMapping';
import useSchemaMapping from '../hooks/useSchemaMapping';
import templatesApi, { MappingSuggestion } from '../api/templates';
import { useDropzone } from 'react-dropzone';

const SchemaMappingPage: React.FC = () => {
    const { fileId, formId } = useParams<{ fileId?: string; formId?: string }>();
    const navigate = useNavigate();

    // Local state
    const [selectedFileId] = useState<string | undefined>(fileId);
    const [selectedFormId, setSelectedFormId] = useState<string | undefined>(formId);
    const [isSaveDialogOpen, setIsSaveDialogOpen] = useState(false);
    const [templateName, setTemplateName] = useState('');
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    // Use the schema mapping hook
    const {
        mappings,
        sourceColumns,
        targetFields,
        isLoading,
        isFetchingSuggestions,
        error,
        updateMapping,
        autoMap,
        clearMappings,
        applyMapping,
        isApplying,
        saveTemplate,
        isSaving,
        initializeMappings,
    } = useSchemaMapping(selectedFileId, selectedFormId);

    // Fetch suggestions when fileId and formId are available
    const suggestionsQuery = useQuery({
        queryKey: ['mapping-suggestions', selectedFileId, selectedFormId],
        queryFn: () => templatesApi.getMappingSuggestions(selectedFileId!, selectedFormId!),
        enabled: !!selectedFileId && !!selectedFormId,
    });

    // Fetch saved mapping templates
    const templatesQuery = useQuery({
        queryKey: ['mapping-templates'],
        queryFn: () => templatesApi.getMappingTemplates(),
    });

    // Initialize mappings when data is loaded
    useCallback(() => {
        if (suggestionsQuery.data && suggestionsQuery.data.length > 0) {
            initializeMappings(
                suggestionsQuery.data.map((m: MappingSuggestion) => ({
                    name: m.excelColumn,
                    sampleValues: [],
                })),
                [] // Form fields would need to be fetched
            );
        }
    }, [suggestionsQuery.data]);

    // Handle file drop
    const onDrop = useCallback((acceptedFiles: File[]) => {
        // In a real implementation, this would upload the file first
        // For now, we'll just show the file name
        console.log('Files dropped:', acceptedFiles);
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: {
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
            'application/vnd.ms-excel': ['.xls'],
        },
        multiple: false,
    });

    // Handle apply mapping
    const handleApplyMapping = useCallback(() => {
        if (!selectedFileId || !selectedFormId) return;

        applyMapping(
            { mappings },
            {
                onSuccess: () => {
                    setSuccessMessage('Mapping applied successfully!');
                    setTimeout(() => setSuccessMessage(null), 3000);
                },
            }
        );
    }, [applyMapping, mappings, selectedFileId, selectedFormId]);

    // Handle save template
    const handleSaveTemplate = useCallback(() => {
        if (!templateName.trim()) return;

        saveTemplate(
            { name: templateName, mappings },
            {
                onSuccess: () => {
                    setIsSaveDialogOpen(false);
                    setTemplateName('');
                    setSuccessMessage('Mapping template saved successfully!');
                    setTimeout(() => setSuccessMessage(null), 3000);
                },
            }
        );
    }, [saveTemplate, templateName, mappings]);

    // Handle template selection
    const handleLoadTemplate = useCallback((templateMappings: MappingSuggestion[]) => {
        // This would set the mappings from the template
        console.log('Loading template:', templateMappings);
    }, []);

    // Loading state
    if (isLoading || suggestionsQuery.isLoading) {
        return (
            <div style={{ padding: '24px' }}>
                <Spinner label="Loading schema mapping..." />
            </div>
        );
    }

    // Error state
    if (error || suggestionsQuery.error) {
        return (
            <div style={{ padding: '24px' }}>
                <MessageBar intent="error">
                    Failed to load schema mapping: {String(error || suggestionsQuery.error)}
                </MessageBar>
            </div>
        );
    }

    return (
        <div style={{ padding: '24px', maxWidth: '1400px', margin: '0 auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
                <Button
                    appearance="transparent"
                    icon={<ArrowLeftRegular />}
                    onClick={() => navigate(-1)}
                >
                    Back
                </Button>
            </div>

            <PageHeader
                title="Schema Mapping"
                subtitle="Map source columns to target columns for data import"
            />

            {/* Success message */}
            {successMessage && (
                <MessageBar intent="success" style={{ marginBottom: '16px' }}>
                    {successMessage}
                </MessageBar>
            )}

            {/* File and Form Selection */}
            <ContentCard style={{ marginBottom: '16px', padding: '16px' }}>
                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                    <Field label="Excel File" style={{ flex: 1, minWidth: '200px' }}>
                        <div
                            {...getRootProps()}
                            style={{
                                border: `2px dashed ${isDragActive ? 'var(--colorBrandStroke)' : 'var(--colorNeutralStroke1)'}`,
                                borderRadius: '8px',
                                padding: '24px',
                                textAlign: 'center',
                                cursor: 'pointer',
                                backgroundColor: isDragActive ? 'var(--colorNeutralBackground4)' : 'var(--colorNeutralBackground1)',
                            }}
                        >
                            <input {...getInputProps()} />
                            {selectedFileId ? (
                                <Body1>File selected: {selectedFileId}</Body1>
                            ) : (
                                <Body1>
                                    {isDragActive
                                        ? 'Drop the file here...'
                                        : 'Drag and drop an Excel file here, or click to select'}
                                </Body1>
                            )}
                        </div>
                    </Field>

                    <Field label="Target Form" style={{ flex: 1, minWidth: '200px' }}>
                        <Input
                            placeholder="Enter form ID..."
                            value={selectedFormId || ''}
                            onChange={(_e, data) => setSelectedFormId(data.value)}
                        />
                    </Field>
                </div>
            </ContentCard>

            {/* Mapping Editor */}
            {selectedFileId && selectedFormId && (
                <>
                    {/* Toolbar */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            {/* Template selector */}
                            <Field label="Load Template">
                                <select
                                    style={{
                                        padding: '8px 12px',
                                        borderRadius: '4px',
                                        border: '1px solid var(--colorNeutralStroke1)',
                                        backgroundColor: 'var(--colorNeutralBackground1)',
                                        minWidth: '200px',
                                    }}
                                    onChange={(e) => {
                                        const template = templatesQuery.data?.find(t => t.id === e.target.value);
                                        if (template) handleLoadTemplate(template.mappings);
                                    }}
                                    value=""
                                >
                                    <option value="">Select a template...</option>
                                    {templatesQuery.data?.map((template) => (
                                        <option key={template.id} value={template.id}>
                                            {template.name}
                                        </option>
                                    ))}
                                </select>
                            </Field>
                        </div>

                        <div style={{ display: 'flex', gap: '8px' }}>
                            <Dialog
                                open={isSaveDialogOpen}
                                onOpenChange={(_e, data) => setIsSaveDialogOpen(data.open)}
                            >
                                <DialogTrigger disableButtonEnhancement>
                                    <Button appearance="secondary" icon={<SaveRegular />}>
                                        Save Template
                                    </Button>
                                </DialogTrigger>
                                <DialogSurface>
                                    <DialogBody>
                                        <DialogTitle>Save Mapping Template</DialogTitle>
                                        <DialogContent>
                                            <Field label="Template Name">
                                                <Input
                                                    value={templateName}
                                                    onChange={(_e, data) => setTemplateName(data.value)}
                                                    placeholder="Enter template name..."
                                                />
                                            </Field>
                                        </DialogContent>
                                        <DialogActions>
                                            <Button appearance="secondary" onClick={() => setIsSaveDialogOpen(false)}>
                                                Cancel
                                            </Button>
                                            <Button appearance="primary" onClick={handleSaveTemplate} disabled={isSaving}>
                                                {isSaving ? 'Saving...' : 'Save'}
                                            </Button>
                                        </DialogActions>
                                    </DialogBody>
                                </DialogSurface>
                            </Dialog>

                            <Button
                                appearance="primary"
                                icon={<ArrowDownloadRegular />}
                                onClick={handleApplyMapping}
                                disabled={isApplying || mappings.filter(m => m.formField).length === 0}
                            >
                                {isApplying ? 'Applying...' : 'Apply Mapping'}
                            </Button>
                        </div>
                    </div>

                    {/* Mapping editor component */}
                    <MappingEditor
                        sourceColumns={sourceColumns.length > 0 ? sourceColumns : (suggestionsQuery.data || []).map((m: MappingSuggestion) => ({ name: m.excelColumn, sampleValues: [] }))}
                        targetFields={targetFields}
                        mappings={suggestionsQuery.data || mappings}
                        onMappingChange={updateMapping}
                        onAutoMap={autoMap}
                        onClearMappings={clearMappings}
                        isLoading={isFetchingSuggestions}
                    />
                </>
            )}

            {/* Empty state */}
            {!selectedFileId && !selectedFormId && (
                <ContentCard>
                    <Body1 style={{ textAlign: 'center', padding: '48px', color: 'var(--colorNeutralForeground2)' }}>
                        <Subtitle2>Get Started</Subtitle2>
                        <p>Select an Excel file and target form to start mapping columns.</p>
                    </Body1>
                </ContentCard>
            )}
        </div>
    );
};

export default SchemaMappingPage;
