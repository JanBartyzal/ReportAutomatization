import React from 'react';
import { Spinner } from '@fluentui/react-components';

interface LoadingSpinnerProps {
    label?: string;
}

export default function LoadingSpinner({ label = 'Loading...' }: LoadingSpinnerProps) {
    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '200px' }}>
            <Spinner label={label} />
        </div>
    );
}
