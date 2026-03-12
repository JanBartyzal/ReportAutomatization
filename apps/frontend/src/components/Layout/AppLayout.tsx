import { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import {
    Button,
    Persona,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    Home24Regular,
    Document24Regular,
    ArrowUpload24Regular,
    Settings24Regular,
    SignOut24Regular,
    ShieldSettings24Regular,
    HeartPulse24Regular,
    PlugConnected24Regular,
    DataArea24Regular,
} from '@fluentui/react-icons';
import { useMe, useLogout } from '../../hooks/useAuth';
import { useSSE } from '../../hooks/useNotifications';
import { NotificationCenter, useNotificationToast } from '../NotificationCenter';
import { GlobalSearchBar } from '../Search';
import { useEffect } from 'react';

/**
 * AppLayout styles per docs/UX-UI/02-design-system.md and 03-figma-components.md
 * - Sidebar: 260px, solid background, no glassmorphism
 * - TopNav: 64px height with glassmorphism per section 1.2
 */
const useStyles = makeStyles({
    root: {
        display: 'flex',
        minHeight: '100vh',
    },
    sidebar: {
        width: '260px',
        backgroundColor: tokens.colorNeutralBackground1,
        borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
        display: 'flex',
        flexDirection: 'column',
        padding: tokens.spacingHorizontalM,
    },
    logo: {
        fontSize: tokens.fontSizeTitle2,
        fontWeight: '600',
        marginBottom: tokens.spacingVerticalXXL,
        padding: `0 ${tokens.spacingHorizontalS}`,
        color: tokens.colorBrandForeground1,
    },
    nav: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
        flex: 1,
    },
    navLink: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        padding: '10px 12px',
        borderRadius: tokens.borderRadiusMedium,
        textDecoration: 'none',
        color: tokens.colorNeutralForeground1,
        transition: 'background-color 0.2s ease-in-out',
        ':hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    navLinkActive: {
        backgroundColor: tokens.colorBrandBackground1,
        color: tokens.colorNeutralForegroundOnBrand,
        ':hover': {
            backgroundColor: tokens.colorBrandBackground1Hover,
        },
    },
    userSection: {
        marginTop: 'auto',
        paddingTop: tokens.spacingVerticalM,
        borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    main: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'auto',
    },
    /**
     * TopNav with glassmorphism per 02-design-system.md section 1.2
     * Values: rgba(20, 20, 20, 0.8) + backdrop-filter: blur(20px)
     * Note: Using Fluent tokens for light/dark mode compatibility
     */
    header: {
        height: '64px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalM,
        padding: `0 ${tokens.spacingHorizontalL}`,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        backgroundColor: 'rgba(255, 255, 255, 0.7)',
        backdropFilter: 'blur(20px) saturate(180%)',
        position: 'sticky',
        top: 0,
        zIndex: 1000,
        '@media (prefers-color-scheme: dark)': {
            backgroundColor: 'rgba(20, 20, 20, 0.7)',
        },
    },
    content: {
        flex: 1,
        padding: tokens.spacingHorizontalL,
        maxWidth: '1400px',
        width: '100%',
        margin: '0 auto',
    },
});

interface AppLayoutProps {
    children: ReactNode;
}

export default function AppLayout({ children }: AppLayoutProps) {
    const styles = useStyles();
    const { data: user, isLoading } = useMe();
    const logout = useLogout();
    const showToast = useNotificationToast();

    // SSE connection for real-time notifications
    const { lastEvent } = useSSE(
        import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000',
        localStorage.getItem('token')
    );

    // Show toast when a high-priority notification is received via SSE
    useEffect(() => {
        if (lastEvent) {
            showToast({
                type: lastEvent.type,
                title: lastEvent.title,
                body: lastEvent.body,
            });
        }
    }, [lastEvent, showToast]);

    return (
        <div className={styles.root}>
            <aside className={styles.sidebar}>
                <div className={styles.logo}>ReportAutomatization</div>

                <nav className={styles.nav}>
                    <NavLink
                        to="/dashboard"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <Home24Regular />
                        Dashboard
                    </NavLink>
                    <NavLink
                        to="/files"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <Document24Regular />
                        Files
                    </NavLink>
                    <NavLink
                        to="/upload"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <ArrowUpload24Regular />
                        Upload
                    </NavLink>
                    <NavLink
                        to="/settings"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <Settings24Regular />
                        Settings
                    </NavLink>
                    <NavLink
                        to="/admin/holding"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <ShieldSettings24Regular />
                        Holding Admin
                    </NavLink>
                    <NavLink
                        to="/admin/health"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <HeartPulse24Regular />
                        Health
                    </NavLink>
                    <NavLink
                        to="/admin/integrations"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <PlugConnected24Regular />
                        Integrations
                    </NavLink>
                    <NavLink
                        to="/admin/promotions"
                        className={({ isActive }: { isActive: boolean }) =>
                            `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                        }
                    >
                        <DataArea24Regular />
                        Data Promotion
                    </NavLink>
                </nav>

                <div className={styles.userSection}>
                    {!isLoading && user && (
                        <Persona
                            name={user.name || user.email}
                            secondaryText={user.organization?.name}
                            size="medium"
                        />
                    )}
                    <Button
                        appearance="subtle"
                        icon={<SignOut24Regular />}
                        onClick={() => logout.mutate()}
                        style={{ marginTop: '12px', width: '100%' }}
                    >
                        Sign Out
                    </Button>
                </div>
            </aside>

            <main className={styles.main}>
                <header className={styles.header}>
                    <GlobalSearchBar />
                    <NotificationCenter />
                </header>
                <div className={styles.content}>
                    {children}
                </div>
            </main>
        </div>
    );
}
