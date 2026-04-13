/**
 * Export Flow Create / Edit Dialog — Multi-step wizard
 * FS27 – Live Excel Export & External Sync
 *
 * Steps:
 *   1. Basic Info    – name, description, active flag
 *   2. Data Source   – SQL query + test preview
 *   3. Target Config – target type (LOCAL_PATH / SHAREPOINT) + path or SP config
 *   4. Sheet & File  – sheet name, file naming (CUSTOM / BATCH_NAME)
 *   5. Trigger       – MANUAL / AUTO + optional triggerFilter JSON
 */

import React, { useState, useEffect } from 'react';
import {
    Dialog,
    DialogSurface,
    DialogBody,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Input,
    Label,
    Select,
    Option,
    Textarea,
    Switch,
    Spinner,
    MessageBar,
    Tab,
    TabList,
    tokens,
    makeStyles,
    Body1,
    Caption1,
    Divider,
} from '@fluentui/react-components';
import { CheckmarkRegular, FlashRegular } from '@fluentui/react-icons';
import { useCreateExportFlow, useUpdateExportFlow, useTestExportFlow } from '../hooks/useExportFlows';
import type {
    ExportFlowDefinition,
    ExportFlowCreateRequest,
    SharePointConfig,
} from '@reportplatform/types';

const useStyles = makeStyles({
    surface: {
        width: '660px',
        maxWidth: '95vw',
    },
    stepBar: {
        marginBottom: '16px',
    },
    formSection: {
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
        minHeight: '240px',
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: '4px',
    },
    row: {
        display: 'flex',
        gap: '12px',
        '& > *': { flex: 1 },
    },
    queryBox: {
        fontFamily: 'monospace',
        fontSize: '13px',
        minHeight: '120px',
        resize: 'vertical',
    },
    previewBox: {
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: '4px',
        padding: '8px',
        maxHeight: '180px',
        overflowY: 'auto',
        fontSize: '12px',
        fontFamily: 'monospace',
        whiteSpace: 'pre-wrap',
    },
    navRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        width: '100%',
    },
    stepHint: {
        color: tokens.colorNeutralForeground3,
        fontSize: '13px',
    },
});

type Step = 'basic' | 'datasource' | 'target' | 'sheet' | 'trigger';
const STEPS: Step[] = ['basic', 'datasource', 'target', 'sheet', 'trigger'];
const STEP_LABELS: Record<Step, string> = {
    basic: 'Basic Info',
    datasource: 'Data Source',
    target: 'Target',
    sheet: 'Sheet & File',
    trigger: 'Trigger',
};

// =============================================================================
// Form state
// =============================================================================

interface FormState {
    name: string;
    description: string;
    active: boolean;
    sqlQuery: string;
    targetType: 'LOCAL_PATH' | 'SHAREPOINT';
    targetPath: string;
    /** SharePoint sub-fields (serialised to JSON string on submit) */
    sp: {
        siteUrl: string;
        library: string;
        folderPath: string;
        clientId: string;
        tenantId: string;
    };
    targetSheet: string;
    fileNaming: 'CUSTOM' | 'BATCH_NAME';
    customFileName: string;
    triggerType: 'MANUAL' | 'AUTO';
    triggerFilter: string;
}

const EMPTY_FORM: FormState = {
    name: '',
    description: '',
    active: true,
    sqlQuery: '',
    targetType: 'LOCAL_PATH',
    targetPath: '',
    sp: { siteUrl: '', library: '', folderPath: '', clientId: '', tenantId: '' },
    targetSheet: 'Data',
    fileNaming: 'CUSTOM',
    customFileName: '',
    triggerType: 'MANUAL',
    triggerFilter: '',
};

