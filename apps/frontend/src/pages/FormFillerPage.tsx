import { useState, useEffect, useCallback } from 'react';
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
    Spinner,
    Progress,
    Divider,
    Badge,
} from '@fluentui/react-components';
import {
    SaveRegular,
    SendRegular,
    ArrowLeftRegular,
    CommentRegular,
} from '@fluentui/react-icons';
import { useForm, useFormResponse, useCreateFormResponse, useUpdateFormResponse, useAutoSave } from '../../hooks/useForms';
import LoadingSpinner from '../LoadingSpinner';

interface FormField {
    field_key: string;
    field_type: string;
    label: string;
    section?: string;
    section_description?: string;
    required: boolean;
    properties: Record<string, unknown>;
}

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
    const { debouncedSave, saveNow, isPending: isSaving } = useAutoSave(formId!, responseId!);

    // Calculate completion percentage
    useEffect(() => {
        if (!form?.fields) return;
        const requiredFields = form.fields.filter((f: FormField) => f.required);
        const filledFields = requiredFields.filter((f: FormField) => fields[f.field_key]?.trim());
        setCompletionPercent(Math.round((filledFields.length / requiredFields.length) * 100));
    }, [fields, form]);

    // Initialize fields from existing response
    useEffect(() => {
        if (existingResponse?.fields) {
            setFields(existingResponse.fields);
        }
    }, [existingResponse]);

    const handleFieldChange = useCallback((fieldKey: string, value: string) => {
        setFields(prev => ({ ...prev, [fieldKey]: value }));
        debouncedSave({ ...fields, [fieldKey]: value });
    }, [debouncedSave, fields]);

    const handleSubmit = async () => {
        const responseData = {
            org_id: '', // Will be set from auth context
            user_id: '', // Will be set from auth context
            status: 'SUBMITTED',
            fields,
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
            org_id: '',
            user_id: '',
            status: 'DRAFT',
            fields,
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
                    <Title3>{form.title}</Title3>
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
                <Progress 
                    value={completionPercent} 
                    color={completionPercent === 100 ? 'success' : 'brand'}
                />
            </div>

            <Divider style={{ marginBottom: '24px' }} />

            {/* Form sections */}
            {sections && Object.entries(sections).map(([sectionName, sectionFields]) => (
                <div key={sectionName} style={{ marginBottom: '32px' }}>
                    <Title3 style={{ marginBottom: '8px' }}>{sectionName}</Title3>
                    {sectionFields[0]?.section_description && (
                        <Body1 style={{ color: 'var(--colorNeutralForeground2)', marginBottom: '16px' }}>
                            {sectionFields[0].section_description}
                        </Body1>
                    )}

                    {sectionFields.map((field: FormField) => {
                        const value = fields[field.field_key] || '';
                        const isInvalid = field.required && !value.trim();
                        
                        return (
                            <Field
                                key={field.field_key}
                                label={field.label}
                                required={field.required}
                                validationState={isInvalid ? 'error' : 'none'}
                                validationMessage={isInvalid ? 'This field is required' : undefined}
                                style={{ marginBottom: '16px' }}
                            >
                                {field.field_type === 'text' && (
                                    field.properties?.max_length ? (
                                        <Textarea
                                            value={value}
                                            onChange={(_, data) => handleFieldChange(field.field_key, data.value)}
                                            placeholder={`Enter ${field.label.toLowerCase()}`}
                                            maxLength={field.properties.max_length as number}
                                        />
                                    ) : (
                                        <Input
                                            value={value}
                                            onChange={(_, data) => handleFieldChange(field.field_key, data.value)}
                                            placeholder={`Enter ${field.label.toLowerCase()}`}
                                        />
                                    )
                                )}

                                {field.field_type === 'number' && (
                                    <Input
                                        type="number"
                                        value={value}
                                        onChange={(_, data) => handleFieldChange(field.field_key, data.value)}
                                        placeholder={`Enter ${field.label.toLowerCase()}`}
                                        contentAfter={field.properties?.currency === 'CZK' ? 'CZK' :
                                            field.properties?.unit ? String(field.properties.unit) : null}
                                    />
                                )}

                                {field.field_type === 'dropdown' && (
                                    <Dropdown
                                        placeholder="Select an option"
                                        value={value}
                                        onOptionSelect={(_, data) => handleFieldChange(field.field_key, String(data.optionValue))}
                                    >
                                        {(field.properties?.options as string[])?.map((option) => (
                                            <Option key={option} value={option}>{option}</Option>
                                        ))}
                                    </Dropdown>
                                )}

                                {field.field_type === 'date' && (
                                    <Input
                                        type="date"
                                        value={value}
                                        onChange={(_, data) => handleFieldChange(field.field_key, data.value)}
                                    />
                                )}

                                {/* Comment button */}
                                <Button
                                    appearance="transparent"
                                    size="small"
                                    icon={<CommentRegular />}
                                    onClick={() => setShowComments(prev => ({ ...prev, [field.field_key]: !prev[field.field_key] }))}
                                    style={{ marginTop: '4px' }}
                                >
                                    {comments[field.field_key] ? 'Edit comment' : 'Add comment'}
                                </Button>

                                {/* Comment input */}
                                {showComments[field.field_key] && (
                                    <Textarea
                                        value={comments[field.field_key] || ''}
                                        onChange={(_, data) => setComments(prev => ({ ...prev, [field.field_key]: data.value }))}
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
