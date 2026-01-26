import React from 'react';
import {
    Title2, Card, CardHeader, Text,
    LargeTitle, Body1,
} from '@fluentui/react-components';
import { useFiles } from '../../api/files';

export const OpexDashboard: React.FC = () => {
    const { data: files, isLoading, isError } = useFiles();


    if (isLoading) {
        return <div className="p-4">Načítám data...</div>;
    }

    if (isError) {
        return <div className="p-4">Něco se pokazilo...</div>;
    }

    return (
        <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title2>Opex Dashboard</Title2>
            </div>

            {/* Statistics */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>

                {files?.length > 0 ? (

                    <Card>
                        <CardHeader header={<Text weight="semibold">Zpracované soubory</Text>} />
                        <LargeTitle>{files?.length || 0}</LargeTitle>
                        <Body1>Celkem nahraných souborů</Body1>
                    </Card>
                ) : (
                    <Card>
                        <CardHeader header={<Text weight="semibold">Zpracované soubory</Text>} />
                        <LargeTitle>0</LargeTitle>
                        <Body1>Celkem nahraných souborů</Body1>
                    </Card>
                )}
            </div>
        </div>
    );
};