function flowToForm(flow: ExportFlowDefinition): FormState {
    let sp = EMPTY_FORM.sp;
    if (flow.sharepointConfig) {
        try {
            const parsed: SharePointConfig = JSON.parse(flow.sharepointConfig);
            sp = {
                siteUrl: parsed.siteUrl ?? '',
                library: parsed.library ?? '',
                folderPath: parsed.folderPath ?? '',
                clientId: parsed.clientId ?? '',
                tenantId: parsed.tenantId ?? '',
            };
        } catch { /* leave defaults */ }
    }
    return {
        name: flow.name,
        description: flow.description ?? '',
        active: flow.active,
        sqlQuery: flow.sqlQuery,
        targetType: flow.targetType as 'LOCAL_PATH' | 'SHAREPOINT',
        targetPath: flow.targetPath,
        sp,
        targetSheet: flow.targetSheet,
        fileNaming: (flow.fileNaming as 'CUSTOM' | 'BATCH_NAME') ?? 'CUSTOM',
        customFileName: flow.customFileName ?? '',
        triggerType: (flow.triggerType as 'MANUAL' | 'AUTO') ?? 'MANUAL',
        triggerFilter: flow.triggerFilter ?? '',
    };
}

function formToRequest(form: FormState): ExportFlowCreateRequest {
    const sharepointConfig: string | undefined =
        form.targetType === 'SHAREPOINT'
            ? JSON.stringify({
                  siteUrl: form.sp.siteUrl,
                  library: form.sp.library,
                  folderPath: form.sp.folderPath || undefined,
                  clientId: form.sp.clientId || undefined,
                  tenantId: form.sp.tenantId || undefined,
              } satisfies SharePointConfig)
            : undefined;

    return {
        name: form.name,
        description: form.description || undefined,
        sqlQuery: form.sqlQuery,
        targetType: form.targetType,
        targetPath: form.targetPath,
        targetSheet: form.targetSheet,
        fileNaming: form.fileNaming,
        customFileName: form.fileNaming === 'CUSTOM' && form.customFileName ? form.customFileName : undefined,
        triggerType: form.triggerType,
        triggerFilter: form.triggerFilter || undefined,
        sharepointConfig,
    };
}

// =============================================================================
// Step validation
// =============================================================================

function validateStep(step: Step, form: FormState): string | null {
    switch (step) {
        case 'basic':
            if (!form.name.trim()) return 'Name is required.';
            break;
        case 'datasource':
            if (!form.sqlQuery.trim()) return 'SQL query is required.';
            break;
        case 'target':
            if (form.targetType === 'LOCAL_PATH' && !form.targetPath.trim())
                return 'Output path is required.';
            if (form.targetType === 'SHAREPOINT') {
                if (!form.sp.siteUrl || !form.sp.library)
                    return 'Site URL and library are required for SharePoint.';
            }
            break;
        case 'sheet':
            if (!form.targetSheet.trim()) return 'Sheet name is required.';
            if (form.fileNaming === 'CUSTOM' && !form.customFileName.trim())
                return 'File name is required for Custom naming.';
            break;
    }
    return null;
}

// =============================================================================
// Step sub-forms
// =============================================================================

interface StepProps {
    form: FormState;
    onChange: (patch: Partial<FormState>) => void;
}

function StepBasic({ form, onChange }: StepProps) {
    const styles = useStyles();
    return (
        <div className={styles.formSection}>
            <div className={styles.field}>
                <Label required htmlFor="ef-name">Name</Label>
                <Input
                    id="ef-name"
                    value={form.name}
                    onChange={(_, d) => onChange({ name: d.value })}
                    placeholder="Monthly OPEX Export"
                />
            </div>
            <div className={styles.field}>
                <Label htmlFor="ef-desc">Description</Label>
                <Textarea
                    id="ef-desc"
                    value={form.description}
                    onChange={(_, d) => onChange({ description: d.value })}
                    placeholder="Describe the purpose of this export flow"
                    rows={3}
                />
            </div>
            <Switch
                label="Active"
                checked={form.active}
                onChange={(_, d) => onChange({ active: d.checked })}
            />
        </div>
    );
}

interface StepDataSourceProps extends StepProps {
    previewResult: string | null;
    isTesting: boolean;
    onTest: () => void;
}

