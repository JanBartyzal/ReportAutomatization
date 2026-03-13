import { useState } from 'react';
import {
    Button,
    makeStyles,
    tokens,
    Popover,
    PopoverTrigger,
    PopoverSurface,
    Badge,
    Spinner,
} from '@fluentui/react-components';
import { useNavigate } from 'react-router-dom';
import {
    Alert24Regular,
    Checkmark24Regular,
    Info24Regular,
    Warning24Regular,
    ErrorCircle24Regular,
} from '@fluentui/react-icons';
import { useNotifications, useMarkAsRead } from '../../hooks/useNotifications';
import type { Notification, NotificationType } from '@reportplatform/types';

const useStyles = makeStyles({
    trigger: {
        position: 'relative',
        padding: tokens.spacingHorizontalS,
    },
    badge: {
        position: 'absolute',
        top: '-4px',
        right: '-4px',
        minWidth: '18px',
        height: '18px',
        fontSize: '11px',
    },
    popoverSurface: {
        width: '380px',
        maxHeight: '500px',
        padding: 0,
        overflow: 'hidden',
        backgroundColor: 'rgba(255, 255, 255, 0.8)',
        backdropFilter: 'blur(20px) saturate(180%)',
        boxShadow: tokens.shadow28, // Level 3 shadow
        '@media (prefers-color-scheme: dark)': {
            backgroundColor: 'rgba(20, 20, 20, 0.8)',
        },
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: tokens.spacingHorizontalM,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    headerTitle: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase400,
    },
    list: {
        maxHeight: '380px',
        overflowY: 'auto',
    },
    notificationItem: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        padding: tokens.spacingHorizontalM,
        cursor: 'pointer',
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        transition: 'background-color 0.15s ease-in-out',
        '&:hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    notificationItemUnread: {
        backgroundColor: tokens.colorBrandBackground2,
    },
    notificationIcon: {
        flexShrink: 0,
        width: '32px',
        height: '32px',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    notificationContent: {
        flex: 1,
        minWidth: 0,
    },
    notificationTitle: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase300,
        marginBottom: '2px',
    },
    notificationBody: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground2,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    notificationTime: {
        fontSize: tokens.fontSizeBase100,
        color: tokens.colorNeutralForeground3,
        marginTop: '4px',
    },
    emptyState: {
        padding: tokens.spacingHorizontalXL,
        textAlign: 'center',
        color: tokens.colorNeutralForeground2,
    },
    footer: {
        padding: tokens.spacingHorizontalM,
        borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
        textAlign: 'center',
    },
});

const getNotificationIcon = (type: NotificationType) => {
    switch (type) {
        case 'FILE_PROCESSED':
            return <Checkmark24Regular style={{ color: tokens.colorPaletteGreenForeground1 }} />;
        case 'FILE_FAILED':
            return <ErrorCircle24Regular style={{ color: tokens.colorPaletteRedForeground1 }} />;
        case 'REPORT_SUBMITTED':
            return <Info24Regular style={{ color: tokens.colorPaletteBlueForeground2 }} />;
        case 'REPORT_APPROVED':
            return <Checkmark24Regular style={{ color: tokens.colorPaletteGreenForeground1 }} />;
        case 'REPORT_REJECTED':
            return <Warning24Regular style={{ color: tokens.colorPaletteDarkOrangeForeground1 }} />;
        case 'DEADLINE_APPROACHING':
        case 'DEADLINE_MISSED':
            return <Warning24Regular style={{ color: tokens.colorPaletteDarkOrangeForeground1 }} />;
        case 'BATCH_COMPLETED':
            return <Checkmark24Regular style={{ color: tokens.colorPaletteGreenForeground1 }} />;
        default:
            return <Alert24Regular style={{ color: tokens.colorNeutralForeground2 }} />;
    }
};

const formatTime = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
};

interface NotificationCenterProps {
    onNotificationClick?: (notification: Notification) => void;
}

