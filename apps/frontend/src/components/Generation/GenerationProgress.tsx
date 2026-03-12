import React from 'react';
import { ProgressBar, Text, Badge } from '@fluentui/react-components';
import { reportBrand } from '../../theme/brandTokens';
import { GenerationStatus } from '../../api/generation';

interface GenerationProgressProps {
    status: GenerationStatus;
    progress?: number;
    current?: number;
    total?: number;
}

export const GenerationProgress: React.FC<GenerationProgressProps> = ({
    status,
    progress,
    current,
    total,
}) => {
    const isIndeterminate = status === 'PROCESSING' && progress === undefined;
    const progressValue = progress ?? (current !== undefined && total !== undefined
        ? (current / total) * 100
        : -1);

    const getStatusLabel = () => {
        switch (status) {
            case 'PENDING':
                return 'Waiting to start...';
            case 'PROCESSING':
                if (current !== undefined && total !== undefined) {
                    return `Processing ${current} of ${total}...`;
                }
                return 'Generating report...';
            case 'COMPLETED':
                return 'Completed';
            case 'FAILED':
                return 'Failed';
            default:
                return '';
        }
    };

    const getStatusColor = () => {
        switch (status) {
            case 'PENDING':
                return '#F7B500'; // Warning
            case 'PROCESSING':
                return reportBrand[90]; // Brand
            case 'COMPLETED':
                return '#107C10'; // Success
            case 'FAILED':
                return '#C4314B'; // Danger (brand color)
            default:
                return '#666';
        }
    };

    return (
        <div style={{ width: '100%', padding: '16px 0' }}>
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '8px'
            }}>
                <Text weight="semibold">{getStatusLabel()}</Text>
                <Badge
                    appearance="filled"
                    color={
                        status === 'COMPLETED' ? 'success' :
                            status === 'FAILED' ? 'danger' :
                                status === 'PROCESSING' ? 'info' :
                                    'warning'
                    }
                >
                    {status}
                </Badge>
            </div>

            <ProgressBar
                thickness="medium"
                value={progressValue}
                style={{
                    '--progress-bar-color': getStatusColor(),
                } as React.CSSProperties}
            />

            {current !== undefined && total !== undefined && (
                <Text size={100} style={{ display: 'block', marginTop: '8px', color: '#666' }}>
                    {current} / {total} reports
                </Text>
            )}
        </div>
    );
};

export default GenerationProgress;
