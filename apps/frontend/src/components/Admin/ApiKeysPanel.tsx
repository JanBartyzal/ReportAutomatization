import React from 'react';
import { useApiKeys } from '../../hooks/useAdmin';
import { Body1, Spinner } from '@fluentui/react-components';

const ApiKeysPanel: React.FC = () => {
    const { data: _keys, isLoading, error } = useApiKeys();

    if (isLoading) {
        return <Spinner label="Loading API keys..." />;
    }

    if (error) {
        return <Body1>Error loading API keys: {error.message}</Body1>;
    }

    return (
        <div>
            <Body1>API Key Management</Body1>
            <p>API key generation and revocation - implementation in progress</p>
        </div>
    );
};

export default ApiKeysPanel;
