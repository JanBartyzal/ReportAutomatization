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
    ProgressBar,
    Badge,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    DocumentPdf24Regular,
    ArrowDownload24Regular,
    ArrowSync24Regular,
} from '@fluentui/react-icons';
import { useGenerateReport, useGenerationPolling } from '../../hooks/useGeneration';
import { reportBrand } from '../../theme/brandTokens';


const useStyles = makeStyles({
    wrapper: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    processingContent: {
        paddingTop: '20px',
        paddingBottom: '20px',
    },
    processingText: {
        marginTop: '12px',
        color: tokens.colorNeutralForeground3,
    },
    completedContent: {
        paddingTop: '20px',
        paddingBottom: '20px',
        textAlign: 'center',
    },
    successText: {
        marginTop: '12px',
        fontWeight: '700',
        color: tokens.colorStatusSuccessForeground1,
    },
    failedContent: {
        paddingTop: '20px',
        paddingBottom: '20px',
        textAlign: 'center',
    },
    failedText: {
        color: tokens.colorStatusDangerForeground1,
    },
});

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
    const styles = useStyles();
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

    const { status, downloadUrl } = useGenerationPolling(
        reportId,
        jobId || null,
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
        <div className={styles.wrapper}>
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
                                <div className={styles.processingContent}>
                                    <ProgressBar
                                        value={undefined}
                                        thickness="medium"
                                        style={{
                                            backgroundColor: tokens.colorNeutralBackground3,
                                        } as React.CSSProperties}
                                    />
                                    <p className={styles.processingText}>
                                        Please wait while your report is being generated...
                                    </p>
                                </div>
                            )}

                            {status === 'COMPLETED' && (
                                <div className={styles.completedContent}>
                                    <DocumentPdf24Regular style={{ fontSize: '48px', color: reportBrand[90] }} />
                                    <p className={styles.successText}>
                                        Report generated successfully!
                                    </p>
                                </div>
                            )}

                            {status === 'FAILED' && (
                                <div className={styles.failedContent}>
                                    <p className={styles.failedText}>
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
