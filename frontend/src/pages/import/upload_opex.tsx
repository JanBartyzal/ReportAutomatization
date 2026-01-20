import React from 'react';
import {
    Title2, Card, CardHeader, Text,
    LargeTitle, Body1, Button
} from '@fluentui/react-components';
import { useNavigate } from 'react-router-dom';
import { Add24Regular } from '@fluentui/react-icons';

export const Dashboard: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title2>Vítejte v CloudInfraMap</Title2>
                <Button appearance="primary" icon={<Add24Regular />} onClick={() => navigate('/import/source')}>
                    Analyzovat nový projekt
                </Button>
            </div>
        </div>
    );
};