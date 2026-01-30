
import React, { useState } from 'react';
import {
    makeStyles,
    shorthands,
    tokens,
    Title3,
    Text,
    Dropdown,
    Option,
    OptionOnSelectData,
    SelectionEvents,
    Toast,
    ToastTitle,
    ToastBody,
    useToastController,
    Toaster,
} from '@fluentui/react-components';
import { FileUploader } from '../../components/FileUploader';
import { useMyReports, useUploadAppendix } from '../../api/files';

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        gap: '2rem',
        padding: '2rem',
        maxWidth: '800px',
        margin: '0 auto',
    },
    section: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
    },
    dropdown: {
        minWidth: '300px',
    },
});

export const ExcelImport: React.FC = () => {
    const styles = useStyles();
    const { dispatchToast } = useToastController('toaster');

    // State
    const [selectedReportId, setSelectedReportId] = useState<number | null>(null);

    // Queries & Mutations
    const { data: reports, isLoading: isLoadingReports } = useMyReports();
    const uploadMutation = useUploadAppendix();

    // Handlers
    const handleReportChange = (e: SelectionEvents, data: OptionOnSelectData) => {
        if (data.optionValue) {
            setSelectedReportId(parseInt(data.optionValue, 10));
        }
    };

    const handleUpload = (files: FileList) => {
        if (files.length === 0) return;
        if (!selectedReportId) {
            dispatchToast(
                <Toast>
                    <ToastTitle>Chyba</ToastTitle>
                    <ToastBody>Vyberte prosím nejprve report, ke kterému chcete data připojit.</ToastBody>
                </Toast>,
                { intent: 'error' }
            );
            return;
        }

        const file = files[0];
        // Validate extension in UI as well
        if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
            dispatchToast(
                <Toast>
                    <ToastTitle>Nesprávný formát</ToastTitle>
                    <ToastBody>Použijte prosím soubory .xlsx nebo .xls.</ToastBody>
                </Toast>,
                { intent: 'error' }
            );
            return;
        }

        uploadMutation.mutate(
            { reportId: selectedReportId, file },
            {
                onSuccess: (data) => {
                    dispatchToast(
                        <Toast>
                            <ToastTitle>Úspěch</ToastTitle>
                            <ToastBody>Excel byl úspěšně nahrán a zpracován. Zpracováno listů: {data.sheets_processed}</ToastBody>
                        </Toast>,
                        { intent: 'success' }
                    );
                },
                onError: (error) => {
                    console.error(error);
                    dispatchToast(
                        <Toast>
                            <ToastTitle>Chyba nahrávání</ToastTitle>
                            <ToastBody>Nepodařilo se nahrát soubor.</ToastBody>
                        </Toast>,
                        { intent: 'error' }
                    );
                }
            }
        );
    };

    return (
        <div className={styles.root}>
            <Toaster toasterId="toaster" />
            <Title3>Import Excel Dat (Opex)</Title3>

            <div className={styles.section}>
                <Text size={400}>1. Vyberte existující Report (Plan)</Text>

                {isLoadingReports ? (
                    <Text>Načítám reporty...</Text>
                ) : (
                    <Dropdown
                        placeholder="Vyberte report..."
                        className={styles.dropdown}
                        onOptionSelect={handleReportChange}
                    >
                        {reports?.map((report) => (
                            <Option key={report.id} value={report.id.toString()}>
                                {report.title ? `${report.title} (ID: ${report.id})` : `Report #${report.id}`}
                            </Option>
                        ))}
                    </Dropdown>
                )}
            </div>

            <div className={styles.section}>
                <Text size={400}>2. Nahrajte Excel soubor (.xlsx)</Text>
                <FileUploader
                    onUpload={handleUpload}
                    isLoading={uploadMutation.isPending}
                    accept=".xlsx,.xls"
                />
            </div>

            {selectedReportId && (
                <Text size={200} italic>
                    Vybrán report ID: {selectedReportId}. Nahráním souboru se přepíší existující data v appendixu.
                </Text>
            )}
        </div>
    );
};