export default function NotificationCenter({ onNotificationClick }: NotificationCenterProps) {
    const styles = useStyles();
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const markAsRead = useMarkAsRead();
    const { data: notificationsData, isLoading } = useNotifications({ read: false, page_size: 20 });
    const { data: allNotificationsData } = useNotifications({ page_size: 5 });

    const unreadCount = notificationsData?.data?.filter(n => !n.read).length || 0;
    const notifications = allNotificationsData?.data || [];

    const handleMarkAllAsRead = () => {
        const unreadIds = notificationsData?.data?.filter(n => !n.read).map(n => n.id) || [];
        if (unreadIds.length > 0) {
            markAsRead.mutate(unreadIds);
        }
    };

    const handleNotificationClick = (notification: Notification) => {
        if (!notification.read) {
            markAsRead.mutate([notification.id]);
        }
        
        setOpen(false);

        // Navigation logic based on notification data
        const data = notification.data || {};
        if (data.file_id) {
            navigate(`/files/${data.file_id}`);
        } else if (data.report_id) {
            navigate(`/reports/${data.report_id}`);
        } else if (data.form_id) {
            navigate(`/forms/${data.form_id}/fill`);
        } else if (data.period_id) {
            navigate(`/periods/${data.period_id}`);
        } else {
            onNotificationClick?.(notification);
        }
    };

    return (
        <Popover open={open} onOpenChange={(_ev, data) => setOpen(data.open)}>
            <PopoverTrigger disableButtonEnhancement>
                <Button
                    appearance="subtle"
                    className={styles.trigger}
                    icon={
                        <div style={{ position: 'relative' }}>
                            <Alert24Regular />
                            {unreadCount > 0 && (
                                <Badge
                                    className={styles.badge}
                                    size="small"
                                    appearance="filled"
                                    color="danger"
                                >
                                    {unreadCount > 99 ? '99+' : unreadCount}
                                </Badge>
                            )}
                        </div>
                    }
                    aria-label={`Notifications (${unreadCount} unread)`}
                />
            </PopoverTrigger>

            <PopoverSurface className={styles.popoverSurface}>
                <div className={styles.header}>
                    <span className={styles.headerTitle}>Notifications</span>
                    {unreadCount > 0 && (
                        <Button
                            appearance="subtle"
                            size="small"
                            onClick={handleMarkAllAsRead}
                            disabled={markAsRead.isPending}
                        >
                            Mark all as read
                        </Button>
                    )}
                </div>

                <div className={styles.list}>
                    {isLoading ? (
                        <div className={styles.emptyState}>
                            <Spinner size="small" />
                        </div>
                    ) : notifications.length === 0 ? (
                        <div className={styles.emptyState}>
                            <Alert24Regular style={{ marginBottom: '8px', opacity: 0.5 }} />
                            <p>No notifications yet</p>
                        </div>
                    ) : (
                        notifications.map((notification) => (
                            <div
                                key={notification.id}
                                className={`${styles.notificationItem} ${!notification.read ? styles.notificationItemUnread : ''}`}
                                onClick={() => handleNotificationClick(notification)}
                            >
                                <div className={styles.notificationIcon}>
                                    {getNotificationIcon(notification.type)}
                                </div>
                                <div className={styles.notificationContent}>
                                    <div className={styles.notificationTitle}>{notification.title}</div>
                                    <div className={styles.notificationBody}>{notification.body}</div>
                                    <div className={styles.notificationTime}>
                                        {formatTime(notification.created_at)}
                                    </div>
                                </div>
                                {!notification.read && (
                                    <div
                                        style={{
                                            width: '8px',
                                            height: '8px',
                                            borderRadius: '50%',
                                            backgroundColor: tokens.colorBrandBackground,
                                            flexShrink: 0,
                                            alignSelf: 'center',
                                        }}
                                    />
                                )}
                            </div>
                        ))
                    )}
                </div>

                <div className={styles.footer}>
                    <Button
                        appearance="subtle"
                        onClick={() => setOpen(false)}
                    >
                        View all notifications
                    </Button>
                </div>
            </PopoverSurface>
        </Popover>
    );
}
