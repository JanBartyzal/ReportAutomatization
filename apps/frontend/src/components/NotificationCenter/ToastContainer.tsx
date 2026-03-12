import { create } from 'zustand';
import { useEffect, useState, useCallback } from 'react';
import {
    makeStyles,
    tokens,
    Button,
} from '@fluentui/react-components';
import {
    CheckmarkCircle24Regular,
    Warning24Regular,
    ErrorCircle24Regular,
    Info24Regular,
    Dismiss24Regular,
} from '@fluentui/react-icons';

const useStyles = makeStyles({
    container: {
        position: 'fixed',
        top: '80px',
        right: '20px',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        maxWidth: '400px',
    },
    toast: {
        display: 'flex',
        alignItems: 'flex-start',
        gap: tokens.spacingHorizontalS,
        padding: tokens.spacingHorizontalM,
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground1,
        boxShadow: tokens.shadowShadow64,
        borderLeft: `4px solid ${tokens.colorNeutralStroke1}`,
        animation: 'slideIn 0.3s ease-out',
        '@keyframes slideIn': {
            from: {
                transform: 'translateX(100%)',
                opacity: 0,
            },
            to: {
                transform: 'translateX(0)',
                opacity: 1,
            },
        },
    },
    toastSuccess: {
        borderLeftColor: tokens.colorGreenForeground1,
    },
    toastWarning: {
        borderLeftColor: tokens.colorOrangeForeground1,
    },
    toastError: {
        borderLeftColor: tokens.colorRedForeground1,
    },
    toastInfo: {
        borderLeftColor: tokens.colorBlueForeground1,
    },
    icon: {
        flexShrink: 0,
        width: '24px',
        height: '24px',
    },
    content: {
        flex: 1,
        minWidth: 0,
    },
    title: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase14,
        marginBottom: '2px',
    },
    message: {
        fontSize: tokens.fontSizeBase13,
        color: tokens.colorNeutralForeground2,
    },
    closeButton: {
        flexShrink: 0,
        marginLeft: tokens.spacingHorizontalS,
    },
});

export type ToastType = 'success' | 'warning' | 'error' | 'info';

export interface Toast {
    id: string;
    type: ToastType;
    title: string;
    message?: string;
    duration?: number;
}

interface ToastStore {
    toasts: Toast[];
    addToast: (toast: Omit<Toast, 'id'>) => void;
    removeToast: (id: string) => void;
}

export const useToastStore = create<ToastStore>((set) => ({
    toasts: [],
    addToast: (toast) => {
        const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        set((state) => ({
            toasts: [...state.toasts, { ...toast, id }],
        }));

        // Auto-remove after duration (default 5 seconds)
        const duration = toast.duration ?? 5000;
        if (duration > 0) {
            setTimeout(() => {
                set((state) => ({
                    toasts: state.toasts.filter((t) => t.id !== id),
                }));
            }, duration);
        }
    },
    removeToast: (id) => {
        set((state) => ({
            toasts: state.toasts.filter((t) => t.id !== id),
        }));
    },
}));

export const useToast = () => {
    const addToast = useToastStore((state) => state.addToast);

    return useCallback(
        (type: ToastType, title: string, message?: string, duration?: number) => {
            addToast({ type, title, message, duration });
        },
        [addToast]
    );
};

const getToastIcon = (type: ToastType) => {
    switch (type) {
        case 'success':
            return <CheckmarkCircle24Regular style={{ color: tokens.colorGreenForeground1 }} />;
        case 'warning':
            return <Warning24Regular style={{ color: tokens.colorOrangeForeground1 }} />;
        case 'error':
            return <ErrorCircle24Regular style={{ color: tokens.colorRedForeground1 }} />;
        case 'info':
            return <Info24Regular style={{ color: tokens.colorBlueForeground1 }} />;
    }
};

function ToastItem({ toast, onClose }: { toast: Toast; onClose: () => void }) {
    const styles = useStyles();

    const toastClasses = {
        success: styles.toastSuccess,
        warning: styles.toastWarning,
        error: styles.toastError,
        info: styles.toastInfo,
    };

    return (
        <div className={`${styles.toast} ${toastClasses[toast.type]}`}>
            <div className={styles.icon}>{getToastIcon(toast.type)}</div>
            <div className={styles.content}>
                <div className={styles.title}>{toast.title}</div>
                {toast.message && <div className={styles.message}>{toast.message}</div>}
            </div>
            <Button
                appearance="subtle"
                size="small"
                icon={<Dismiss24Regular />}
                className={styles.closeButton}
                onClick={onClose}
            />
        </div>
    );
}

export default function ToastContainer() {
    const styles = useStyles();
    const toasts = useToastStore((state) => state.toasts);
    const removeToast = useToastStore((state) => state.removeToast);

    if (toasts.length === 0) return null;

    return (
        <div className={styles.container}>
            {toasts.map((toast) => (
                <ToastItem
                    key={toast.id}
                    toast={toast}
                    onClose={() => removeToast(toast.id)}
                />
            ))}
        </div>
    );
}

// Hook to display toast when notification arrives via SSE
export function useNotificationToast() {
    const addToast = useToastStore((state) => state.addToast);

    return useCallback(
        (notification: { type: string; title: string; body: string }) => {
            // Only show high-priority notifications as toasts
            const priorityTypes = ['FILE_FAILED', 'DEADLINE_MISSED', 'REPORT_REJECTED'];
            if (priorityTypes.includes(notification.type)) {
                addToast({
                    type: 'error',
                    title: notification.title,
                    message: notification.body,
                    duration: 8000,
                });
            } else if (notification.type === 'DEADLINE_APPROACHING') {
                addToast({
                    type: 'warning',
                    title: notification.title,
                    message: notification.body,
                    duration: 8000,
                });
            }
        },
        [addToast]
    );
}
