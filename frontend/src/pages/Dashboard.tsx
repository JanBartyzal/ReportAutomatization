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

            {/* Rychlé statistiky */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
                <Card>
                    <CardHeader header={<Text weight="semibold">Poslední aktivní plán</Text>} />
                    <LargeTitle>$1,250.00</LargeTitle>
                    <Body1>Terraform Production (v.3)</Body1>
                </Card>
                <Card>
                    <CardHeader header={<Text weight="semibold">Analyzované projekty</Text>} />
                    <LargeTitle>12</LargeTitle>
                    <Body1>Za tento měsíc</Body1>
                </Card>
                <Card>
                    <CardHeader header={<Text weight="semibold">Status Workerů</Text>} />
                    <LargeTitle style={{ color: 'green' }}>Online</LargeTitle>
                    <Body1>Dapr Sidecars připraveny</Body1>
                </Card>
            </div>

            {/* Seznam posledních (Placeholder) */}
            <Title2>Nedávná historie</Title2>
            <Card>
                <div style={{ padding: '1rem' }}>
                    <Text>Zde bude tabulka posledních 5 analýz...</Text>
                </div>
            </Card>
        </div>
    );
};