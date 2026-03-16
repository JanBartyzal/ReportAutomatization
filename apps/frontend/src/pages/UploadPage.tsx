import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDropzone } from 'react-dropzone';
import {
    Title2,
    Body1,
    ProgressBar,
    makeStyles,
    tokens,
    RadioGroup,
    Radio,
    Label,
    Select,
    Option,
} from '@fluentui/react-components';
import { ArrowUpload24Regular } from '@fluentui/react-icons';
import { useUpload } from '../hooks/useFiles';
import { useBatches, useAddFileToBatch } from '../hooks/useBatches';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '800px',
    },
    title: {
        marginBottom: tokens.spacingHorizontalL,
    },
    description: {
        marginBottom: tokens.spacingHorizontalM,
    },
    dropzone: {
        border: `2px dashed ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        padding: '48px',
        textAlign: 'center',
        cursor: 'pointer',
        transition: 'border-color 0.2s ease-in-out',
        backgroundColor: tokens.colorNeutralBackground1,
        '&:hover': {
            border: `2px dashed ${tokens.colorBrandForeground1}`,
        },
    },
    dropzoneActive: {
        border: `2px dashed ${tokens.colorBrandForeground1}`,
        backgroundColor: tokens.colorBrandBackground2,
    },
    uploadIcon: {
        fontSize: '48px',
        marginBottom: tokens.spacingHorizontalM,
        color: tokens.colorBrandForeground1,
    },
    supportedText: {
        marginTop: tokens.spacingVerticalS,
        color: tokens.colorNeutralForeground2,
    },
    progressSection: {
        marginTop: tokens.spacingHorizontalL,
    },
    progressText: {
        marginTop: tokens.spacingVerticalS,
    },
    errorText: {
        marginTop: tokens.spacingHorizontalM,
        color: tokens.colorPaletteRedForeground1,
    },
});

export default function UploadPage() {
    const styles = useStyles();
    const navigate = useNavigate();
    const uploadMutation = useUpload();
    const addFileToBatch = useAddFileToBatch();
    const { data: batches } = useBatches();
    const [uploadProgress, setUploadProgress] = useState<number | null>(null);
    const [selectedPurpose, setSelectedPurpose] = useState<string>('PARSE');
    const [selectedBatchId, setSelectedBatchId] = useState<string>('');

    const onDrop = useCallback(async (acceptedFiles: File[]) => {
        for (const file of acceptedFiles) {
            try {
                const result = await uploadMutation.mutateAsync({
                    file,
                    purpose: selectedPurpose as any,
                    onProgress: (event) => {
                        if (event.total) {
                            setUploadProgress((event.loaded / event.total) * 100);
                        }
                    },
                });
                setUploadProgress(null);

                // Auto-assign to batch if selected
                if (selectedBatchId && result?.file_id) {
                    try {
                        await addFileToBatch.mutateAsync({
                            batchId: selectedBatchId,
                            fileId: result.file_id,
                        });
                    } catch (batchError) {
                        console.error('Failed to assign file to batch:', batchError);
                    }
                }

                navigate('/files');
            } catch (error) {
                console.error('Upload failed:', error);
                setUploadProgress(null);
            }
        }
    }, [uploadMutation, selectedPurpose, selectedBatchId, addFileToBatch, navigate]);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        accept: {
            'application/vnd.openxmlformats-officedocument.presentationml.presentation': ['.pptx'],
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
            'application/pdf': ['.pdf'],
            'text/csv': ['.csv'],
        },
        maxSize: 100 * 1024 * 1024, // 100MB
    });

    return (
        <div className={styles.container}>
            <Title2 className={styles.title}>Upload Files</Title2>
            <Body1 className={styles.description}>
                Upload PPTX, XLSX, PDF, or CSV files for processing.
            </Body1>

            <div style={{ marginBottom: tokens.spacingHorizontalL }}>
                <Label style={{ display: 'block', marginBottom: tokens.spacingVerticalS }}>
                    Upload Purpose
                </Label>
                <RadioGroup
                    value={selectedPurpose}
                    onChange={(_, data) => setSelectedPurpose(data.value)}
                    layout="horizontal"
                >
                    <Radio value="PARSE" label="Data Parsing" />
                    <Radio value="FORM_IMPORT" label="Form Import" />
                </RadioGroup>
            </div>

            <div style={{ marginBottom: tokens.spacingHorizontalL }}>
                <Label style={{ display: 'block', marginBottom: tokens.spacingVerticalS }}>
                    Assign to Batch (optional)
                </Label>
                <Select
                    value={selectedBatchId}
                    onChange={(_: any, data: any) => setSelectedBatchId(data.value)}
                >
                    <Option value="">— No batch —</Option>
                    {(batches || []).map((batch: any) => (
                        <Option key={batch.id} value={batch.id} text={`${batch.name} (${batch.period}) — ${batch.status}`}>
                            {batch.name} ({batch.period}) — {batch.status}
                        </Option>
                    ))}
                </Select>
            </div>

            <div
                {...getRootProps()}
                className={`${styles.dropzone} ${isDragActive ? styles.dropzoneActive : ''}`}
            >
                <input {...getInputProps()} />
                <ArrowUpload24Regular className={styles.uploadIcon} />
                <Body1 block>
                    {isDragActive
                        ? 'Drop the files here...'
                        : 'Drag and drop files here, or click to select files'}
                </Body1>
                <Body1 block className={styles.supportedText}>
                    Supported: .pptx, .xlsx, .pdf, .csv (max 100MB)
                </Body1>
            </div>

            {uploadProgress !== null && (
                <div className={styles.progressSection}>
                    <ProgressBar value={uploadProgress} />
                    <Body1 className={styles.progressText}>Uploading... {Math.round(uploadProgress)}%</Body1>
                </div>
            )}

            {uploadMutation.isError && (
                <Body1 className={styles.errorText}>
                    Upload failed. Please try again.
                </Body1>
            )}
        </div>
    );
}
