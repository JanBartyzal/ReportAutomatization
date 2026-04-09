import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle1,
    Body1,
    Caption1,
    Label,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    DataGrid,
    DataGridHeader,
    DataGridBody,
    DataGridRow,
    DataGridHeaderCell,
    DataGridCell,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
    Badge,
    Button,
    Card,
    CardHeader,
    CardPreview,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Input,
    Option,
    Select,
    Spinner,
} from '@fluentui/react-components';
import {
    Add24Regular,
    DocumentPdf24Regular,
} from '@fluentui/react-icons';
import { useTemplates, useUploadTemplate } from '../hooks/useTemplates';
import { reportBrand } from '../theme/brandTokens';
import { ScopeBadge } from '../components/ScopeBadge';

const useStyles = makeStyles({
    spinner: {
        display: 'flex',
        justifyContent: 'center',
        padding: '40px',
    },
    errorContainer: {
        padding: '20px',
    },
    errorText: {
        color: tokens.colorStatusDangerForeground1,
    },
    page: {
        padding: '20px',
        maxWidth: '1200px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '24px',
    },
    dialogForm: {
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
        paddingTop: '16px',
        paddingBottom: '16px',
    },
    dropzone: {
        border: `2px dashed ${tokens.colorNeutralStroke1}`,
        borderRadius: '8px',
        padding: '24px',
        textAlign: 'center',
    },
    dropLabel: {
        cursor: 'pointer',
    },
    uploadFileText: {
        display: 'block',
        marginTop: '8px',
    },
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
        gap: '16px',
    },
    cardClickable: {
        cursor: 'pointer',
    },
    cardHeaderRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    cardPreview: {
        height: '120px',
        backgroundColor: tokens.colorNeutralBackground2,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    emptyState: {
        textAlign: 'center',
        padding: '60px 20px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: '8px',
    },
    emptyText: {
        color: tokens.colorNeutralForeground3,
        marginTop: '8px',
    },
    emptyIcon: {
        color: tokens.colorNeutralForeground4,
    },
    allTemplatesSection: {
        marginTop: '32px',
    },
    allTemplatesTitle: {
        marginBottom: '16px',
    },
});

// Types
interface Template {
    id: string;
    name: string;
    version: number;
    scope: 'CENTRAL' | 'LOCAL';
    placeholderCount: number;
    createdAt: string;
    updatedAt: string;
}


