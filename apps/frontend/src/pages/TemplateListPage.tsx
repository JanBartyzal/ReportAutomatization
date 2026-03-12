import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Page,
    Title3,
    Subtitle1,
    Body1,
    Caption1,
} from '@fluentui/react-components';
import {
    DataGrid,
    DataGridHeader,
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
    Delete24Regular,
} from '@fluentui/react-icons';
import { useTemplates, useUploadTemplate, useDeleteTemplate } from '../hooks/useTemplates';
import { reportBrand } from '../theme/brandTokens';
import { ScopeBadge } from '../components/ScopeBadge';

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

export const TemplateListPage: React.FC = () => {
    const navigate = useNavigate();
    const { data: templates, isLoading, error } = useTemplates();
    const uploadMutation = useUploadTemplate();
    const deleteMutation = useDeleteTemplate();

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

    const handleDelete = async (id: string) => {
        if (window.confirm('Are you sure you want to delete this template?')) {
            await deleteMutation.mutateAsync(id);
        }
    };

    if (isLoading) {
        return (
            <Page>
                <div style={{ display: 'flex', justifyContent: 'center', padding: '40px' }}>
                    <Spinner label="Loading templates..." />
                </div>
            </Page>
        );
    }

    if (error) {
        return (
            <Page>
                <div style={{ padding: '20px' }}>
                    <Body1 style={{ color: '#C4314B' }}>
                        Error loading templates: {(error as Error).message}
                    </Body1>
                </div>
            </Page>
        );
    }

    return (
        <Page>
            <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
                {/* Header */}
                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '24px'
                }}>
                    <div>
                        <Title3 style={{ color: reportBrand[90] }}>PPTX Templates</Title3>
                        <Subtitle1>Manage PowerPoint report templates</Subtitle1>
                    </div>
                    <Dialog open={isUploadDialogOpen} onOpenChange={(_, d) => setIsUploadDialogOpen(d.open)}>
                        <DialogTrigger disableButtonEnhancement>
                            <Button
                                appearance="primary"
                                icon={<Add24Regular />}
                                style={{ backgroundColor: reportBrand[90] }}
                            >
                                Upload Template
                            </Button>
                        </DialogTrigger>
                        <DialogSurface>
                            <DialogBody>
                                <DialogTitle>Upload PPTX Template</DialogTitle>
                                <DialogContent>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '16px 0' }}>
                                        <Input
                                            label="Template Name"
                                            value={uploadName}
                                            onChange={(_, d) => setUploadName(d.value)}
                                            placeholder="Enter template name"
                                        />
                                        <Select
                                            label="Scope"
                                            value={uploadScope}
                                            onChange={(_, d) => setUploadScope(d.value as 'CENTRAL' | 'LOCAL')}
                                        >
                                            <Option value="CENTRAL">Central (HoldingAdmin)</Option>
                                            <Option value="LOCAL">Local</Option>
                                        </Select>
                                        <div style={{ border: '2px dashed #d0d0d0', borderRadius: '8px', padding: '24px', textAlign: 'center' }}>
                                            <input
                                                type="file"
                                                accept=".pptx"
                                                onChange={(e) => setUploadFile((e.target as HTMLInputElement).files?.[0] || null)}
                                                style={{ display: 'none' }}
                                                id="template-upload"
                                            />
                                            <label htmlFor="template-upload" style={{ cursor: 'pointer' }}>
                                                <DocumentPdf24Regular style={{ fontSize: '32px', color: reportBrand[90] }} />
                                                <Body1 style={{ display: 'block', marginTop: '8px' }}>
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
                                        style={{ backgroundColor: reportBrand[90] }}
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
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                        gap: '16px'
                    }}>
                        {templates.map((template) => (
                            <Card
                                key={template.id}
                                onClick={() => navigate(`/templates/${template.id}`)}
                                style={{ cursor: 'pointer' }}
                            >
                                <CardHeader
                                    header={
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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
                                <CardPreview style={{
                                    height: '120px',
                                    backgroundColor: '#f5f5f5',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center'
                                }}>
                                    <DocumentPdf24Regular style={{ fontSize: '48px', color: reportBrand[80] }} />
                                </CardPreview>
                            </Card>
                        ))}
                    </div>
                ) : (
                    <div style={{
                        textAlign: 'center',
                        padding: '60px 20px',
                        backgroundColor: '#fafafa',
                        borderRadius: '8px'
                    }}>
                        <DocumentPdf24Regular style={{ fontSize: '64px', color: '#ccc' }} />
                        <Title3 style={{ marginTop: '16px' }}>No templates yet</Title3>
                        <Body1 style={{ color: '#666', marginTop: '8px' }}>
                            Upload your first PPTX template to get started
                        </Body1>
                    </div>
                )}

                {/* Data Grid for detailed view */}
                {templates && templates.length > 0 && (
                    <div style={{ marginTop: '32px' }}>
                        <Title3 style={{ marginBottom: '16px' }}>All Templates</Title3>
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
                            {({ item, rowId }: { item: Template, rowId: string }) => (
                                <DataGridRow<Template> key={rowId}>
                                    {({ renderCell }) => <DataGridCell>{renderCell(item)}</DataGridCell>}
                                </DataGridRow>
                            )}
                        </DataGrid>
                    </div>
                )}
            </div>
        </Page>
    );
};

export default TemplateListPage;
