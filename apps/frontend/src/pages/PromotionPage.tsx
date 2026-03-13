/**
 * Smart Persistence Promotion Admin Page
 * P7 - External Integrations & Data Optimization (FS24)
 */

import React, { useState } from 'react';
import {
    Title2,
    Title3,
    Body1,
    Divider,
    Tab,
    TabList,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridCell,
    DataGridBody,
    TableColumnDefinition,
    createTableColumn,
    Button,
    Spinner,
    Badge,
    ProgressBar,
    MessageBar,
    Textarea,
} from '@fluentui/react-components';
import {
    CheckmarkCircleRegular,
    DismissCircleRegular,
    PlayRegular,
    EyeRegular,
} from '@fluentui/react-icons';
import {
    usePromotionCandidates,
    useApprovePromotion,
    useDismissPromotion,
    useUpdatePromotionCandidate,
    usePromotedTables,
    useMigrationProgress,
} from '../hooks/usePromotions';
import type { PromotionCandidate } from '@reportplatform/types';

type PromotionTab = 'candidates' | 'promoted';

const statusColors: Record<string, 'success' | 'warning' | 'danger' | 'informative'> = {
    candidate: 'informative',
    approved: 'success',
    promoted: 'success',
    dismissed: 'warning',
    migration_in_progress: 'informative',
    migration_failed: 'danger',
};

