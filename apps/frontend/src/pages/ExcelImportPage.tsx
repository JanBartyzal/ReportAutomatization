import React, { useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle2,
    Body1,
    Button,
    Field,
    Dropdown,
    Option,
    Spinner,
    Badge,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Divider,
    MessageBar,
} from '@fluentui/react-components';
import {
    ArrowUpload24Regular,
    ArrowLeftRegular,
    CheckmarkCircleRegular,
    WarningRegular,
    ErrorCircleRegular,
} from '@fluentui/react-icons';
import { useImportExcel, useForm } from '../hooks/useForms';
import { exportExcelTemplate } from '../api/forms';
import templatesApi, { MappingSuggestion } from '../api/templates';

export const ExcelImportPage: React.FC = () => {
    const { formId } = useParams<{ formId: string }>();
    const navigate = useNavigate();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const importExcel = useImportExcel(formId!);
    const { data: form } = useForm(formId!);

    // Use the schema mapping hook
    const [selectedFileId, setSelectedFileId] = useState<string | null>(null);
    const [mappings, setMappings] = useState<MappingSuggestion[]>([]);
    const [step, setStep] = useState<'upload' | 'mapping' | 'preview'>('upload');
    const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Fetch mapping suggestions from API when file is selected
    const fetchSuggestions = useCallback(async (fileId: string, fId: string) => {
        setIsLoadingSuggestions(true);
        setError(null);
        try {
            const suggestions = await templatesApi.getMappingSuggestions(fileId, fId);
            setMappings(suggestions);
            setStep('mapping');
        } catch (err) {
            console.error('Failed to fetch mapping suggestions:', err);
            setError('Failed to get mapping suggestions. Please try again.');
            // Fall back to mock data if API fails
            const mockMappings: MappingSuggestion[] = [
                { excelColumn: 'headcount', formField: 'headcount', confidence: 0.95, suggestions: ['headcount', 'total_headcount'] },
                { excelColumn: 'salaries_total', formField: 'salaries_total', confidence: 0.92, suggestions: ['salaries_total', 'total_salaries'] },
                { excelColumn: 'budget_category', formField: 'budget_category', confidence: 0.88, suggestions: ['budget_category', 'category'] },
                { excelColumn: 'unknown_column', formField: null, confidence: 0, suggestions: [] },
            ];
            setMappings(mockMappings);
            setStep('mapping');
        } finally {
            setIsLoadingSuggestions(false);
        }
    }, []);

    const handleDownloadTemplate = async () => {
        try {
            const blob = await exportExcelTemplate(formId!);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `template_${form?.name || 'form'}.xlsx`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Failed to download template', error);
        }
    };

    const handleFileSelect = async () => {
        // In a real implementation, this would upload the file first
        // For now, we'll simulate with a mock file ID
        const mockFileId = `file_${Date.now()}`;
        setSelectedFileId(mockFileId);
        await fetchSuggestions(mockFileId, formId!);
    };

    const handleMappingChange = (excelColumn: string, formField: string | null) => {
        setMappings(prev => prev.map(m =>
            m.excelColumn === excelColumn ? { ...m, formField } : m
        ));
    };

    const handleConfirmMapping = () => {
        setStep('preview');
    };

    const handleApplyMapping = async () => {
        if (!selectedFileId || !formId) return;

        try {
            await templatesApi.applyMapping({
                fileId: selectedFileId,
                formId: formId,
                mappings: mappings,
            });
            navigate(`/forms/${formId}`);
        } catch (err) {
            console.error('Failed to apply mapping:', err);
            setError('Failed to apply mapping. Please try again.');
        }
    };

    const getConfidenceBadge = (confidence: number) => {
        if (confidence >= 0.8) {
            return <Badge appearance="filled" color="success"><CheckmarkCircleRegular /> High</Badge>;
        } else if (confidence >= 0.5) {
            return <Badge appearance="filled" color="warning"><WarningRegular /> Medium</Badge>;
        }
        return <Badge appearance="filled" color="danger"><ErrorCircleRegular /> Low</Badge>;
    };

    // Get unique form field options
    const formFieldOptions = [
        'headcount', 'salaries_total', 'personnel_other', 'it_hardware', 'it_software',
        'it_cloud', 'it_support', 'office_rent', 'office_utilities', 'office_supplies',
        'office_insurance', 'travel_domestic', 'travel_international', 'travel_entertainment',
        'budget_total', 'budget_category', 'budget_notes'
    ];

    return (
        <div style={{ padding: '24px', maxWidth: '1000px', margin: '0 auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
                <Button
                    appearance="transparent"
                    icon={<ArrowLeftRegular />}
                    onClick={() => navigate(-1)}
                >
                    Back
                </Button>
            </div>

            <Title3>Import Excel Data</Title3>
            <Subtitle2 style={{ color: 'var(--colorNeutralForeground2)', marginBottom: '24px' }}>
                Upload an Excel file and map columns to form fields
            </Subtitle2>

            {/* Error message */}
            {error && (
                <MessageBar intent="error" style={{ marginBottom: '16px' }}>
                    {error}
                </MessageBar>
            )}

            {/* Step 1: Upload */}
            {step === 'upload' && (
                <div>
                    <Field
                        label="Select Excel File"
                        hint="Upload an .xlsx file with your data"
                    >
                        <input
                            type="file"
                            accept=".xlsx"
                            ref={fileInputRef}
                            style={{ display: 'none' }}
                            onChange={(e) => {
                                const file = (e.target as HTMLInputElement).files?.[0];
                                if (file) {
                                    // Trigger file selection
                                    handleFileSelect();
                                }
                            }}
                        />
                        <Button
                            appearance="secondary"
                            icon={<ArrowUpload24Regular />}
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isLoadingSuggestions}
                        >
                            {isLoadingSuggestions ? 'Loading...' : 'Choose File'}
                        </Button>
                        {isLoadingSuggestions && (
                            <Spinner size="tiny" style={{ marginLeft: '8px' }} />
                        )}
                    </Field>

                    <Divider style={{ margin: '24px 0' }} />

                    <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
                        <Button
                            appearance="primary"
                            disabled={importExcel.isPending}
                            onClick={handleFileSelect}
                        >
                            {importExcel.isPending ? <Spinner size="tiny" /> : 'Continue'}
                        </Button>
                        <Button
                            appearance="secondary"
                            onClick={handleDownloadTemplate}
                        >
                            Download Template
                        </Button>
                    </div>
                </div>
            )}

            {/* Step 2: Mapping */}
            {step === 'mapping' && (
                <div>
                    <Body1 style={{ marginBottom: '16px' }}>
                        Review the column mappings below. Fields with low confidence may need manual adjustment.
                    </Body1>

                    {isLoadingSuggestions ? (
                        <div style={{ textAlign: 'center', padding: '48px' }}>
                            <Spinner label="Loading mapping suggestions..." />
                        </div>
                    ) : (
                        <>
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHeaderCell>Excel Column</TableHeaderCell>
                                        <TableHeaderCell>Confidence</TableHeaderCell>
                                        <TableHeaderCell>Map to Form Field</TableHeaderCell>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {mappings.map((mapping) => (
                                        <TableRow key={mapping.excelColumn}>
                                            <TableCell>{mapping.excelColumn}</TableCell>
                                            <TableCell>{getConfidenceBadge(mapping.confidence)}</TableCell>
                                            <TableCell>
                                                <Dropdown
                                                    placeholder="Select form field..."
                                                    value={mapping.formField || ''}
                                                    onOptionSelect={(_, data) => handleMappingChange(mapping.excelColumn, data.optionValue as string)}
                                                >
                                                    <Option value="">-- Unmapped --</Option>
                                                    {formFieldOptions.map(field => (
                                                        <Option key={field} value={field}>
                                                            {field}
                                                        </Option>
                                                    ))}
                                                </Dropdown>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>

                            <Divider style={{ margin: '24px 0' }} />

                            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                                <Button appearance="secondary" onClick={() => setStep('upload')}>
                                    Back
                                </Button>
                                <Button appearance="primary" onClick={handleConfirmMapping}>
                                    Confirm Mapping & Preview
                                </Button>
                            </div>
                        </>
                    )}
                </div>
            )}

            {/* Step 3: Preview */}
            {step === 'preview' && (
                <div>
                    <Body1 style={{ marginBottom: '16px' }}>
                        Review your imported data before submitting.
                    </Body1>

                    <div style={{
                        padding: '16px',
                        background: 'var(--colorNeutralBackground1)',
                        borderRadius: '8px',
                        marginBottom: '24px'
                    }}>
                        <Title3>Imported Data Preview</Title3>
                        <Body1>
                            {mappings.filter(m => m.formField).length} columns will be mapped to form fields
                        </Body1>
                    </div>

                    <div style={{ marginBottom: '16px' }}>
                        <Subtitle2>Mapping Summary</Subtitle2>
                        <ul>
                            {mappings.filter(m => m.formField).map(m => (
                                <li key={m.excelColumn}>
                                    {m.excelColumn} → {m.formField}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <Divider style={{ margin: '24px 0' }} />

                    <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                        <Button appearance="secondary" onClick={() => setStep('mapping')}>
                            Back to Mapping
                        </Button>
                        <Button
                            appearance="primary"
                            onClick={handleApplyMapping}
                            disabled={importExcel.isPending}
                        >
                            {importExcel.isPending ? 'Importing...' : 'Import & View Form'}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ExcelImportPage;
