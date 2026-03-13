import React from 'react';
import { ProgressBar, Text, Badge, makeStyles, tokens, Caption1 } from '@fluentui/react-components';

import { GenerationStatus } from '../../api/generation';

const useStyles = makeStyles({
  root: {
    width: '100%',
    paddingTop: '16px',
    paddingBottom: '16px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  counter: {
    display: 'block',
    marginTop: '8px',
    color: tokens.colorNeutralForeground3,
  },
});

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
    const styles = useStyles();
    // const isIndeterminate = status === 'PROCESSING' && progress === undefined;
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



    return (
        <div className={styles.root}>
            <div className={styles.header}>
                <Text weight="semibold">{getStatusLabel()}</Text>
                <Badge
                    appearance="filled"
                    color={
                        status === 'COMPLETED' ? 'success' :
                            status === 'FAILED' ? 'danger' :
                                status === 'PROCESSING' ? 'informative' :
                                    'warning'
                    }
                >
                    {status}
                </Badge>
            </div>

            <ProgressBar
                thickness="medium"
                value={progressValue}
            />

            {current !== undefined && total !== undefined && (
                <Caption1 className={styles.counter}>
                    {current} / {total} reports
                </Caption1>
            )}
        </div>
    );
};

export default GenerationProgress;
