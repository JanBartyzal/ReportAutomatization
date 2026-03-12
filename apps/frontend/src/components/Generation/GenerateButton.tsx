import React, { useState } from 'react';
import {
    Button,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    DialogLoadingState,
    Spinner,
    ProgressBar,
    Badge,
} from '@fluentui/react-components';
import {
    DocumentPdf24Regular,
    ArrowDownload24Regular,
    ArrowSync24Regular,
} from '@fluentui/react-icons';
import { useGenerateReport, useGenerationPolling } from '../../hooks/useGeneration';
import { reportBrand } from '../../theme/brandTokens';
import { GenerationStatus } from '../../api/generation';

// Status badge colors per design system
const getStatusBadge = (status: GenerationStatus) => {
    const statusConfig = {
        PENDING: { appearance: 'filled' as const, color: 'warning' as const, label: 'Pending' },
        PROCESSING: { appearance: 'filled' as const, color: 'info' as const, label: 'Processing' },
        COMPLETED: { appearance: 'filled' as const, color: 'success' as const, label: 'Completed' },
        FAILED: { appearance: 'filled' as const, color: 'danger' as const, label: 'Failed' },
    };
    const config = statusConfig[status];
    return <Badge appearance={config.appearance} color={config.color}>{config.label}</Badge>;
};

interface GenerateButtonProps {
    reportId: string;
    reportStatus: 'APPROVED' | 'DRAFT' | 'SUBMITTED' | 'REJECTED';
    templateId?: string;
    existingJobId?: string;
    existingDownloadUrl?: string;
}

export const GenerateButton: React.FC<GenerateButtonProps> = ({
    reportId,
    reportStatus,
    templateId,
    existingJobId,
    existingDownloadUrl,
}) => {
    const [jobId, setJobId] = useState<string | undefined>(existingJobId);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const generateMutation = useGenerateReport();

    const handleGenerate = async () => {
        try {
            const response = await generateMutation.mutateAsync({
                reportId,
                templateId,
            });
            setJobId(response.jobId);
        } catch (error) {
            console.error('Failed to start generation:', error);
        }
    };

    const { status, downloadUrl, isPolling } = useGenerationPolling(
        reportId,
        jobId || undefined,
        !!jobId,
        () => setIsDialogOpen(false),
        () => setIsDialogOpen(false)
    );

    const handleDownload = () => {
        if (downloadUrl) {
            window.open(downloadUrl, '_blank');
        }
    };

    // Only show button when report is APPROVED
    if (reportStatus !== 'APPROVED') {
        return null;
    }

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {/* Status Badge (if generation exists) */}
            {existingDownloadUrl && !jobId && (
                <Badge appearance="filled" color="success">Generated</Badge>
            )}

            {/* Download Button (if already generated) */}
            {existingDownloadUrl && (
                <Button
                    appearance="primary"
                    icon={<ArrowDownload24Regular />}
                    onClick={handleDownload}
                    style={{ backgroundColor: reportBrand[90] }}
                >
                    Download PPTX
                </Button>
            )}

            {/* Generate/Regenerate Button */}
            <Dialog open={isDialogOpen} onOpenChange={(_, d) => setIsDialogOpen(d.open)}>
                <DialogTrigger disableButtonEnhancement>
                    <Button
                        appearance="primary"
                        icon={existingDownloadUrl ? <ArrowSync24Regular /> : <DocumentPdf24Regular />}
                        onClick={() => setIsDialogOpen(true)}
                        style={{ backgroundColor: reportBrand[90] }}
                    >
                        {existingDownloadUrl ? 'Regenerate' : 'Generate PPTX'}
                    </Button>
                </DialogTrigger>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>
                            {status === 'PROCESSING' || status === 'COMPLETED' ? 'Generating Report...' : 'Generate PowerPoint Report'}
                        </DialogTitle>
                        <DialogContent>
                            {status === 'PROCESSING' && (
                                <div style={{ padding: '20px 0' }}>
                                    <ProgressBar
                                        progress={{ value: -1, label: 'Generating...' }}
                                        thickness="medium"
                                        style={{
                                            '--progress-bar-color': reportBrand[90]
                                        } as React.CSSProperties}
                                    />
                                    <p style={{ marginTop: '12px', color: '#666' }}>
                                        Please wait while your report is being generated...
                                    </p>
                                </div>
                            )}

                            {status === 'COMPLETED' && (
                                <div style={{ padding: '20px 0', textAlign: 'center' }}>
                                    <DocumentPdf24Regular style={{ fontSize: '48px', color: reportBrand[90] }} />
                                    <p style={{ marginTop: '12px', fontWeight: 'bold', color: '#2d8a2d' }}>
                                        Report generated successfully!
                                    </p>
                                </div>
                            )}

                            {status === 'FAILED' && (
                                <div style={{ padding: '20px 0', textAlign: 'center' }}>
                                    <p style={{ color: '#C4314B' }}>
                                        Failed to generate report. Please try again.
                                    </p>
                                </div>
                            )}

                            {status === 'PENDING' && (
                                <p>
                                    This will generate a PowerPoint report using the configured template.
                                    The generation process may take a few minutes.
                                </p>
                            )}
                        </DialogContent>
                        <DialogActions>
                            {status === 'COMPLETED' ? (
                                <>
                                    <Button appearance="secondary" onClick={() => setIsDialogOpen(false)}>
                                        Close
                                    </Button>
                                    <Button
                                        appearance="primary"
                                        onClick={handleDownload}
                                        icon={<ArrowDownload24Regular />}
                                        style={{ backgroundColor: reportBrand[90] }}
                                    >
                                        Download
                                    </Button>
                                </>
                            ) : status === 'PROCESSING' ? (
                                <Button appearance="secondary" disabled>
                                    Processing...
                                </Button>
                            ) : (
                                <>
                                    <Button appearance="secondary" onClick={() => setIsDialogOpen(false)}>
                                        Cancel
                                    </Button>
                                    <Button
                                        appearance="primary"
                                        onClick={handleGenerate}
                                        disabled={generateMutation.isPending}
                                        style={{ backgroundColor: reportBrand[90] }}
                                    >
                                        {generateMutation.isPending ? 'Starting...' : 'Generate'}
                                    </Button>
                                </>
                            )}
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default GenerateButton;
