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
                <Title2>Welcome in Report Automatization tool</Title2>
                <Button appearance="primary" icon={<Add24Regular />} onClick={() => navigate('/import/opex')}>
                    Add new source
                </Button>
            </div>

            {/* Rychlé statistiky */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
                <Card>
                    <CardHeader header={<Text weight="semibold">Last updated</Text>} />
                    <LargeTitle>CzechDemo</LargeTitle>
                    <Body1>demo</Body1>
                </Card>
                <Card>
                    <CardHeader header={<Text weight="semibold">Stats</Text>} />
                    <LargeTitle>12.000</LargeTitle>
                    <Body1>Za tento měsíc</Body1>
                </Card>

            </div>

            {/* Seznam posledních (Placeholder) */}
            <Title2>History</Title2>
            <Card>
                <div style={{ padding: '1rem' }}>
                    <Text>Zde bude tabulka posledních změn</Text>
                </div>
            </Card>
        </div>
    );
};