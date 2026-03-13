/**
 * Sidebar — solid background navigation per design system
 * Expanded: 260px, Collapsed: 60px, Transition: 200ms ease-in-out
 * Fluent icons: 24Regular for nav, *Filled for active
 * Section headers: Caption (size 100), uppercase, 0.05em letter-spacing
 * Solid background (NOT glassmorphism per design system rules)
 */
import { makeStyles, tokens, Button, Persona } from '@fluentui/react-components';
import {
  Navigation24Regular,
  SignOut24Regular,
} from '@fluentui/react-icons';
import { NavLink } from 'react-router-dom';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: tokens.colorNeutralBackground1,
    borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
    transitionProperty: 'width',
    transitionDuration: '200ms',
    transitionTimingFunction: 'ease-in-out',
    overflow: 'hidden',
    flexShrink: 0,
  },
  expanded: {
    width: '260px',
    padding: tokens.spacingHorizontalM,
  },
  collapsed: {
    width: '60px',
    padding: tokens.spacingHorizontalXS,
    alignItems: 'center',
  },
  logoArea: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: '40px',
    paddingLeft: tokens.spacingHorizontalS,
    paddingRight: tokens.spacingHorizontalS,
  },
  logoText: {
    fontSize: tokens.fontSizeBase500,
    fontWeight: '600',
    color: tokens.colorBrandForeground1,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
  },
  sectionHeader: {
    fontSize: '10px',
    fontWeight: '400',
    lineHeight: '1.4',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    color: tokens.colorNeutralForeground3,
    padding: '16px 8px 4px',
    whiteSpace: 'nowrap',
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
    transitionProperty: 'background-color',
    transitionDuration: '200ms',
    transitionTimingFunction: 'ease-in-out',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    ':hover': {
      backgroundColor: tokens.colorNeutralBackground1Hover,
    },
  },
  navLinkActive: {
    backgroundColor: tokens.colorBrandBackground,
    color: tokens.colorNeutralForegroundOnBrand,
    ':hover': {
      backgroundColor: tokens.colorBrandBackgroundHover,
    },
  },
  userSection: {
    marginTop: 'auto',
    paddingTop: tokens.spacingVerticalM,
    borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
  },
});

export interface NavItem {
  to: string;
  icon: ReactNode;
  label: string;
}

export interface NavSection {
  title?: string;
  items: NavItem[];
}

export interface SidebarProps {
  sections: NavSection[];
  collapsed?: boolean;
  onToggle?: () => void;
  user?: { name: string; secondaryText?: string } | null;
  onLogout?: () => void;
}

export function Sidebar({
  sections,
  collapsed = false,
  onToggle,
  user,
  onLogout,
}: SidebarProps) {
  const styles = useStyles();

  return (
    <aside className={`${styles.root} ${collapsed ? styles.collapsed : styles.expanded}`}>
      <div className={styles.logoArea}>
        {!collapsed && <span className={styles.logoText}>ReportPlatform</span>}
        {onToggle && (
          <Button
            appearance="subtle"
            icon={<Navigation24Regular />}
            size="small"
            onClick={onToggle}
          />
        )}
      </div>

      <nav className={styles.nav}>
        {sections.map((section, si) => (
          <div key={si}>
            {section.title && !collapsed && (
              <div className={styles.sectionHeader}>{section.title}</div>
            )}
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
                }
                title={collapsed ? item.label : undefined}
              >
                {item.icon}
                {!collapsed && item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className={styles.userSection}>
        {user && !collapsed && (
          <Persona
            name={user.name}
            secondaryText={user.secondaryText}
            size="medium"
          />
        )}
        {onLogout && (
          <Button
            appearance="subtle"
            icon={<SignOut24Regular />}
            onClick={onLogout}
            style={{ marginTop: '12px', width: '100%' }}
          >
            {!collapsed && 'Sign Out'}
          </Button>
        )}
      </div>
    </aside>
  );
}
