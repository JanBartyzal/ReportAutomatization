import { useFailedJobs, useReprocessFailedJob } from '../../hooks/useAdmin';
import { Body1, Button, Spinner } from '@fluentui/react-components';

const FailedJobsPanel: React.FC = () => {
    const { data, isLoading, error } = useFailedJobs();
    const reprocessJob = useReprocessFailedJob();

    if (isLoading) {
        return <Spinner label="Loading failed jobs..." />;
    }

    if (error) {
        return <Body1>Error loading failed jobs: {error.message}</Body1>;
    }

    return (
        <div>
            <Body1>Failed Jobs (DLQ)</Body1>
            <p>Failed job reprocessing functionality - implementation in progress</p>
        </div>
    );
};

export default FailedJobsPanel;
