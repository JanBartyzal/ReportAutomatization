import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title3,
    Subtitle2,
    Body1,
    Button,
    Field,
    Input,
    Textarea,
    Dropdown,
    Option,
    Checkbox,
    Divider,
    Card,
    makeStyles,
    shorthands,
    tokens,
    Tab,
    TabList,
} from '@fluentui/react-components';
import {
    SaveRegular,
    SendRegular,
    ArrowLeftRegular,
    AddRegular,
    DeleteRegular,
    ArrowUpRegular,
    ArrowDownRegular,
    EyeRegular,
    EditRegular,
} from '@fluentui/react-icons';
import { useForm, useCreateForm, useUpdateForm, usePublishForm } from '../hooks/useForms';
import { FormField, FormFieldType, FormStatus } from '@reportplatform/types';
import LoadingSpinner from '../components/LoadingSpinner';

const useStyles = makeStyles({
    container: {
        padding: '24px',
        maxWidth: '1200px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '24px',
    },
    editorLayout: {
        display: 'grid',
        gridTemplateColumns: 'minmax(0, 1fr) 350px',
        gap: '24px',
    },
    fieldCard: {
        marginBottom: '16px',
        position: 'relative',
        ...shorthands.padding('16px'),
    },
    fieldActions: {
        position: 'absolute',
        top: '8px',
        right: '8px',
        display: 'flex',
        gap: '4px',
    },
    propPanel: {
        position: 'sticky',
        top: '24px',
        height: 'fit-content',
        ...shorthands.padding('16px'),
    },
    sectionHeader: {
        backgroundColor: tokens.colorNeutralBackground2,
        ...shorthands.padding('12px'),
        ...shorthands.borderRadius(tokens.borderRadiusMedium),
        marginBottom: '16px',
        marginTop: '24px',
    },
    mainPanel: {}
});