export const TemplateListPage: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const { data: templates, isLoading, error } = useTemplates();
    const uploadMutation = useUploadTemplate();

    const columns: TableColumnDefinition<Template>[] = [
        createTableColumn<Template>({
            columnId: 'name',
            compare: (a: Template, b: Template) => a.name.localeCompare(b.name),
            renderHeaderCell: () => 'Name',
            renderCell: (item) => (
                <TableCellLayout>
                    <strong>{item.name}</strong>
                </TableCellLayout>
            ),
        }),
        createTableColumn<Template>({
            columnId: 'version',
            renderHeaderCell: () => 'Version',
            renderCell: (item: Template) => (
                <TableCellLayout>
                    <Badge appearance="filled" color="brand">
                        v{item.version}
                    </Badge>
                </TableCellLayout>
            ),
        }),
        createTableColumn<Template>({
            columnId: 'scope',
            renderHeaderCell: () => 'Scope',
            renderCell: (item: Template) => (
                <TableCellLayout>
                    <ScopeBadge scope={item.scope} />
                </TableCellLayout>
            ),
        }),
        createTableColumn<Template>({
            columnId: 'placeholders',
            renderHeaderCell: () => 'Placeholders',
            renderCell: (item: Template) => (
                <TableCellLayout>
                    <Badge appearance="filled" shape="rounded">
                        {item.placeholderCount}
                    </Badge>
                </TableCellLayout>
            ),
        }),
        createTableColumn<Template>({
            columnId: 'updatedAt',
            renderHeaderCell: () => 'Last Updated',
            renderCell: (item: Template) => (
                <TableCellLayout>
                    {new Date(item.updatedAt).toLocaleDateString()}
                </TableCellLayout>
            ),
        }),
        createTableColumn<Template>({
            columnId: 'actions',
            renderHeaderCell: () => '',
            renderCell: (item) => (
                <TableCellLayout>
                    <Button
                        appearance="subtle"
                        onClick={() => navigate(`/templates/${item.id}`)}
                    >
                        View Details
                    </Button>
                </TableCellLayout>
            ),
        }),
    ];

    const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false);
    const [uploadName, setUploadName] = useState('');
    const [uploadScope, setUploadScope] = useState<'CENTRAL' | 'LOCAL'>('CENTRAL');
    const [uploadFile, setUploadFile] = useState<File | null>(null);

    const handleUpload = async () => {
        if (!uploadFile || !uploadName) return;

        const formData = new FormData();
        formData.append('file', uploadFile);
        formData.append('name', uploadName);
        formData.append('scope', uploadScope);

        try {
            await uploadMutation.mutateAsync(formData);
            setIsUploadDialogOpen(false);
            setUploadName('');
            setUploadFile(null);
        } catch (err) {
            console.error('Upload failed:', err);
        }
    };

    if (isLoading) {
        return (
            <div className={styles.page}>
                <div className={styles.spinner}>
                    <Spinner label="Loading templates..." />
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className={styles.page}>
                <div className={styles.errorContainer}>
                    <Body1 className={styles.errorText}>
                        Error loading templates: {(error as Error).message}
                    </Body1>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
                {/* Header */}
                <div className={styles.header}>
                    <div>
                        <Title3 block style={{ color: reportBrand[90] }}>PPTX Templates</Title3>
                        <Subtitle1 block>Manage PowerPoint report templates</Subtitle1>
                    </div>
                    <Dialog open={isUploadDialogOpen} onOpenChange={(_, d) => setIsUploadDialogOpen(d.open)}>
                        <DialogTrigger disableButtonEnhancement>
                            <Button
                                appearance="primary"
                                icon={<Add24Regular />}
                            >
                                Upload Template
                            </Button>
                        </DialogTrigger>
                        <DialogSurface>
                            <DialogBody>
                                <DialogTitle>Upload PPTX Template</DialogTitle>
                                <DialogContent>
                                    <div className={styles.dialogForm}>
                                        <Label htmlFor="template-name">Template Name</Label>
                                        <Input
                                            id="template-name"
                                            value={uploadName}
                                            onChange={(_e: any, d: any) => setUploadName(d.value)}
                                            placeholder="Enter template name"
                                        />
                                        <Label htmlFor="template-scope">Scope</Label>
                                        <Select
                                            id="template-scope"
                                            value={uploadScope}
                                            onChange={(_e: any, d: any) => setUploadScope(d.value as 'CENTRAL' | 'LOCAL')}
                                        >
                                            <Option value="CENTRAL">Central (HoldingAdmin)</Option>
                                            <Option value="LOCAL">Local</Option>
                                        </Select>
                                        <div className={styles.dropzone}>
                                            <input
                                                type="file"
                                                accept=".pptx"
                                                onChange={(e) => setUploadFile((e.target as HTMLInputElement).files?.[0] || null)}
                                                style={{ display: 'none' }}
                                                id="template-upload"
                                            />
                                            <label htmlFor="template-upload" className={styles.dropLabel}>
                                                <DocumentPdf24Regular style={{ fontSize: '32px', color: reportBrand[90] }} />
                                                <Body1 className={styles.uploadFileText}>
                                                    {uploadFile ? uploadFile.name : 'Click to select PPTX file'}
                                                </Body1>
                                            </label>
                                        </div>
                                    </div>
                                </DialogContent>
                                <DialogActions>
                                    <Button appearance="secondary" onClick={() => setIsUploadDialogOpen(false)}>
                                        Cancel
                                    </Button>
                                    <Button
                                        appearance="primary"
                                        onClick={handleUpload}
                                        disabled={!uploadFile || !uploadName || uploadMutation.isPending}
                                    >
                                        {uploadMutation.isPending ? 'Uploading...' : 'Upload'}
                                    </Button>
                                </DialogActions>
                            </DialogBody>
                        </DialogSurface>
                    </Dialog>
                </div>

                {/* Template Cards Grid */}
                {templates && templates.length > 0 ? (
                    <div className={styles.grid}>
                        {templates.map((template) => (
                            <Card
                                key={template.id}
                                onClick={() => navigate(`/templates/${template.id}`)}
                                className={styles.cardClickable}
                            >
                                <CardHeader
                                    header={
                                        <div className={styles.cardHeaderRow}>
                                            <Subtitle1>{template.name}</Subtitle1>
                                            <Badge appearance="filled" color="brand">v{template.version}</Badge>
                                        </div>
                                    }
                                    description={
                                        <Caption1>
                                            {template.placeholderCount} placeholders • {template.scope}
                                        </Caption1>
                                    }
                                />
                                <CardPreview className={styles.cardPreview}>
                                    <DocumentPdf24Regular style={{ fontSize: '48px', color: reportBrand[80] }} />
                                </CardPreview>
                            </Card>
                        ))}
                    </div>
                ) : (
                    <div className={styles.emptyState}>
                        <DocumentPdf24Regular style={{ fontSize: '64px' }} className={styles.emptyIcon} />
                        <Title3 style={{ marginTop: '16px' }}>No templates yet</Title3>
                        <Body1 className={styles.emptyText}>
                            Upload your first PPTX template to get started
                        </Body1>
                    </div>
                )}

                {/* Data Grid for detailed view */}
                {templates && templates.length > 0 && (
                    <div className={styles.allTemplatesSection}>
                        <Title3 className={styles.allTemplatesTitle}>All Templates</Title3>
                        <DataGrid
                            items={templates}
                            columns={columns}
                            sortable
                            resizableColumns
                            style={{ minWidth: '100%' }}
                        >
                            <DataGridHeader>
                                <DataGridRow>
                                    {({ renderHeaderCell }) => (
                                        <DataGridHeaderCell>{renderHeaderCell()}</DataGridHeaderCell>
                                    )}
                                </DataGridRow>
                            </DataGridHeader>
                        <DataGridBody<Template>>
                            {({ item, rowId }) => (
                                <DataGridRow<Template> key={rowId}>
                                    {({ renderCell }) => <DataGridCell>{renderCell(item)}</DataGridCell>}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                        </DataGrid>
                    </div>
                )}
        </div>
    );
};

export default TemplateListPage;
