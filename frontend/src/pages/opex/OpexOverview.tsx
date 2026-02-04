import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    makeStyles,
    Title1,
    Button,
    Card,
    CardHeader,
    Text,
    Tab,
    TabList,
    SelectTabData,
    SelectTabEvent,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Badge,
    Spinner,
} from '@fluentui/react-components';
import {
    Add24Regular,
    ArrowUpload24Regular,
    DocumentTableSearch24Regular,
    Play24Regular
} from '@fluentui/react-icons';
import { useFiles } from '../../api/files';
import { api } from '../../api/endpoints';
import {
    Toast,
    ToastTitle,
    ToastBody,
    useToastController,
    Toaster
} from '@fluentui/react-components';
import { format } from 'date-fns';

const useStyles = makeStyles({
    container: {
        padding: '2rem',
        maxWidth: '1200px',
        margin: '0 auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '2rem',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    actions: {
        display: 'flex',
        gap: '1rem',
    },
    card: {
        padding: '0',
    },
    tableContainer: {
        overflowX: 'auto',
    }
});

export const OpexOverview: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const { dispatchToast } = useToastController('toaster');
    const [selectedTab, setSelectedTab] = useState<string>('files');
    const { data: files, isLoading: isLoadingFiles } = useFiles();
    const [runningImportId, setRunningImportId] = useState<number | null>(null);

    const handleImport = async (fileId: number) => {
        setRunningImportId(fileId);
        try {
            await api.opex.runProceedOpex(fileId.toString());
            dispatchToast(
                <Toast>
                    <ToastTitle>Import Started</ToastTitle>
                    <ToastBody>Process started in background.</ToastBody>
                </Toast>,
                { intent: 'success' }
            );
        } catch (error) {
            console.error("Import failed", error);
            dispatchToast(
                <Toast>
                    <ToastTitle>Import Failed</ToastTitle>
                    <ToastBody>Could not start import process.</ToastBody>
                </Toast>,
                { intent: 'error' }
            );
        } finally {
            setRunningImportId(null);
        }
    };

    const onTabSelect = (event: SelectTabEvent, data: SelectTabData) => {
        setSelectedTab(data.value as string);
    };

    return (
        <div className={styles.container}>
            <Toaster toasterId="toaster" />
            <div className={styles.header}>
                <Title1>Opex Management</Title1>
                <div className={styles.actions}>
                    <Button
                        appearance="secondary"
                        icon={<Add24Regular />}
                        onClick={() => navigate('/opex/new-batch')}
                    >
                        New Batch
                    </Button>
                    <Button
                        appearance="primary"
                        icon={<ArrowUpload24Regular />}
                        onClick={() => navigate('/import/upload')}
                    >
                        Upload Files
                    </Button>
                </div>
            </div>

            <Card className={styles.card}>
                <div style={{ padding: '0 1rem' }}>
                    <TabList selectedValue={selectedTab} onTabSelect={onTabSelect}>
                        <Tab value="files">Uploaded Files</Tab>
                        <Tab value="batches">Batches (Coming Soon)</Tab>
                    </TabList>
                </div>

                <div className={styles.tableContainer}>
                    {selectedTab === 'files' && (
                        isLoadingFiles ? (
                            <div style={{ padding: '2rem', textAlign: 'center' }}>
                                <Spinner label="Loading files..." />
                            </div>
                        ) : (
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHeaderCell>Filename</TableHeaderCell>
                                        <TableHeaderCell>Batch ID</TableHeaderCell>
                                        <TableHeaderCell>Region</TableHeaderCell>
                                        <TableHeaderCell>Uploaded At</TableHeaderCell>
                                        <TableHeaderCell>Status</TableHeaderCell>
                                        <TableHeaderCell>Actions</TableHeaderCell>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {files?.map((file) => (
                                        <TableRow key={file.id}>
                                            <TableCell>
                                                <Text weight="semibold">{file.filename}</Text>
                                            </TableCell>
                                            {/* Assuming API files return batch_id, if not, it will be empty for now until updated */}
                                            <TableCell>{(file as any).batch_id || '-'}</TableCell>
                                            <TableCell>{file.region || 'N/A'}</TableCell>
                                            <TableCell>
                                                {file.created_at ? format(new Date(file.created_at), 'MMM d, yyyy HH:mm') : '-'}
                                            </TableCell>
                                            <TableCell>
                                                <Badge appearance="tint" color="success">Processed</Badge>
                                            </TableCell>
                                            <TableCell>
                                                <Button
                                                    icon={<DocumentTableSearch24Regular />}
                                                    appearance="subtle"
                                                    onClick={() => navigate(`/opex/view/${file.id}`)}
                                                >
                                                    View
                                                </Button>
                                                <Button
                                                    appearance="subtle"
                                                    style={{ color: 'blue' }}
                                                    disabled={runningImportId === file.id}
                                                    icon={runningImportId === file.id ? <Spinner size="tiny" /> : <Play24Regular />}
                                                    onClick={() => handleImport(file.id)}
                                                >
                                                    {runningImportId === file.id ? 'Running...' : 'Run Import'}
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {files?.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={6} style={{ textAlign: 'center', padding: '2rem' }}>
                                                No files uploaded yet.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        )
                    )}
                    {selectedTab === 'batches' && (
                        <div style={{ padding: '2rem', textAlign: 'center', color: 'gray' }}>
                            Batch list implementation coming in next iteration.
                            Use "New Batch" to create one.
                        </div>
                    )}
                </div>
            </Card>
        </div>
    );
};
