import { useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle2,
    Body1,
    Button,
    Field,
    Input,
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
} from '@fluentui/react-components';
import {
    UploadRegular,
    ArrowLeftRegular,
    CheckmarkCircleRegular,
    WarningRegular,
    ErrorCircleRegular,
} from '@fluentui/react-icons';
import { useImportExcel, useForm } from '../../hooks/useForms';
import { exportExcelTemplate } from '../../api/forms';
import LoadingSpinner from '../LoadingSpinner';

interface MappingSuggestion {
    excelColumn: string;
    formField: string | null;
    confidence: number;
    suggestions: string[];
}

export const ExcelImportPage: React.FC = () => {
    const { formId } = useParams<{ formId: string }>();
    const navigate = useNavigate();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const importExcel = useImportExcel(formId!);
    const { data: form } = useForm(formId!);

    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [mappings, setMappings] = useState<MappingSuggestion[]>([]);
    const [step, setStep] = useState<'upload' | 'mapping' | 'preview'>('upload');

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
        if (!selectedFile) return;

        // This would call the API to get mapping suggestions
        // For now, we'll simulate with mock data
        const mockMappings: MappingSuggestion[] = [
            { excelColumn: 'headcount', formField: 'headcount', confidence: 0.95, suggestions: ['headcount', 'total_headcount'] },
            { excelColumn: 'salaries_total', formField: 'salaries_total', confidence: 0.92, suggestions: ['salaries_total', 'total_salaries'] },
            { excelColumn: 'budget_category', formField: 'budget_category', confidence: 0.88, suggestions: ['budget_category', 'category'] },
            { excelColumn: 'unknown_column', formField: null, confidence: 0, suggestions: [] },
        ];
        setMappings(mockMappings);
        setStep('mapping');
    };

    const handleMappingChange = (excelColumn: string, formField: string | null) => {
        setMappings(prev => prev.map(m =>
            m.excelColumn === excelColumn ? { ...m, formField } : m
        ));
    };

    const handleConfirmMapping = () => {
        setStep('preview');
    };

    const getConfidenceBadge = (confidence: number) => {
        if (confidence >= 0.8) {
            return <Badge appearance="filled" color="success"><CheckmarkCircleRegular /> High</Badge>;
        } else if (confidence >= 0.5) {
            return <Badge appearance="filled" color="warning"><WarningRegular /> Medium</Badge>;
        }
        return <Badge appearance="filled" color="danger"><ErrorCircleRegular /> Low</Badge>;
    };

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
                                if (file) setSelectedFile(file);
                            }}
                        />
                        <Button
                            appearance="secondary"
                            icon={<UploadRegular />}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            {selectedFile ? selectedFile.name : 'Choose File'}
                        </Button>
                        {selectedFile && (
                            <Body1 style={{ marginTop: '8px' }}>
                                Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                            </Body1>
                        )}
                    </Field>

                    <Divider style={{ margin: '24px 0' }} />

                    <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
                        <Button
                            appearance="primary"
                            disabled={!selectedFile || importExcel.isPending}
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
                                            <Option value="headcount">headcount - Total Headcount</Option>
                                            <Option value="salaries_total">salaries_total - Total Salaries</Option>
                                            <Option value="personnel_other">personnel_other - Other Personnel Costs</Option>
                                            <Option value="it_hardware">it_hardware - Hardware</Option>
                                            <Option value="it_software">it_software - Software</Option>
                                            <Option value="it_cloud">it_cloud - Cloud Services</Option>
                                            <Option value="it_support">it_support - IT Support</Option>
                                            <Option value="office_rent">office_rent - Rent</Option>
                                            <Option value="office_utilities">office_utilities - Utilities</Option>
                                            <Option value="office_supplies">office_supplies - Supplies</Option>
                                            <Option value="office_insurance">office_insurance - Insurance</Option>
                                            <Option value="travel_domestic">travel_domestic - Domestic Travel</Option>
                                            <Option value="travel_international">travel_international - International Travel</Option>
                                            <Option value="travel_entertainment">travel_entertainment - Entertainment</Option>
                                            <Option value="budget_total">budget_total - Total Budget</Option>
                                            <Option value="budget_category">budget_category - Category</Option>
                                            <Option value="budget_notes">budget_notes - Notes</Option>
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
                        <Body1>5 rows imported from {selectedFile?.name}</Body1>
                    </div>

                    <Divider style={{ margin: '24px 0' }} />

                    <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                        <Button appearance="secondary" onClick={() => setStep('mapping')}>
                            Back to Mapping
                        </Button>
                        <Button
                            appearance="primary"
                            onClick={() => navigate(`/forms/${formId}`)}
                        >
                            Import & View Form
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ExcelImportPage;
