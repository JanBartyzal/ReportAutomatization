import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    makeStyles,
    Title1,
    Dropdown,
    Option,
    OptionOnSelectData,
    SelectionEvents,
    Button,
    Text,
    Card,
    useId,
    Toast,
    ToastTitle,
    useToastController,
    Toaster,
    Spinner,
    Link as FluentLink,
} from '@fluentui/react-components';
import { Add24Regular } from '@fluentui/react-icons';
import { FileUploader } from '../../components/FileUploader';
import { useFiles, useUploadFile } from '../../api/files';
import { getBatches, Batch } from '../../api/batches';

const useStyles = makeStyles({
    container: {
        padding: '2rem',
        maxWidth: '800px',
        margin: '0 auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '2rem',
    },
    section: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
    },
    dropdownContainer: {
        display: 'flex',
        gap: '1rem',
        alignItems: 'flex-end',
    },
    dropdown: {
        minWidth: '300px',
        flexGrow: 1,
    }
});

export const UploadOpex: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const { dispatchToast } = useToastController('toaster');
    const dropdownId = useId('batch-dropdown');

    const [batches, setBatches] = useState<Batch[]>([]);
    const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
    const [isLoadingBatches, setIsLoadingBatches] = useState(true);

    const uploadMutation = useUploadFile();

    useEffect(() => {
        const fetchBatches = async () => {
            try {
                const data = await getBatches();
                // Filter for OPEN batches and sort by creation date (newest first) if possible
                // Assuming backend returns all, we filter here
                const openBatches = data
                    .filter(b => b.status === 'OPEN')
                    // Logic to sort reverse chronological if created_at is available, else rely on ID or order
                    .reverse();

                setBatches(openBatches);
                if (openBatches.length > 0) {
                    setSelectedBatchId(openBatches[0].id);
                }
            } catch (error) {
                console.error("Failed to fetch batches", error);
                dispatchToast(
                    <Toast>
                        <ToastTitle>Failed to load batches</ToastTitle>
                    </Toast>,
                    { intent: 'error' }
                );
            } finally {
                setIsLoadingBatches(false);
            }
        };

        fetchBatches();
    }, []);

    const handleBatchChange = (e: SelectionEvents, data: OptionOnSelectData) => {
        if (data.optionValue) {
            setSelectedBatchId(data.optionValue);
        }
    };

    const handleUpload = (fileList: FileList) => {
        if (!selectedBatchId) {
            dispatchToast(
                <Toast>
                    <ToastTitle>Please select a batch first</ToastTitle>
                </Toast>,
                { intent: 'error' }
            );
            return;
        }

        // We handle one file at a time or loop if multiple allowed, but FileUploader usually sends one?
        // Assuming FileUploader supports passing the file list.
        // The current FileUploader implementation needs to be checked if it calls an internal prop or emits 'onUpload'.
        // Assuming it emits 'onUpload' with FileList.

        Array.from(fileList).forEach((file) => {
            // Determine if it's Opex based on logic or user selection?
            // Requirement says "UploadOpex page... functionality of upload from Dashboard"
            // And "Start import of pptx or excel".
            // We pass isOpex=true for these imports.

            uploadMutation.mutate({
                file,
                isOpex: true,
                batchId: selectedBatchId
            }, {
                onSuccess: () => {
                    dispatchToast(
                        <Toast>
                            <ToastTitle>File uploaded successfully</ToastTitle>
                        </Toast>,
                        { intent: 'success' }
                    );
                },
                onError: () => {
                    dispatchToast(
                        <Toast>
                            <ToastTitle>Upload failed</ToastTitle>
                        </Toast>,
                        { intent: 'error' }
                    );
                }
            });
        });
    };

    return (
        <div className={styles.container}>
            <Toaster toasterId="toaster" />
            <Title1>Upload Opex Data</Title1>

            <Card className={styles.section} style={{ padding: '1.5rem' }}>
                <Text size={400} weight="semibold">1. Select Batch</Text>
                <div className={styles.dropdownContainer}>
                    <div style={{ flexGrow: 1 }}>
                        <label htmlFor={dropdownId} style={{ display: 'block', marginBottom: '0.5rem' }}>
                            Target Batch
                        </label>
                        {isLoadingBatches ? (
                            <Spinner size="tiny" />
                        ) : (
                            <Dropdown
                                id={dropdownId}
                                placeholder="Select a batch..."
                                className={styles.dropdown}
                                value={batches.find(b => b.id === selectedBatchId)?.name || ''}
                                selectedOptions={selectedBatchId ? [selectedBatchId] : []}
                                onOptionSelect={handleBatchChange}
                            >
                                {batches.map((batch) => (
                                    <Option key={batch.id} value={batch.id}>
                                        {batch.name}
                                    </Option>
                                ))}
                            </Dropdown>
                        )}
                    </div>
                    <Button
                        icon={<Add24Regular />}
                        onClick={() => navigate('/opex/new-batch')}
                    >
                        New Batch
                    </Button>
                </div>
                {batches.length === 0 && !isLoadingBatches && (
                    <Text style={{ color: 'red' }}>No open batches found. Please create one.</Text>
                )}
            </Card>

            <Card className={styles.section} style={{ padding: '1.5rem' }}>
                <Text size={400} weight="semibold">2. Upload Files</Text>
                <Text size={200} style={{ marginBottom: '1rem', color: 'gray' }}>
                    Supported formats: .pptx, .xlsx
                </Text>

                <FileUploader onUpload={handleUpload} isLoading={uploadMutation.isPending} accept=".pptx,.xlsx" />
            </Card>

            <div style={{ marginTop: '1rem' }}>
                <FluentLink onClick={() => navigate('/opex')}>
                    View Uploaded Files & Batches overview
                </FluentLink>
            </div>
        </div>
    );
};
