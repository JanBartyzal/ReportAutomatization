import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    makeStyles,
    Title1,
    Input,
    Button,
    Text,
    Card,
    CardHeader,
    CardFooter,
    useId,
    Toast,
    ToastTitle,
    useToastController,
    Toaster,
    Spinner,
} from '@fluentui/react-components';
import { createBatch } from '../../api/batches';
import { ArrowLeft24Regular } from '@fluentui/react-icons';

const useStyles = makeStyles({
    container: {
        padding: '2rem',
        maxWidth: '600px',
        margin: '0 auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '2rem',
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
        marginBottom: '1rem',
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
        padding: '1.5rem',
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: '0.5rem',
    },
});

export const BatchCreate: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const { dispatchToast } = useToastController('toaster');
    const inputId = useId('batch-name');
    const [name, setName] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) return;

        setIsSubmitting(true);
        try {
            await createBatch({ name });
            dispatchToast(
                <Toast>
                    <ToastTitle>Batch created successfully</ToastTitle>
                </Toast>,
                { intent: 'success' }
            );
            // Navigate back to Upload Opex page or Opex Overview
            setTimeout(() => navigate('/import/upload'), 1000);
        } catch (error) {
            console.error('Failed to create batch', error);
            dispatchToast(
                <Toast>
                    <ToastTitle>Failed to create batch</ToastTitle>
                </Toast>,
                { intent: 'error' }
            );
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className={styles.container}>
            <Toaster toasterId="toaster" />
            <div className={styles.header}>
                <Button
                    appearance="subtle"
                    icon={<ArrowLeft24Regular />}
                    onClick={() => navigate(-1)}
                    aria-label="Go back"
                />
                <Title1>Create New Batch</Title1>
            </div>

            <Card className={styles.form}>
                <CardHeader header={<Text weight="semibold">Batch Details</Text>} />

                <form onSubmit={handleSubmit}>
                    <div className={styles.field}>
                        <label htmlFor={inputId}>Batch Name</label>
                        <Input
                            id={inputId}
                            value={name}
                            onChange={(e, data) => setName(data.value)}
                            placeholder="e.g., Q1 2024 OPEX Import"
                            disabled={isSubmitting}
                            required
                        />
                        <Text size={200} style={{ color: 'gray' }}>
                            Give your batch a descriptive name to identify it later.
                        </Text>
                    </div>

                    <div style={{ marginTop: '2rem', display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                        <Button appearance="secondary" onClick={() => navigate(-1)} disabled={isSubmitting}>
                            Cancel
                        </Button>
                        <Button
                            appearance="primary"
                            type="submit"
                            disabled={!name.trim() || isSubmitting}
                            icon={isSubmitting ? <Spinner size="tiny" /> : undefined}
                        >
                            Create Batch
                        </Button>
                    </div>
                </form>
            </Card>
        </div>
    );
};
