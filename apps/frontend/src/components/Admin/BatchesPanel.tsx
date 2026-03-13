import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Body1, Spinner } from '@fluentui/react-components';

const BatchesPanel: React.FC = () => {
    // Placeholder for batch API - would use proper API client
    const { isLoading, error } = useQuery({
        queryKey: ['batches'],
        queryFn: async () => {
            // API call would go here
            return [];
        }
    });

    if (isLoading) {
        return <Spinner label="Loading batches..." />;
    }

    if (error) {
        return <Body1>Error loading batches: {error.message}</Body1>;
    }

    return (
        <div>
            <Body1>Batch Management</Body1>
            <p>Batch creation and management functionality - implementation in progress</p>
        </div>
    );
};

export default BatchesPanel;