export const FormEditorPage: React.FC = () => {
    const styles = useStyles();
    const { formId } = useParams<{ formId: string }>();
    const navigate = useNavigate();
    const isNew = !formId || formId === 'new';

    const { data: formData, isLoading: formLoading } = useForm(isNew ? '' : formId!);
    const createForm = useCreateForm();
    const updateForm = useUpdateForm();
    const publishForm = usePublishForm();

    const [formState, setFormState] = useState({
        name: '',
        description: '',
        fields: [] as FormField[],
    });

    const [selectedFieldId, setSelectedFieldId] = useState<string | null>(null);
    const [viewMode, setViewMode] = useState<'edit' | 'preview'>('edit');

    useEffect(() => {
        if (formData) {
            setFormState({
                name: formData.name,
                description: formData.description || '',
                fields: [...formData.fields].sort((a, b) => a.order - b.order),
            });
        }
    }, [formData]);

    const handleAddField = () => {
        const newField: FormField = {
            field_id: `field_${Date.now()}`,
            name: 'New Field',
            type: FormFieldType.TEXT,
            required: false,
            order: formState.fields.length,
            section: 'General',
        };
        setFormState(prev => ({
            ...prev,
            fields: [...prev.fields, newField]
        }));
        setSelectedFieldId(newField.field_id);
    };

    const handleDeleteField = (fieldId: string) => {
        setFormState(prev => ({
            ...prev,
            fields: prev.fields.filter(f => f.field_id !== fieldId)
        }));
        if (selectedFieldId === fieldId) setSelectedFieldId(null);
    };

    const handleMoveField = (index: number, direction: 'up' | 'down') => {
        const newFields = [...formState.fields];
        const newIndex = direction === 'up' ? index - 1 : index + 1;
        if (newIndex < 0 || newIndex >= newFields.length) return;

        [newFields[index], newFields[newIndex]] = [newFields[newIndex], newFields[index]];
        
        // Update orders
        newFields.forEach((f, i) => f.order = i);
        
        setFormState(prev => ({ ...prev, fields: newFields }));
    };

    const handleUpdateField = (fieldId: string, updates: Partial<FormField>) => {
        setFormState(prev => ({
            ...prev,
            fields: prev.fields.map(f => f.field_id === fieldId ? { ...f, ...updates } : f)
        }));
    };

    const handleSave = async (publish: boolean = false) => {
        const payload = {
            ...formState,
            status: publish ? FormStatus.PUBLISHED : (formData?.status || FormStatus.DRAFT),
            report_type: 'CUSTOM', // Default for now
            scope: 'CENTRAL' as const,
            fields: formState.fields,
        } as any;

        if (isNew) {
            const result = await createForm.mutateAsync(payload);
            if (publish) {
                await publishForm.mutateAsync(result.id!);
            }
            navigate(`/forms/${result.id}/edit`);
        } else {
            await updateForm.mutateAsync({ formId: formId!, form: payload });
            if (publish) {
                await publishForm.mutateAsync(formId!);
            }
        }
    };

    if (formLoading && !isNew) return <LoadingSpinner />;

    const selectedField = formState.fields.find(f => f.field_id === selectedFieldId);

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <Button
                        appearance="transparent"
                        icon={<ArrowLeftRegular />}
                        onClick={() => navigate('/forms')}
                        style={{ marginBottom: '8px' }}
                    >
                        Back to List
                    </Button>
                    <Title3>{isNew ? 'Create New Form' : `Editing: ${formState.name}`}</Title3>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <TabList 
                        selectedValue={viewMode} 
                        onTabSelect={(_, data) => setViewMode(data.value as 'edit' | 'preview')}
                    >
                        <Tab icon={<EditRegular />} value="edit">Editor</Tab>
                        <Tab icon={<EyeRegular />} value="preview">Preview</Tab>
                    </TabList>
                    <Divider vertical style={{ height: '32px' }} />
                    <Button icon={<SaveRegular />} onClick={() => handleSave(false)}>Save Draft</Button>
                    <Button 
                        appearance="primary" 
                        icon={<SendRegular />} 
                        onClick={() => handleSave(true)}
                    >
                        Publish
                    </Button>
                </div>
            </div>

            {viewMode === 'edit' ? (
                <div className={styles.editorLayout}>
                    <div className={styles.mainPanel}>
                        <Card style={{ marginBottom: '24px', padding: '16px' }}>
                            <Field label="Form Name" required>
                                <Input 
                                    value={formState.name} 
                                    onChange={(_, d) => setFormState(p => ({ ...p, name: d.value }))}
                                    placeholder="Enter form name..."
                                />
                            </Field>
                            <Field label="Description">
                                <Textarea 
                                    value={formState.description}
                                    onChange={(_, d) => setFormState(p => ({ ...p, description: d.value }))}
                                    placeholder="Enter form description..."
                                />
                            </Field>
                        </Card>

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                            <Subtitle2>Form Fields</Subtitle2>
                            <Button icon={<AddRegular />} onClick={handleAddField}>Add Field</Button>
                        </div>

                        {formState.fields.map((field, index) => (
                            <Card 
                                key={field.field_id} 
                                className={styles.fieldCard}
                                appearance={selectedFieldId === field.field_id ? 'filled-alternative' : 'outline'}
                                onClick={() => setSelectedFieldId(field.field_id)}
                            >
                                <div className={styles.fieldActions}>
                                    <Button 
                                        size="small" 
                                        appearance="transparent" 
                                        icon={<ArrowUpRegular />} 
                                        disabled={index === 0}
                                        onClick={(e) => { e.stopPropagation(); handleMoveField(index, 'up'); }}
                                    />
                                    <Button 
                                        size="small" 
                                        appearance="transparent" 
                                        icon={<ArrowDownRegular />} 
                                        disabled={index === formState.fields.length - 1}
                                        onClick={(e) => { e.stopPropagation(); handleMoveField(index, 'down'); }}
                                    />
                                    <Button 
                                        size="small" 
                                        appearance="transparent" 
                                        icon={<DeleteRegular />} 
                                        onClick={(e) => { e.stopPropagation(); handleDeleteField(field.field_id); }}
                                    />
                                </div>
                                <Body1><strong>{field.name}</strong></Body1>
                                <Subtitle2 style={{ color: tokens.colorNeutralForeground3 }}>
                                    {field.type} {field.required ? '(Required)' : ''}
                                </Subtitle2>
                            </Card>
                        ))}

                        {formState.fields.length === 0 && (
                            <Card appearance="subtle" style={{ textAlign: 'center', padding: '40px' }}>
                                <Body1>No fields added yet. Click "Add Field" to start building your form.</Body1>
                            </Card>
                        )}
                    </div>

                    <div className={styles.propPanel}>
                        <Card style={{ height: '100%' }}>
                            <Subtitle2>Field Properties</Subtitle2>
                            <Divider style={{ margin: '12px 0' }} />
                            {selectedField ? (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                    <Field label="Label" required>
                                        <Input 
                                            value={selectedField.name}
                                            onChange={(_, d) => handleUpdateField(selectedField.field_id, { name: d.value })}
                                        />
                                    </Field>
                                    <Field label="Type">
                                        <Dropdown
                                            value={selectedField.type}
                                            onOptionSelect={(_, d) => handleUpdateField(selectedField.field_id, { type: d.optionValue as FormFieldType })}
                                        >
                                            {Object.values(FormFieldType).map(type => (
                                                <Option key={type} value={type}>{type}</Option>
                                            ))}
                                        </Dropdown>
                                    </Field>
                                    <Field label="Section">
                                        <Input 
                                            value={selectedField.section || ''}
                                            onChange={(_, d) => handleUpdateField(selectedField.field_id, { section: d.value })}
                                        />
                                    </Field>
                                    <Checkbox 
                                        label="Required" 
                                        checked={selectedField.required}
                                        onChange={(_, d) => handleUpdateField(selectedField.field_id, { required: !!d.checked })}
                                    />

                                    {selectedField.type === FormFieldType.DROPDOWN && (
                                        <Field label="Options (comma separated)">
                                            <Input 
                                                value={selectedField.options?.join(', ') || ''}
                                                onChange={(_, d) => handleUpdateField(selectedField.field_id, { options: d.value.split(',').map(s => s.trim()) })}
                                            />
                                        </Field>
                                    )}
                                </div>
                            ) : (
                                <Body1 style={{ color: tokens.colorNeutralForeground4 }}>
                                    Select a field to edit its properties.
                                </Body1>
                            )}
                        </Card>
                    </div>
                </div>
            ) : (
                <div style={{ maxWidth: '800px', margin: '0 auto' }}>
                    <Card style={{ padding: '24px' }}>
                        <Title3 block>{formState.name}</Title3>
                        <Body1 block style={{ color: tokens.colorNeutralForeground3 }}>{formState.description}</Body1>
                        <Divider style={{ margin: '20px 0' }} />
                        {formState.fields.map(field => (
                            <Field 
                                key={field.field_id} 
                                label={field.name} 
                                required={field.required}
                                style={{ marginBottom: '16px' }}
                            >
                                <Input placeholder={`Preview for ${field.type.toLowerCase()}`} disabled />
                            </Field>
                        ))}
                    </Card>
                </div>
            )}
        </div>
    );
};

export default FormEditorPage;
