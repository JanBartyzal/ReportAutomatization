import React, { useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
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
    makeStyles,
    tokens,
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
import { uploadFile } from '../api/files';
import { UploadPurpose } from '@reportplatform/types';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalXXL,
        maxWidth: '1000px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        marginBottom: tokens.spacingVerticalXL,
    },
    subtitle: {
        color: tokens.colorNeutralForeground2,
        marginBottom: tokens.spacingVerticalXL,
    },
    error: {
        marginBottom: tokens.spacingVerticalM,
    },
    selectedFileName: {
        marginTop: tokens.spacingVerticalS,
        color: tokens.colorNeutralForeground2,
    },
    uploadStatus: {
        marginTop: tokens.spacingVerticalS,
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    divider: {
        margin: `${tokens.spacingVerticalXL} 0`,
    },
    actionRow: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalM,
    },
    actionRowEnd: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        justifyContent: 'flex-end',
    },
    previewBox: {
        padding: tokens.spacingHorizontalL,
        background: tokens.colorNeutralBackground1,
        borderRadius: tokens.borderRadiusMedium,
        marginBottom: tokens.spacingVerticalXL,
    },
    mappingSummary: {
        marginBottom: tokens.spacingVerticalM,
    },
});

export const ExcelImportPage: React.FC = () => {
    const { formId } = useParams<{ formId: string }>();
    const navigate = useNavigate();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const styles = useStyles();

    const importExcel = useImportExcel(formId!);
    const { data: form } = useForm(formId!);

    const { data: formFields = [], isLoading: formFieldsLoading } = useQuery({
        queryKey: ['form-fields', formId],
        queryFn: () => templatesApi.getFormFields(formId!),
        enabled: !!formId,
        staleTime: 5 * 60 * 1000,
    });

    const [selectedFileId, setSelectedFileId] = useState<string | null>(null);
    const [selectedFileName, setSelectedFileName] = useState<string | null>(null);
    const [pendingFile, setPendingFile] = useState<File | null>(null);
    const [mappings, setMappings] = useState<MappingSuggestion[]>([]);
    const [step, setStep] = useState<'upload' | 'mapping' | 'preview'>('upload');
    const [isProcessing, setIsProcessing] = useState(false);
    const [uploadProgress, setUploadProgress] = useState<number>(0);
    const [error, setError] = useState<string | null>(null);

    const handleFileSelect = useCallback(async (file: File) => {
        setIsProcessing(true);
        setError(null);
        setUploadProgress(0);
        try {
            // Upload file to ingestor and get a real server-assigned file ID
            const uploadResponse = await uploadFile(file, UploadPurpose.FORM_IMPORT, (progress) => {
                if (progress.total) {
                    setUploadProgress(Math.round((progress.loaded / progress.total) * 100));
                }
            });
            setSelectedFileId(uploadResponse.file_id);

            // Get AI-assisted column-to-field mapping suggestions from the uploaded file
            const suggestions = await templatesApi.getMappingSuggestions(uploadResponse.file_id, formId!);
            setMappings(suggestions);
            setStep('mapping');
        } catch (err) {
            console.error('Failed to upload file or fetch mapping suggestions:', err);
            setError('Failed to process file. Please try again.');
        } finally {
            setIsProcessing(false);
            setUploadProgress(0);
        }
    }, [formId]);

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
        } catch (err) {
            console.error('Failed to download template', err);
        }
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

    const uploadStatusLabel = uploadProgress > 0 && uploadProgress < 100
        ? `Uploading... ${uploadProgress}%`
        : 'Analyzing columns...';

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Button
                    appearance="transparent"
                    icon={<ArrowLeftRegular />}
                    onClick={() => navigate(-1)}
                >
                    Back
                </Button>
            </div>

            <Title3 block>Import Excel Data</Title3>
            <Subtitle2 block className={styles.subtitle}>
                Upload an Excel file and map columns to form fields
            </Subtitle2>

            {error && (
                <MessageBar intent="error" className={styles.error}>
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
                            title="Select an Excel file (.xlsx)"
                            aria-label="Select an Excel file"
                            hidden
                            onChange={(e) => {
                                const file = (e.target as HTMLInputElement).files?.[0];
                                if (file) {
                                    setSelectedFileName(file.name);
                                    setPendingFile(file);
                                    handleFileSelect(file);
                                }
                            }}
                        />
                        <Button
                            appearance="secondary"
                            icon={<ArrowUpload24Regular />}
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isProcessing}
                        >
                            {isProcessing ? 'Processing...' : 'Choose File'}
                        </Button>
                        {selectedFileName && !isProcessing && (
                            <Body1 block className={styles.selectedFileName}>
                                Selected: {selectedFileName}
                            </Body1>
                        )}
                        {isProcessing && (
                            <div className={styles.uploadStatus}>
                                <Spinner size="tiny" />
                                <Body1>{uploadStatusLabel}</Body1>
                            </div>
                        )}
                    </Field>

                    <Divider className={styles.divider} />

                    <div className={styles.actionRow}>
                        <Button
                            appearance="primary"
                            disabled={isProcessing || !pendingFile}
                            onClick={() => pendingFile && handleFileSelect(pendingFile)}
                        >
                            {isProcessing ? <Spinner size="tiny" /> : 'Retry Upload'}
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
                    <Body1 className={styles.mappingSummary}>
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
                                            placeholder={formFieldsLoading ? 'Loading fields...' : 'Select form field...'}
                                            value={mapping.formField || ''}
                                            disabled={formFieldsLoading}
                                            onOptionSelect={(_, data) =>
                                                handleMappingChange(mapping.excelColumn, data.optionValue as string)
                                            }
                                        >
                                            <Option value="">-- Unmapped --</Option>
                                            {formFields.map(field => (
                                                <Option key={field.name} value={field.name}>
                                                    {field.label}
                                                </Option>
                                            ))}
                                        </Dropdown>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>

                    <Divider className={styles.divider} />

                    <div className={styles.actionRowEnd}>
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
                    <Body1 className={styles.mappingSummary}>
                        Review your imported data before submitting.
                    </Body1>

                    <div className={styles.previewBox}>
                        <Title3 block>Imported Data Preview</Title3>
                        <Body1 block>
                            {mappings.filter(m => m.formField).length} columns will be mapped to form fields
                        </Body1>
                    </div>

                    <div className={styles.mappingSummary}>
                        <Subtitle2 block>Mapping Summary</Subtitle2>
                        <ul>
                            {mappings.filter(m => m.formField).map(m => (
                                <li key={m.excelColumn}>
                                    {m.excelColumn} → {m.formField}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <Divider className={styles.divider} />

                    <div className={styles.actionRowEnd}>
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
