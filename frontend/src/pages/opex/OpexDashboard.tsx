import React, { useEffect, useState } from 'react';
import {
    Title2, Card, CardHeader, Text,
    LargeTitle, Body1, Button, Spinner,
    Select
} from '@fluentui/react-components';
import { ArrowLeft24Regular, ArrowRight24Regular } from '@fluentui/react-icons';
import { api } from '../../api/endpoints';

export const OpexDashboard: React.FC = () => {
    const [files, setFiles] = useState<any[]>([]);
    useEffect(() => {
        loadFiles();
    }, []);

    const loadFiles = async () => {
        try {
            const data = await api.opex.listUploadedFiles();
            setFiles(data);
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title2>Opex Dashboard</Title2>
            </div>

            {/* Statistics */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
                <Card>
                    <CardHeader header={<Text weight="semibold">Zpracované soubory</Text>} />
                    <LargeTitle>{files.length}</LargeTitle>
                    <Body1>Celkem nahraných souborů</Body1>
                </Card>
            </div>
        </div>
    );
};