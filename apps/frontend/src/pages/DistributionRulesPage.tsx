/**
 * Report Distribution Rules Page
 * P7 - External Integrations & Data Optimization
 */

import React, { useState } from 'react';
import {
    Title2,
    Title3,
    Body1,
    Divider,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridCell,
    DataGridBody,
    TableColumnDefinition,
    createTableColumn,
    Button,
    Spinner,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    Input,
    Label,
    Select,
    Option,
    Badge,
    Tag,
    TagGroup,
} from '@fluentui/react-components';
import type { 
    DialogOpenChangeEvent, 
    DialogOpenChangeData,
} from '@fluentui/react-components';
import { AddRegular, DeleteRegular, EditRegular, ArrowDownloadRegular } from '@fluentui/react-icons';
import {
    useDistributionRules,
    useCreateDistributionRule,
    useUpdateDistributionRule,
    useDeleteDistributionRule,
    useAllDistributionHistory,
    useAllSchedules,
} from '../hooks/useIntegrations';
import { useTemplates } from '../hooks/useTemplates';
import { Template } from '../api/templates';
import type { CreateDistributionRuleRequest, DistributionRule } from '@reportplatform/types';

const DistributionRulesPage: React.FC = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingRule, setEditingRule] = useState<DistributionRule | null>(null);

    // Form state
    const [formData, setFormData] = useState<CreateDistributionRuleRequest>({
        schedule_id: '',
        report_template_id: '',
        recipients: [],
        format: 'xlsx',
        enabled: true,
    });
    const [recipientInput, setRecipientInput] = useState('');

    // Queries
    const { data: rules, isLoading } = useDistributionRules();
    const { data: schedules } = useAllSchedules();
    const { data: distributionHistory } = useAllDistributionHistory();
    const { data: templates } = useTemplates();

    // Mutations
    const createRule = useCreateDistributionRule();
    const updateRule = useUpdateDistributionRule();
    const deleteRule = useDeleteDistributionRule();

    const handleOpenDialog = (rule?: DistributionRule) => {
        if (rule) {
            setEditingRule(rule);
            setFormData({
                schedule_id: rule.schedule_id,
                report_template_id: rule.report_template_id,
                recipients: rule.recipients,
                format: rule.format,
                enabled: rule.enabled,
            });
        } else {
            setEditingRule(null);
            setFormData({
                schedule_id: '',
                report_template_id: '',
                recipients: [],
                format: 'xlsx',
                enabled: true,
            });
        }
        setIsDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setIsDialogOpen(false);
        setEditingRule(null);
    };

    const handleAddRecipient = () => {
        if (recipientInput.trim() && !formData.recipients.includes(recipientInput.trim())) {
            setFormData({
                ...formData,
                recipients: [...formData.recipients, recipientInput.trim()],
            });
            setRecipientInput('');
        }
    };

    const handleRemoveRecipient = (recipient: string) => {
        setFormData({
            ...formData,
            recipients: formData.recipients.filter(r => r !== recipient),
        });
    };

    const handleSaveRule = async () => {
        if (editingRule) {
            await updateRule.mutateAsync({
                ruleId: editingRule.id,
                rule: formData,
            });
        } else {
            await createRule.mutateAsync(formData);
        }
        handleCloseDialog();
    };

    const handleDeleteRule = async (id: string) => {
        if (confirm('Are you sure you want to delete this distribution rule?')) {
            await deleteRule.mutateAsync(id);
        }
    };

    const columns: TableColumnDefinition<DistributionRule>[] = [
        createTableColumn<DistributionRule>({
            columnId: 'schedule',
            renderHeaderCell: () => 'Schedule',
            renderCell: (item) => schedules?.find((s: any) => s.id === item.schedule_id)?.cron_expression || item.schedule_id,
        }),
        createTableColumn<DistributionRule>({
            columnId: 'template',
            renderHeaderCell: () => 'Report Template',
            renderCell: (item) => item.report_template_name || item.report_template_id,
        }),
        createTableColumn<DistributionRule>({
            columnId: 'recipients',
            renderHeaderCell: () => 'Recipients',
            renderCell: (item) => item.recipients.length > 0 ? item.recipients.join(', ') : 'None',
        }),
        createTableColumn<DistributionRule>({
            columnId: 'format',
            renderHeaderCell: () => 'Format',
            renderCell: (item) => item.format.toUpperCase(),
        }),
        createTableColumn<DistributionRule>({
            columnId: 'enabled',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => (
                <Badge appearance="filled" color={item.enabled ? 'success' : 'warning'}>
                    {item.enabled ? 'Active' : 'Inactive'}
                </Badge>
            ),
        }),
        createTableColumn<DistributionRule>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<EditRegular />}
                        onClick={() => handleOpenDialog(item)}
                    />
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<DeleteRegular />}
                        onClick={() => handleDeleteRule(item.id)}
                    />
                </div>
            ),
        }),
    ];

    return (
        <div className="distribution-page" style={{ padding: '24px' }}>
            <div className="page-header">
                <Title2>Report Distribution</Title2>
                <Body1>Configure automated report delivery to email recipients after sync</Body1>
            </div>

            <Divider className="divider" style={{ margin: '16px 0' }} />

            <div className="action-bar" style={{ marginBottom: '16px' }}>
                <Dialog open={isDialogOpen} onOpenChange={(_: DialogOpenChangeEvent, data: DialogOpenChangeData) => !data.open && handleCloseDialog()}>
                    <DialogTrigger disableButtonEnhancement>
                        <Button
                            appearance="primary"
                            icon={<AddRegular />}
                            onClick={() => handleOpenDialog()}
                        >
                            Add Distribution Rule
                        </Button>
                    </DialogTrigger>
                    <DialogSurface>
                        <DialogBody>
                            <DialogTitle>
                                {editingRule ? 'Edit Distribution Rule' : 'New Distribution Rule'}
                            </DialogTitle>
                            <DialogContent>
                                <div className="form-fields" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    <div>
                                        <Label required>Schedule</Label>
                                        <Select
                                            style={{ width: '100%' }}
                                            value={formData.schedule_id}
                                            onChange={(_, data) => setFormData({ ...formData, schedule_id: data.value })}
                                        >
                                            <Option value="">Select a schedule...</Option>
                                            {schedules?.map(schedule => (
                                                <Option key={schedule.id} value={schedule.id}>
                                                    {schedule.cron_expression}
                                                </Option>
                                            ))}
                                        </Select>
                                    </div>

                                    <div>
                                        <Label required>Report Template</Label>
                                        <Select
                                            style={{ width: '100%' }}
                                            value={formData.report_template_id}
                                            onChange={(_, data) => setFormData({ ...formData, report_template_id: data.value })}
                                        >
                                            <Option value="">Select a template...</Option>
                                            {templates?.map((t: Template) => (
                                                <Option key={t.id} value={t.id}>{t.name}</Option>
                                            ))}
                                        </Select>
                                    </div>

                                    <div>
                                        <Label required>Format</Label>
                                        <Select
                                            style={{ width: '100%' }}
                                            value={formData.format}
                                            onChange={(_, data) => setFormData({ ...formData, format: data.value as 'xlsx' | 'pdf' | 'pptx' })}
                                        >
                                            <Option value="xlsx">Excel (XLSX)</Option>
                                            <Option value="pdf">PDF</Option>
                                            <Option value="pptx">PowerPoint (PPTX)</Option>
                                        </Select>
                                    </div>

                                    <div>
                                        <Label>Recipients</Label>
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            <Input
                                                style={{ flex: 1 }}
                                                value={recipientInput}
                                                onChange={(_, data) => setRecipientInput(data.value)}
                                                placeholder="email@example.com"
                                                onKeyDown={(e) => e.key === 'Enter' && handleAddRecipient()}
                                            />
                                            <Button onClick={handleAddRecipient}>Add</Button>
                                        </div>
                                    </div>
                                    
                                    {formData.recipients.length > 0 && (
                                        <TagGroup>
                                            {formData.recipients.map((recipient: string) => (
                                                <Tag
                                                    key={recipient}
                                                    dismissible
                                                    onClick={() => handleRemoveRecipient(recipient)}
                                                >
                                                    {recipient}
                                                </Tag>
                                            ))}
                                        </TagGroup>
                                    )}
                                </div>
                            </DialogContent>
                            <DialogActions>
                                <Button appearance="secondary" onClick={handleCloseDialog}>
                                    Cancel
                                </Button>
                                <Button appearance="primary" onClick={handleSaveRule}>
                                    {editingRule ? 'Update' : 'Create'}
                                </Button>
                            </DialogActions>
                        </DialogBody>
                    </DialogSurface>
                </Dialog>
            </div>

            {isLoading ? (
                <Spinner label="Loading distribution rules..." />
            ) : (
                <DataGrid
                    items={rules || []}
                    columns={columns}
                    sortable
                    style={{ minWidth: '100%' }}
                >
                    <DataGridHeader>
                        <DataGridRow>
                            {({ renderHeaderCell }) => (
                                <DataGridCell>{renderHeaderCell()}</DataGridCell>
                            )}
                        </DataGridRow>
                    </DataGridHeader>
                    <DataGridBody<DistributionRule>>
                        {({ item, rowId }) => (
                            <DataGridRow<DistributionRule> key={rowId}>
                                {({ renderCell }) => (
                                    <DataGridCell>{renderCell(item)}</DataGridCell>
                                )}
                            </DataGridRow>
                        )}
                    </DataGridBody>
                </DataGrid>
            )}

            {distributionHistory && distributionHistory.data.length > 0 && (
                <div style={{ marginTop: '32px' }}>
                    <Divider style={{ margin: '16px 0' }} />
                    <Title3>Distribution History</Title3>
                    <DataGrid
                        items={distributionHistory.data}
                        columns={[
                            createTableColumn({
                                columnId: 'rule_id',
                                renderHeaderCell: () => 'Rule',
                                renderCell: (item: any) => rules?.find((r: any) => r.id === item.rule_id)?.report_template_name || item.rule_id,
                            }),
                            createTableColumn({
                                columnId: 'recipients',
                                renderHeaderCell: () => 'Recipients',
                                renderCell: (item: any) => item.recipients.join(', '),
                            }),
                            createTableColumn({
                                columnId: 'timestamp',
                                renderHeaderCell: () => 'Timestamp',
                                renderCell: (item: any) => new Date(item.timestamp).toLocaleString(),
                            }),
                            createTableColumn({
                                columnId: 'status',
                                renderHeaderCell: () => 'Status',
                                renderCell: (item: any) => (
                                    <Badge appearance="filled" color={item.status === 'sent' ? 'success' : item.status === 'failed' ? 'danger' : 'warning'}>
                                        {item.status}
                                    </Badge>
                                ),
                            }),
                            createTableColumn({
                                columnId: 'file',
                                renderHeaderCell: () => 'File',
                                renderCell: (item: any) => item.file_url ? (
                                    <Button
                                        size="small"
                                        appearance="subtle"
                                        icon={<ArrowDownloadRegular />}
                                        onClick={() => window.open(item.file_url!, '_blank')}
                                    >
                                        Download
                                    </Button>
                                ) : '-',
                            }),
                        ]}
                        style={{ minWidth: '100%', marginTop: '16px' }}
                    >
                        <DataGridHeader>
                            <DataGridRow>
                                {({ renderHeaderCell }) => (
                                    <DataGridCell>{renderHeaderCell()}</DataGridCell>
                                )}
                            </DataGridRow>
                        </DataGridHeader>
                        <DataGridBody<any>>
                            {({ item, rowId }) => (
                                <DataGridRow key={rowId}>
                                    {({ renderCell }) => (
                                        <DataGridCell>{renderCell(item)}</DataGridCell>
                                    )}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                    </DataGrid>
                </div>
            )}
        </div>
    );
};

export default DistributionRulesPage;