const PromotionPage: React.FC = () => {
    const [selectedTab, setSelectedTab] = useState<PromotionTab>('candidates');
    const [selectedCandidate, setSelectedCandidate] = useState<PromotionCandidate | null>(null);
    const [isEditingDDL, setIsEditingDDL] = useState(false);
    const [editedDDL, setEditedDDL] = useState('');

    // Queries
    const { data: candidates, isLoading: loadingCandidates } = usePromotionCandidates();
    const { data: promotedTables } = usePromotedTables();
    const { data: migrationProgress } = useMigrationProgress(selectedCandidate?.id || '');

    // Mutations
    const approvePromotion = useApprovePromotion();
    const dismissPromotion = useDismissPromotion();
    const updateCandidate = useUpdatePromotionCandidate();

    const handleApprove = async (candidateId: string) => {
        if (confirm('Are you sure you want to approve this promotion? This will create a new table.')) {
            await approvePromotion.mutateAsync({ candidateId });
        }
    };

    const handleDismiss = async (candidateId: string) => {
        const reason = prompt('Please provide a reason for dismissing:');
        if (reason) {
            await dismissPromotion.mutateAsync({ candidateId, request: { reason } });
        }
    };

    const candidateColumns: TableColumnDefinition<PromotionCandidate>[] = [
        createTableColumn<PromotionCandidate>({
            columnId: 'mapping_name',
            compare: (a, b) => a.mapping_name.localeCompare(b.mapping_name),
            renderHeaderCell: () => 'Mapping Name',
            renderCell: (item) => item.mapping_name,
        }),
        createTableColumn<PromotionCandidate>({
            columnId: 'usage_count',
            compare: (a, b) => (a.usage_count || 0) - (b.usage_count || 0),
            renderHeaderCell: () => 'Usage Count',
            renderCell: (item) => item.usage_count,
        }),
        createTableColumn<PromotionCandidate>({
            columnId: 'distinct_org_count',
            renderHeaderCell: () => 'Organizations',
            renderCell: (item) => item.distinct_org_count,
        }),
        createTableColumn<PromotionCandidate>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => (
                <Badge appearance="filled" color={statusColors[item.status] || 'informative'}>
                    {item.status.replace(/_/g, ' ')}
                </Badge>
            ),
        }),
        createTableColumn<PromotionCandidate>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                        size="small"
                        appearance="subtle"
                        icon={<EyeRegular />}
                        onClick={() => {
                            setSelectedCandidate(item);
                            setEditedDDL(item.proposed_ddl);
                            setIsEditingDDL(false);
                        }}
                        title="View Details"
                    />
                    {item.status === 'candidate' && (
                        <>
                            <Button
                                size="small"
                                appearance="subtle"
                                icon={<CheckmarkCircleRegular />}
                                onClick={() => handleApprove(item.id)}
                                title="Approve"
                            />
                            <Button
                                size="small"
                                appearance="subtle"
                                icon={<DismissCircleRegular />}
                                onClick={() => handleDismiss(item.id)}
                                title="Dismiss"
                            />
                        </>
                    )}
                    {(item.status === 'approved' || item.status === 'migration_in_progress') && (
                        <Button
                            size="small"
                            appearance="subtle"
                            icon={<PlayRegular />}
                            title="View Migration Progress"
                        />
                    )}
                </div>
            ),
        }),
    ];
    
    const highlightSQL = (sql: string) => {
        const keywords = ['CREATE', 'TABLE', 'OR', 'REPLACE', 'PRIMARY', 'KEY', 'VARCHAR', 'INTEGER', 'TIMESTAMP', 'TEXT', 'BOOLEAN', 'NOT', 'NULL', 'CONSTRAINT', 'FOREIGN', 'REFERENCES'];
        let highlighted = sql
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
        
        keywords.forEach(kw => {
            const regex = new RegExp(`\\b${kw}\\b`, 'gi');
            highlighted = highlighted.replace(regex, `<span style="color: #0078d4; font-weight: bold;">${kw}</span>`);
        });
        
        // Simplified comment highlighting
        highlighted = highlighted.replace(/(--.*)/g, '<span style="color: #a19f9d; font-style: italic;">$1</span>');
        
        return highlighted;
    };

    return (
        <div className="promotion-page" style={{ padding: '24px' }}>
            <div className="page-header">
                <Title2>Smart Persistence Promotion</Title2>
                <Body1>Manage schema mapping promotions and dedicated table creation</Body1>
            </div>

            <Divider className="divider" style={{ margin: '16px 0' }} />

            <TabList
                selectedValue={selectedTab}
                onTabSelect={(_, data) => setSelectedTab(data.value as PromotionTab)}
            >
                <Tab value="candidates">Candidates</Tab>
                <Tab value="promoted">Promoted Tables</Tab>
            </TabList>

            {selectedTab === 'candidates' && (
                <div className="tab-content" style={{ marginTop: '16px' }}>
                    {loadingCandidates ? (
                        <Spinner label="Loading candidates..." />
                    ) : (
                        <DataGrid
                            items={candidates?.data || []}
                            columns={candidateColumns}
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
                            <DataGridBody<PromotionCandidate>>
                                {({ item, rowId }) => (
                                    <DataGridRow<PromotionCandidate> key={rowId}>
                                        {({ renderCell }) => (
                                            <DataGridCell>{renderCell(item)}</DataGridCell>
                                        )}
                                    </DataGridRow>
                                )}
                            </DataGridBody>
                        </DataGrid>
                    )}

                    {selectedCandidate && (
                        <div className="candidate-detail-panel" style={{ marginTop: '32px', border: '1px solid #edebe9', padding: '16px', borderRadius: '4px' }}>
                            <Title3>Proposal Details: {selectedCandidate.mapping_name}</Title3>

                            {selectedCandidate.status === 'migration_in_progress' && migrationProgress && (
                                <div className="migration-progress" style={{ margin: '16px 0' }}>
                                    <Body1>Migration Progress</Body1>
                                    <ProgressBar
                                        value={migrationProgress.migrated_records}
                                        max={migrationProgress.total_records}
                                    />
                                    <Body1>
                                        {migrationProgress.migrated_records} / {migrationProgress.total_records} records
                                    </Body1>
                                    {migrationProgress.status === 'failed' && (
                                        <MessageBar intent="warning">
                                            {migrationProgress.error_detail || 'Migration failed'}
                                        </MessageBar>
                                    )}
                                </div>
                            )}

                            <div className="proposed-schema" style={{ margin: '16px 0' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                    <Body1 block style={{ fontWeight: 'bold' }}>Proposed DDL</Body1>
                                    {!isEditingDDL ? (
                                        <Button size="small" appearance="subtle" onClick={() => setIsEditingDDL(true)}>Edit DDL</Button>
                                    ) : (
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            <Button size="small" appearance="primary" onClick={async () => {
                                                if (selectedCandidate) {
                                                    await updateCandidate.mutateAsync({
                                                        candidateId: selectedCandidate.id,
                                                        updates: { proposed_ddl: editedDDL }
                                                    });
                                                    setIsEditingDDL(false);
                                                }
                                            }}>Save</Button>
                                            <Button size="small" appearance="secondary" onClick={() => {
                                                setIsEditingDDL(false);
                                                setEditedDDL(selectedCandidate?.proposed_ddl || '');
                                            }}>Cancel</Button>
                                        </div>
                                    )}
                                </div>
                                 {isEditingDDL ? (
                                    <Textarea
                                        value={editedDDL}
                                        onChange={(_, data) => setEditedDDL(data.value)}
                                        style={{ width: '100%', minHeight: '200px', fontFamily: 'monospace' }}
                                    />
                                ) : (
                                    <pre 
                                        style={{
                                            background: '#f5f5f5',
                                            padding: '16px',
                                            borderRadius: '4px',
                                            overflow: 'auto',
                                            fontFamily: 'monospace',
                                            whiteSpace: 'pre-wrap',
                                            fontSize: '12px',
                                            margin: 0,
                                            border: '1px solid #edebe9'
                                        }}
                                        dangerouslySetInnerHTML={{ __html: highlightSQL(selectedCandidate.proposed_ddl) }}
                                    />
                                )}
                            </div>

                            <div className="proposed-columns" style={{ margin: '16px 0' }}>
                                <Body1 block style={{ fontWeight: 'bold', marginBottom: '8px' }}>Proposed Columns</Body1>
                                <DataGrid
                                    items={selectedCandidate.proposed_columns}
                                    columns={[
                                        createTableColumn({
                                            columnId: 'name',
                                            renderHeaderCell: () => 'Name',
                                            renderCell: (item: any) => item.name,
                                        }),
                                        createTableColumn({
                                            columnId: 'data_type',
                                            renderHeaderCell: () => 'Data Type',
                                            renderCell: (item: any) => item.data_type,
                                        }),
                                        createTableColumn({
                                            columnId: 'nullable',
                                            renderHeaderCell: () => 'Nullable',
                                            renderCell: (item: any) => item.nullable ? 'Yes' : 'No',
                                        }),
                                        createTableColumn({
                                            columnId: 'confidence',
                                            renderHeaderCell: () => 'Confidence',
                                            renderCell: (item: any) => `${((item.confidence || 0) * 100).toFixed(0)}%`,
                                        }),
                                    ]}
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

                            {selectedCandidate.proposed_indexes.length > 0 && (
                                <div className="proposed-indexes" style={{ margin: '16px 0' }}>
                                    <Body1 block style={{ fontWeight: 'bold' }}>Proposed Indexes</Body1>
                                    <ul style={{ paddingLeft: '20px' }}>
                                        {selectedCandidate.proposed_indexes.map((idx: any) => (
                                            <li key={idx.name}>
                                                <Body1>{idx.name} ({idx.columns.join(', ')}) {idx.unique ? '(UNIQUE)' : ''}</Body1>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}

                            <Button onClick={() => setSelectedCandidate(null)}>Close Details</Button>
                        </div>
                    )}
                </div>
            )}

            {selectedTab === 'promoted' && (
                <div className="tab-content" style={{ marginTop: '16px' }}>
                    <DataGrid
                        items={promotedTables || []}
                        columns={[
                            createTableColumn({
                                columnId: 'table_name',
                                renderHeaderCell: () => 'Table Name',
                                renderCell: (item: any) => item.table_name,
                            }),
                            createTableColumn({
                                columnId: 'column_count',
                                renderHeaderCell: () => 'Columns',
                                renderCell: (item: any) => item.column_count,
                            }),
                            createTableColumn({
                                columnId: 'row_count',
                                renderHeaderCell: () => 'Rows',
                                renderCell: (item: any) => item.row_count?.toLocaleString() || 'N/A',
                            }),
                            createTableColumn({
                                columnId: 'size_bytes',
                                renderHeaderCell: () => 'Size',
                                renderCell: (item: any) => item.size_bytes ? `${(item.size_bytes / 1024 / 1024).toFixed(2)} MB` : 'N/A',
                            }),
                            createTableColumn({
                                columnId: 'dual_write_until',
                                renderHeaderCell: () => 'Dual Write Until',
                                renderCell: (item: any) => item.dual_write_until
                                    ? new Date(item.dual_write_until).toLocaleString()
                                    : 'Completed',
                            }),
                            createTableColumn({
                                columnId: 'created_at',
                                renderHeaderCell: () => 'Created',
                                renderCell: (item: any) => new Date(item.created_at).toLocaleString(),
                            }),
                        ]}
                        style={{ minWidth: '100%' }}
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

export default PromotionPage;
