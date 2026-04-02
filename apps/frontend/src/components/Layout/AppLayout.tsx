/**
 * AppLayout — composed from Sidebar, TopNav, AppShell layout components
 * Per docs/UX-UI/02-design-system.md and 03-figma-components.md
 */
import { type ReactNode, useEffect, useState } from 'react';
import { makeStyles } from '@fluentui/react-components';
import {
  Home24Regular,
  Document24Regular,
  ArrowUpload24Regular,
  Settings24Regular,
  ShieldSettings24Regular,
  HeartPulse24Regular,
  PlugConnected24Regular,
  DataArea24Regular,
  Grid24Regular,
  CalendarLtr24Regular,
  Form24Regular,
  DocumentBulletList24Regular,
  TableSimple24Regular,
  Notebook24Regular,
  People24Regular,
} from '@fluentui/react-icons';
import { useMe, useLogout } from '../../hooks/useAuth';
import { useSSE } from '../../hooks/useNotifications';
import { NotificationCenter, useNotificationToast } from '../NotificationCenter';
import { GlobalSearchBar } from '../Search';
import { useBreakpoint } from '../../hooks/useBreakpoint';
import { TopNav } from './TopNav';
import { Sidebar, type NavSection } from './Sidebar';
import { AppShell } from './AppShell';
import { OrgSwitcher } from './OrgSwitcher';

const useStyles = makeStyles({
  root: {
    display: 'flex',
    minHeight: '100vh',
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    overflow: 'auto',
  },
});

const navSections: NavSection[] = [
  {
    items: [
      { to: '/dashboard', icon: <Home24Regular />, label: 'Dashboard' },
      { to: '/files', icon: <Document24Regular />, label: 'Files' },
      { to: '/upload', icon: <ArrowUpload24Regular />, label: 'Upload' },
      { to: '/reports', icon: <DocumentBulletList24Regular />, label: 'Reports' },
      { to: '/periods', icon: <CalendarLtr24Regular />, label: 'Periods' },
      { to: '/forms', icon: <Form24Regular />, label: 'Forms' },
      { to: '/templates', icon: <Notebook24Regular />, label: 'Templates' },
      { to: '/dashboards', icon: <Grid24Regular />, label: 'Dashboards' },
      { to: '/matrix', icon: <TableSimple24Regular />, label: 'Matrix' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { to: '/admin/holding', icon: <ShieldSettings24Regular />, label: 'Holding Admin' },
      { to: '/admin/health', icon: <HeartPulse24Regular />, label: 'Health' },
      { to: '/admin/integrations', icon: <PlugConnected24Regular />, label: 'Integrations' },
      { to: '/admin/promotions', icon: <DataArea24Regular />, label: 'Data Promotion' },
      { to: '/admin/manage', icon: <People24Regular />, label: 'Admin Panel' },
      { to: '/settings', icon: <Settings24Regular />, label: 'Settings' },
    ],
  },
];

interface AppLayoutProps {
  children: ReactNode;
}

export default function AppLayout({ children }: AppLayoutProps) {
  const styles = useStyles();
  const { data: user, isLoading } = useMe();
  const logout = useLogout();
  const showToast = useNotificationToast();
  const { isMobile } = useBreakpoint();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Auto-collapse sidebar on mobile
  useEffect(() => {
    setSidebarCollapsed(isMobile);
  }, [isMobile]);

  // SSE connection for real-time notifications
  const { lastEvent } = useSSE(
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000',
    localStorage.getItem('token')
  );

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
      <Sidebar
        sections={navSections}
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed((c) => !c)}
        user={
          !isLoading && user
            ? { 
                name: user.display_name || user.email, 
                secondaryText: user.organizations.find(o => o.id === user.active_org_id)?.name || user.organizations[0]?.name 
              }
            : null
        }
        onLogout={() => logout.mutate()}
      />

      <main className={styles.main}>
        <TopNav>
          <GlobalSearchBar />
          <OrgSwitcher />
          <NotificationCenter />
        </TopNav>
        <AppShell>{children}</AppShell>
      </main>
    </div>
  );
}
