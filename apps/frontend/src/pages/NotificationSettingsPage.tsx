import {
    Title2,
    Title3,
    Body1,
    makeStyles,
    tokens,
    Switch,
    Card,
} from '@fluentui/react-components';
import { useNotificationSettings, useUpdateNotificationSettings } from '../hooks/useNotifications';
import LoadingSpinner from '../components/LoadingSpinner';
import type { TypePreference } from '@reportplatform/types';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '800px',
    },
    header: {
        marginBottom: tokens.spacingHorizontalL,
    },
    section: {
        marginBottom: tokens.spacingHorizontalXL,
    },
    card: {
        padding: tokens.spacingHorizontalL,
        marginBottom: tokens.spacingHorizontalM,
    },
    cardHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalM,
    },
    toggleRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: `${tokens.spacingVerticalS} 0`,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        '&:last-child': {
            borderBottom: 'none',
        },
    },
    toggleLabel: {
        display: 'flex',
        flexDirection: 'column',
    },
    toggleTitle: {
        fontWeight: tokens.fontWeightSemibold,
    },
    toggleDescription: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground2,
    },
    globalSettings: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
});

const NOTIFICATION_TYPE_INFO: { type: any; title: string; description: string }[] = [
    { type: 'FILE_PROCESSED', title: 'File Processed', description: 'When a file has been successfully processed' },
    { type: 'FILE_FAILED', title: 'File Failed', description: 'When a file processing has failed' },
    { type: 'REPORT_SUBMITTED', title: 'Report Submitted', description: 'When a new report is submitted for review' },
    { type: 'REPORT_APPROVED', title: 'Report Approved', description: 'When a report is approved' },
    { type: 'REPORT_REJECTED', title: 'Report Rejected', description: 'When a report is rejected' },
    { type: 'DEADLINE_APPROACHING', title: 'Deadline Approaching', description: 'When a deadline is approaching (within 3 days)' },
    { type: 'DEADLINE_MISSED', title: 'Deadline Missed', description: 'When a deadline has been missed' },
    { type: 'BATCH_COMPLETED', title: 'Batch Completed', description: 'When a batch processing job completes' },
];

export default function NotificationSettingsPage() {
    const styles = useStyles();
    const { data: settings, isLoading } = useNotificationSettings();
    const updateSettings = useUpdateNotificationSettings();

    if (isLoading) {
        return <LoadingSpinner label="Loading notification settings..." />;
    }

    const currentSettings = settings || {
        email_enabled: true,
        in_app_enabled: true,
        preferences: {},
    };

    const handleGlobalToggle = (key: 'email_enabled' | 'in_app_enabled', checked: boolean) => {
        updateSettings.mutate({
            ...currentSettings,
            [key]: checked,
        });
    };

    const handleTypeToggle = (type: any, channel: 'email' | 'in_app', checked: boolean) => {
        const currentPrefs = currentSettings.preferences || {} as any;
        const typePref = (currentPrefs as any)[type] || { email: true, in_app: true };

        updateSettings.mutate({
            ...currentSettings,
            preferences: {
                ...currentPrefs,
                [type]: {
                    ...typePref,
                    [channel]: checked,
                },
            },
        });
    };

    const getTypePreference = (type: any): TypePreference => {
        return (currentSettings.preferences as any)?.[type] || { email: true, in_app: true };
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Title2 block>Notification Settings</Title2>
                <Body1 block style={{ color: tokens.colorNeutralForeground2, marginTop: '8px' }}>
                    Configure how you receive notifications
                </Body1>
            </div>

            <div className={styles.section}>
                <Title3>Global Settings</Title3>
                <Card className={styles.card}>
                    <div className={styles.globalSettings}>
                        <div className={styles.toggleRow}>
                            <div className={styles.toggleLabel}>
                                <span className={styles.toggleTitle}>Email Notifications</span>
                                <span className={styles.toggleDescription}>
                                    Receive notifications via email
                                </span>
                            </div>
                            <Switch
                                checked={currentSettings.email_enabled}
                                onChange={(_ev, data) => handleGlobalToggle('email_enabled', data.checked)}
                            />
                        </div>
                        <div className={styles.toggleRow}>
                            <div className={styles.toggleLabel}>
                                <span className={styles.toggleTitle}>In-App Notifications</span>
                                <span className={styles.toggleDescription}>
                                    Receive notifications within the application
                                </span>
                            </div>
                            <Switch
                                checked={currentSettings.in_app_enabled}
                                onChange={(_ev, data) => handleGlobalToggle('in_app_enabled', data.checked)}
                            />
                        </div>
                    </div>
                </Card>
            </div>

            <div className={styles.section}>
                <Title3 block>Notification Types</Title3>
                <Body1 block style={{ color: tokens.colorNeutralForeground2, marginBottom: '16px' }}>
                    Enable or disable specific notification types
                </Body1>

                {NOTIFICATION_TYPE_INFO.map((info) => {
                    const pref = getTypePreference(info.type);
                    return (
                        <Card key={info.type} className={styles.card}>
                            <div className={styles.cardHeader}>
                                <div className={styles.toggleLabel}>
                                    <span className={styles.toggleTitle}>{info.title}</span>
                                    <span className={styles.toggleDescription}>{info.description}</span>
                                </div>
                            </div>
                            <div className={styles.toggleRow}>
                                <span>Email</span>
                                <Switch
                                    checked={pref.email}
                                    disabled={!currentSettings.email_enabled}
                                    onChange={(_ev, data) =>
                                        handleTypeToggle(info.type, 'email', data.checked)
                                    }
                                />
                            </div>
                            <div className={styles.toggleRow}>
                                <span>In-App</span>
                                <Switch
                                    checked={pref.in_app}
                                    disabled={!currentSettings.in_app_enabled}
                                    onChange={(_ev, data) =>
                                        handleTypeToggle(info.type, 'in_app', data.checked)
                                    }
                                />
                            </div>
                        </Card>
                    );
                })}
            </div>
        </div>
    );
}