function StepDataSource({ form, onChange, previewResult, isTesting, onTest }: StepDataSourceProps) {
    const styles = useStyles();
    return (
        <div className={styles.formSection}>
            <div className={styles.field}>
                <Label required htmlFor="ef-sql">SQL Query</Label>
                <Textarea
                    id="ef-sql"
                    className={styles.queryBox}
                    value={form.sqlQuery}
                    onChange={(_, d) => onChange({ sqlQuery: d.value })}
                    placeholder="SELECT col1, col2 FROM my_table WHERE org_id = :orgId"
                    rows={7}
                />
                <Caption1>Use :orgId, :periodId as named parameters.</Caption1>
            </div>
            {previewResult && (
                <div className={styles.field}>
                    <Caption1>Preview</Caption1>
                    <pre className={styles.previewBox}>{previewResult}</pre>
                </div>
            )}
            <div>
                <Button
                    size="small"
                    appearance="outline"
                    icon={isTesting ? <Spinner size="tiny" /> : <FlashRegular />}
                    onClick={onTest}
                    disabled={isTesting || !form.sqlQuery.trim()}
                >
                    Test Query
                </Button>
            </div>
        </div>
    );
}

function StepTarget({ form, onChange }: StepProps) {
    const styles = useStyles();
    return (
        <div className={styles.formSection}>
            <div className={styles.field}>
                <Label required>Target Type</Label>
                <Select
                    value={form.targetType}
                    onChange={(_, d) =>
                        onChange({ targetType: d.value as 'LOCAL_PATH' | 'SHAREPOINT' })
                    }
                >
                    <Option value="LOCAL_PATH">Local Path / Network Share</Option>
                    <Option value="SHAREPOINT">SharePoint Online</Option>
                </Select>
            </div>

            {form.targetType === 'LOCAL_PATH' && (
                <div className={styles.field}>
                    <Label required htmlFor="ef-path">Output Path</Label>
                    <Input
                        id="ef-path"
                        value={form.targetPath}
                        onChange={(_, d) => onChange({ targetPath: d.value })}
                        placeholder="/mnt/exports/opex"
                    />
                </div>
            )}

            {form.targetType === 'SHAREPOINT' && (
                <>
                    <div className={styles.field}>
                        <Label required htmlFor="ef-sp-path">SharePoint Path (targetPath)</Label>
                        <Input
                            id="ef-sp-path"
                            value={form.targetPath}
                            onChange={(_, d) => onChange({ targetPath: d.value })}
                            placeholder="/sites/Finance/Shared Documents/Reports"
                        />
                    </div>
                    <div className={styles.row}>
                        <div className={styles.field}>
                            <Label required htmlFor="ef-sp-siteurl">Site URL</Label>
                            <Input
                                id="ef-sp-siteurl"
                                value={form.sp.siteUrl}
                                onChange={(_, d) =>
                                    onChange({ sp: { ...form.sp, siteUrl: d.value } })
                                }
                                placeholder="https://contoso.sharepoint.com/sites/Finance"
                            />
                        </div>
                        <div className={styles.field}>
                            <Label required htmlFor="ef-sp-lib">Library</Label>
                            <Input
                                id="ef-sp-lib"
                                value={form.sp.library}
                                onChange={(_, d) =>
                                    onChange({ sp: { ...form.sp, library: d.value } })
                                }
                                placeholder="Shared Documents"
                            />
                        </div>
                    </div>
                    <div className={styles.field}>
                        <Label htmlFor="ef-sp-folder">Folder Path</Label>
                        <Input
                            id="ef-sp-folder"
                            value={form.sp.folderPath}
                            onChange={(_, d) =>
                                onChange({ sp: { ...form.sp, folderPath: d.value } })
                            }
                            placeholder="/Reports/Monthly"
                        />
                    </div>
                    <div className={styles.row}>
                        <div className={styles.field}>
                            <Label htmlFor="ef-sp-tenant">Tenant ID</Label>
                            <Input
                                id="ef-sp-tenant"
                                value={form.sp.tenantId}
                                onChange={(_, d) =>
                                    onChange({ sp: { ...form.sp, tenantId: d.value } })
                                }
                                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                            />
                        </div>
                        <div className={styles.field}>
                            <Label htmlFor="ef-sp-client">Client ID</Label>
                            <Input
                                id="ef-sp-client"
                                value={form.sp.clientId}
                                onChange={(_, d) =>
                                    onChange({ sp: { ...form.sp, clientId: d.value } })
                                }
                                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                            />
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

function StepSheet({ form, onChange }: StepProps) {
    const styles = useStyles();
    return (
        <div className={styles.formSection}>
            <div className={styles.field}>
                <Label required htmlFor="ef-sheet">Target Sheet Name</Label>
                <Input
                    id="ef-sheet"
                    value={form.targetSheet}
                    onChange={(_, d) => onChange({ targetSheet: d.value })}
                    placeholder="Data"
                />
                <Caption1>The sheet will be created or overwritten if it already exists.</Caption1>
            </div>
            <div className={styles.field}>
                <Label required>File Naming</Label>
                <Select
                    value={form.fileNaming}
                    onChange={(_, d) =>
                        onChange({ fileNaming: d.value as 'CUSTOM' | 'BATCH_NAME' })
                    }
                >
                    <Option value="CUSTOM">Custom file name</Option>
                    <Option value="BATCH_NAME">Batch name (uses period/batch label)</Option>
                </Select>
            </div>
            {form.fileNaming === 'CUSTOM' && (
                <div className={styles.field}>
                    <Label required htmlFor="ef-fname">File Name</Label>
                    <Input
                        id="ef-fname"
                        value={form.customFileName}
                        onChange={(_, d) => onChange({ customFileName: d.value })}
                        placeholder="monthly_opex.xlsx"
                    />
                </div>
            )}
        </div>
    );
}

function StepTrigger({ form, onChange }: StepProps) {
    const styles = useStyles();
    return (
        <div className={styles.formSection}>
            <div className={styles.field}>
                <Label required>Trigger Type</Label>
                <Select
                    value={form.triggerType}
                    onChange={(_, d) =>
                        onChange({ triggerType: d.value as 'MANUAL' | 'AUTO' })
                    }
                >
                    <Option value="MANUAL">Manual only</Option>
                    <Option value="AUTO">Automatic (event-driven)</Option>
                </Select>
            </div>

            {form.triggerType === 'AUTO' && (
                <div className={styles.field}>
                    <Label htmlFor="ef-tf">Trigger Filter (JSON, optional)</Label>
                    <Textarea
                        id="ef-tf"
                        value={form.triggerFilter}
                        onChange={(_, d) => onChange({ triggerFilter: d.value })}
                        placeholder={'{"topic": "data-imported"}'}
                        rows={3}
                        style={{ fontFamily: 'monospace', fontSize: '13px' }}
                    />
                    <Caption1>
                        Leave empty to react to all incoming events. Use JSON to filter by topic or
                        payload fields.
                    </Caption1>
                </div>
            )}

            {form.triggerType === 'MANUAL' && (
                <Body1 style={{ color: tokens.colorNeutralForeground3 }}>
                    This flow will only run when triggered manually via the "Export Now" button or
                    the REST API.
                </Body1>
            )}
        </div>
    );
}

// =============================================================================
// Main Dialog
// =============================================================================

export interface ExportFlowDialogProps {
    open: boolean;
    onClose: () => void;
    editFlow?: ExportFlowDefinition;
}

export function ExportFlowDialog({ open, onClose, editFlow }: ExportFlowDialogProps) {
    const styles = useStyles();
    const [currentStep, setCurrentStep] = useState<Step>('basic');
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [stepError, setStepError] = useState<string | null>(null);
    const [testPreview, setTestPreview] = useState<string | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);

    const createFlow = useCreateExportFlow();
    const updateFlow = useUpdateExportFlow();
    const testFlow = useTestExportFlow();

    useEffect(() => {
        if (open) {
            setForm(editFlow ? flowToForm(editFlow) : EMPTY_FORM);
            setCurrentStep('basic');
            setStepError(null);
            setTestPreview(null);
            setSaveError(null);
        }
    }, [open, editFlow]);

    const handleChange = (patch: Partial<FormState>) => {
        setForm((prev) => ({ ...prev, ...patch }));
        setStepError(null);
    };

    const stepIndex = STEPS.indexOf(currentStep);
    const isFirst = stepIndex === 0;
    const isLast = stepIndex === STEPS.length - 1;

    const goNext = () => {
        const err = validateStep(currentStep, form);
        if (err) { setStepError(err); return; }
        setStepError(null);
        setCurrentStep(STEPS[stepIndex + 1]);
    };

    const goBack = () => {
        setStepError(null);
        setCurrentStep(STEPS[stepIndex - 1]);
    };

    const handleTest = async () => {
        if (!editFlow) {
            setTestPreview('Save the flow first to test the query against live data.');
            return;
        }
        try {
            const result = await testFlow.mutateAsync(editFlow.id);
            if (result.error) {
                setTestPreview(`Error: ${result.error}`);
            } else {
                const header = result.headers.join(' | ');
                const rows = result.rows
                    .slice(0, 5)
                    .map((r) => result.headers.map((h) => String(r[h] ?? '')).join(' | '))
                    .join('\n');
                setTestPreview(
                    `${header}\n${'─'.repeat(header.length)}\n${rows}\n\n${result.totalRows} total rows${result.truncated ? ' (truncated)' : ''}`
                );
            }
        } catch (e: any) {
            setTestPreview(`Error: ${e?.message ?? 'unknown error'}`);
        }
    };

    const handleSave = async () => {
        const err = validateStep(currentStep, form);
        if (err) { setStepError(err); return; }
        setSaveError(null);
        try {
            const payload = formToRequest(form);
            if (editFlow) {
                await updateFlow.mutateAsync({ id: editFlow.id, payload });
            } else {
                await createFlow.mutateAsync(payload);
            }
            onClose();
        } catch (e: any) {
            setSaveError(
                e?.response?.data?.message ?? e?.message ?? 'Failed to save export flow.'
            );
        }
    };

    const isSaving = createFlow.isPending || updateFlow.isPending;

    return (
        <Dialog open={open} onOpenChange={(_, d) => !d.open && onClose()}>
            <DialogSurface className={styles.surface}>
                <DialogBody>
                    <DialogTitle>
                        {editFlow ? `Edit — ${editFlow.name}` : 'New Export Flow'}
                    </DialogTitle>

                    <DialogContent>
                        <TabList
                            className={styles.stepBar}
                            selectedValue={currentStep}
                            onTabSelect={(_, d) => {
                                const targetIdx = STEPS.indexOf(d.value as Step);
                                if (targetIdx < stepIndex) {
                                    setStepError(null);
                                    setCurrentStep(d.value as Step);
                                }
                            }}
                        >
                            {STEPS.map((s) => (
                                <Tab key={s} value={s}>
                                    {STEP_LABELS[s]}
                                </Tab>
                            ))}
                        </TabList>

                        <Divider style={{ marginBottom: '16px' }} />

                        {currentStep === 'basic' && (
                            <StepBasic form={form} onChange={handleChange} />
                        )}
                        {currentStep === 'datasource' && (
                            <StepDataSource
                                form={form}
                                onChange={handleChange}
                                previewResult={testPreview}
                                isTesting={testFlow.isPending}
                                onTest={handleTest}
                            />
                        )}
                        {currentStep === 'target' && (
                            <StepTarget form={form} onChange={handleChange} />
                        )}
                        {currentStep === 'sheet' && (
                            <StepSheet form={form} onChange={handleChange} />
                        )}
                        {currentStep === 'trigger' && (
                            <StepTrigger form={form} onChange={handleChange} />
                        )}

                        {stepError && (
                            <MessageBar intent="error" style={{ marginTop: '12px' }}>
                                {stepError}
                            </MessageBar>
                        )}
                        {saveError && (
                            <MessageBar intent="error" style={{ marginTop: '12px' }}>
                                {saveError}
                            </MessageBar>
                        )}
                    </DialogContent>

                    <DialogActions>
                        <div className={styles.navRow}>
                            <span className={styles.stepHint}>
                                Step {stepIndex + 1} of {STEPS.length}
                            </span>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <Button
                                    appearance="secondary"
                                    onClick={onClose}
                                    disabled={isSaving}
                                >
                                    Cancel
                                </Button>
                                {!isFirst && (
                                    <Button
                                        appearance="secondary"
                                        onClick={goBack}
                                        disabled={isSaving}
                                    >
                                        Back
                                    </Button>
                                )}
                                {!isLast ? (
                                    <Button appearance="primary" onClick={goNext}>
                                        Next
                                    </Button>
                                ) : (
                                    <Button
                                        appearance="primary"
                                        icon={isSaving ? <Spinner size="tiny" /> : <CheckmarkRegular />}
                                        onClick={handleSave}
                                        disabled={isSaving}
                                    >
                                        {editFlow ? 'Save Changes' : 'Create'}
                                    </Button>
                                )}
                            </div>
                        </div>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

export default ExportFlowDialog;
