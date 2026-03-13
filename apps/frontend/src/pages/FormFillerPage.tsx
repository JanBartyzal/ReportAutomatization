import React, { useState, useEffect, useCallback } from 'react';
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
    ProgressBar,
    Spinner,
    Divider,
    Badge,
} from '@fluentui/react-components';
import {
    SaveRegular,
    SendRegular,
    ArrowLeftRegular,
    CommentRegular,
} from '@fluentui/react-icons';
import { useForm, useFormResponse, useCreateFormResponse, useUpdateFormResponse, useAutoSave } from '../hooks/useForms';
import LoadingSpinner from '../components/LoadingSpinner';

/* Using shared FormField type from @reportplatform/types */
import type { FormField } from '@reportplatform/types';

export const FormFillerPage: React.FC = () => {
    const { formId, responseId } = useParams<{ formId: string; responseId?: string }>();
    const navigate = useNavigate();

    const { data: form, isLoading: formLoading } = useForm(formId!);
    const { data: existingResponse, isLoading: responseLoading } = useFormResponse(formId!, responseId!);
    const createResponse = useCreateFormResponse(formId!);
    const updateResponse = useUpdateFormResponse(formId!);

    const [fields, setFields] = useState<Record<string, string>>({});
    const [comments, setComments] = useState<Record<string, string>>({});
    const [showComments, setShowComments] = useState<Record<string, boolean>>({});
    const [lastSaved, setLastSaved] = useState<Date | null>(null);
    const [completionPercent, setCompletionPercent] = useState(0);

    // Auto-save hook
    const { debouncedSave, isPending: isSaving } = useAutoSave(formId!, responseId!);

    // Calculate completion percentage
    useEffect(() => {
        if (!form?.fields) return;
        const requiredFields = form.fields.filter((f: FormField) => f.required);
        const filledFields = requiredFields.filter((f: FormField) => fields[f.field_id]?.trim());
        setCompletionPercent(Math.round((filledFields.length / requiredFields.length) * 100));
    }, [fields, form]);

    // Initialize fields from existing response
    useEffect(() => {
        if (existingResponse?.fields) {
            const fieldsMap: Record<string, string> = {};
            existingResponse.fields.forEach(f => {
                fieldsMap[f.field_id] = f.value;
            });
            setFields(fieldsMap);
        }
    }, [existingResponse]);

    const handleFieldChange = useCallback((fieldKey: string, value: string) => {
        setFields(prev => {
            const newFields = { ...prev, [fieldKey]: value };
            // Convert to FormFieldValue[] for auto-save if needed
            const fieldValues = Object.entries(newFields).map(([id, val]) => ({
                field_id: id,
                value: val
            }));
            debouncedSave(fieldValues);
            return newFields;
        });
    }, [debouncedSave]);

    const handleSubmit = async () => {
        const responseData = {
            form_id: formId!,
            form_version_id: (form as any).version_id || 'v1',
            org_id: '', // Will be set from auth context
            status: 'SUBMITTED' as const,
            fields: Object.entries(fields).map(([id, val]) => ({
                field_id: id,
                value: val,
                comment: comments[id]
            })),
        };

        if (responseId) {
            await updateResponse.mutateAsync({ responseId, response: responseData });
        } else {
            const newResponse = await createResponse.mutateAsync(responseData);
            navigate(`/forms/${formId}/fill/${newResponse.response_id}`);
        }
    };

    const handleSaveDraft = async () => {
        const responseData = {
            form_id: formId!,
            form_version_id: (form as any).version_id || 'v1',
            org_id: '',
            status: 'DRAFT' as const,
            fields: Object.entries(fields).map(([id, val]) => ({
                field_id: id,
                value: val,
                comment: comments[id]
            })),
        };

        if (responseId) {
            await updateResponse.mutateAsync({ responseId, response: responseData });
        } else {
            const newResponse = await createResponse.mutateAsync(responseData);
            navigate(`/forms/${formId}/fill/${newResponse.response_id}`);
        }
        setLastSaved(new Date());
    };

    // Group fields by section
    const sections = form?.fields?.reduce((acc: Record<string, FormField[]>, field: FormField) => {
        const section = field.section || 'General';
        if (!acc[section]) acc[section] = [];
        acc[section].push(field);
        return acc;
    }, {});

    if (formLoading || responseLoading) {
        return <LoadingSpinner />;
    }

    if (!form) {
        return <div>Form not found</div>;
    }

    return (
        <div style={{ padding: '24px', maxWidth: '900px', margin: '0 auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px' }}>
                <Button
                    appearance="transparent"
                    icon={<ArrowLeftRegular />}
                    onClick={() => navigate('/forms')}
                >
                    Back
                </Button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
                <div>
                    <Title3>{form.name}</Title3>
                    <Subtitle2 style={{ color: 'var(--colorNeutralForeground2)' }}>
                        {form.description}
                    </Subtitle2>
                </div>

                {/* Save indicator */}
                <div style={{ textAlign: 'right' }}>
                    {isSaving ? (
                        <Badge appearance="tint" color="informative">
                            <Spinner size="tiny" style={{ marginRight: '4px' }} /> Saving...
                        </Badge>
                    ) : lastSaved ? (
                        <Badge appearance="tint" color="success">
                            Saved {lastSaved.toLocaleTimeString()}
                        </Badge>
                    ) : null}
                </div>
            </div>

            {/* Completion progress */}
            <div style={{ marginBottom: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <Body1>Completion</Body1>
                    <Body1><strong>{completionPercent}%</strong></Body1>
                </div>
                <ProgressBar 
                    value={completionPercent / 100} 
                    color={completionPercent === 100 ? 'success' : 'brand'}
                />
            </div>

            <Divider style={{ marginBottom: '24px' }} />

            {/* Form sections */}
            {sections && Object.entries(sections).map(([sectionName, sectionFields]) => (
                <div key={sectionName} style={{ marginBottom: '32px' }}>
                    <Title3 style={{ marginBottom: '8px' }}>{sectionName}</Title3>
                    {(sectionFields as FormField[])[0] && ((sectionFields as FormField[])[0] as any).section_description && (
                        <Body1 style={{ color: 'var(--colorNeutralForeground2)', marginBottom: '16px' }}>
                            {((sectionFields as FormField[])[0] as any).section_description}
                        </Body1>
                    )}

                    {(sectionFields as FormField[]).map((field: FormField) => {
                        const value = fields[field.field_id] || '';
                        const isInvalid = field.required && !value.trim();
                        
                        return (
                            <Field
                                key={field.field_id}
                                label={field.name}
                                required={field.required}
                                validationState={isInvalid ? 'error' : 'none'}
                                validationMessage={isInvalid ? 'This field is required' : undefined}
                                style={{ marginBottom: '16px' }}
                            >
                                {field.type === 'TEXT' && (
                                    <Input
                                        value={value}
                                        onChange={(_, data) => handleFieldChange(field.field_id, data.value)}
                                        placeholder={`Enter ${field.name.toLowerCase()}`}
                                    />
                                )}

                                {field.type === 'NUMBER' && (
                                    <Input
                                        type="number"
                                        value={value}
                                        onChange={(_, data) => handleFieldChange(field.field_id, data.value)}
                                        placeholder={`Enter ${field.name.toLowerCase()}`}
                                    />
                                )}

                                {field.type === 'DROPDOWN' && (
                                    <Dropdown
                                        placeholder="Select an option"
                                        value={value}
                                        onOptionSelect={(_, data) => handleFieldChange(field.field_id, String(data.optionValue))}
                                    >
                                        {(field.options as string[])?.map((option) => (
                                            <Option key={option} value={option}>{option}</Option>
                                        ))}
                                    </Dropdown>
                                )}

                                {field.type === 'DATE' && (
                                    <Input
                                        type="date"
                                        value={value}
                                        onChange={(_, data) => handleFieldChange(field.field_id, data.value)}
                                    />
                                )}

                                {/* Comment button */}
                                <Button
                                    appearance="transparent"
                                    size="small"
                                    icon={<CommentRegular />}
                                    onClick={() => setShowComments(prev => ({ ...prev, [field.field_id]: !prev[field.field_id] }))}
                                    style={{ marginTop: '4px' }}
                                >
                                    {comments[field.field_id] ? 'Edit comment' : 'Add comment'}
                                </Button>

                                {/* Comment input */}
                                {showComments[field.field_id] && (
                                    <Textarea
                                        value={comments[field.field_id] || ''}
                                        onChange={(_, data) => setComments(prev => ({ ...prev, [field.field_id]: data.value }))}
                                        placeholder="Add a comment for reviewers..."
                                        style={{ marginTop: '8px' }}
                                    />
                                )}
                            </Field>
                        );
                    })}
                </div>
            ))}

            <Divider style={{ marginTop: '24px', marginBottom: '24px' }} />

            {/* Action buttons */}
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                <Button
                    appearance="secondary"
                    icon={<SaveRegular />}
                    onClick={handleSaveDraft}
                    disabled={createResponse.isPending || updateResponse.isPending}
                >
                    Save Draft
                </Button>
                <Button
                    appearance="primary"
                    icon={<SendRegular />}
                    onClick={handleSubmit}
                    disabled={completionPercent < 100 || createResponse.isPending || updateResponse.isPending}
                >
                    Submit
                </Button>
            </div>
        </div>
    );
};

export default FormFillerPage;
