import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
    Title3, Card, Text,
    MessageBar, MessageBarBody,
    Link, makeStyles, tokens
} from '@fluentui/react-components';
import { FileUploader } from '../../components/FileUploader';
import { api } from '../../api/endpoints';

const useStyles = makeStyles({
    container: {
        maxWidth: '800px',
        margin: '2rem auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '2rem',
    },
    codeBlock: {
        backgroundColor: tokens.colorNeutralBackground2,
        padding: '1rem',
        borderRadius: tokens.borderRadiusMedium,
        fontFamily: 'monospace',
        fontSize: '0.9rem',
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        marginTop: '0.5rem',
    }
});

export const OpexPptxImport: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const [uploading, setUploading] = useState(false);
    const [activePlanId, setActivePlanId] = useState<string | null>(null);

    // 1. Upload Handler
    const handleUpload = async (files: FileList) => {
        setUploading(true);
        try {
            // Voláme endpoint pro upload (v endpointu /api/parser/upload)
            const plan = await api.import.uploadOpexPptx(files);
            setActivePlanId(plan.id);
        } catch (err) {
            alert("Chyba při nahrávání souboru. Zkontrolujte, zda jde o validní JSON.");
            setUploading(false);
        }
    };

    // 2. Polling statusu zpracování
    useQuery({
        queryKey: ['planStatus', activePlanId],
        queryFn: async () => {
            if (!activePlanId) return null;
            const status = await api.import.getStatus(activePlanId);

            // Jakmile je hotovo, přesměrujeme na detail
            if (status.status === 'completed') {
                navigate(`/plan/${activePlanId}`);
            }
            return status;
        },
        enabled: !!activePlanId, // Spustit jen když máme ID
        refetchInterval: 1000,   // Kontrola každou sekundu
    });

    return (
        <div className={styles.container}>
            <div>
                <Title3>Importovat Opex File (PPTX)</Title3>
                <Text block style={{ color: tokens.colorNeutralForeground2, marginTop: '0.5rem' }}>
                    Nahrajte powerpoint prezentaci ve formátu OPEX
                </Text>
            </div>

            <Card style={{ padding: '2rem' }}>
                <FileUploader
                    onUpload={handleUpload}
                    isLoading={uploading || !!activePlanId}
                    accept=".pptx"
                    multiple={false} // Zde chceme typicky jen jeden JSON soubor
                />

                {(uploading || activePlanId) && (
                    <Text align="center" style={{ marginTop: '1rem', color: tokens.colorBrandForeground1 }}>
                        Zpracovávám strukturu souboru...
                    </Text>
                )}
            </Card>
        </div>
    );
};