/**
 * TopNav — glassmorphism navigation bar per design system
 * rgba(20, 20, 20, 0.8) + backdrop-filter: blur(20px)
 * Height: 64px, fixed position, Level 2 shadow
 */
import { makeStyles, tokens } from '@fluentui/react-components';
import type { ReactNode } from 'react';
import { useAppTheme } from '../../theme';

const useStyles = makeStyles({
  root: {
    height: '64px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: tokens.spacingHorizontalM,
    paddingLeft: tokens.spacingHorizontalL,
    paddingRight: tokens.spacingHorizontalL,
    borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
    backdropFilter: 'blur(20px) saturate(180%)',
    position: 'sticky',
    top: 0,
    zIndex: 1000,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
  },
  light: {
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
  },
  dark: {
    backgroundColor: 'rgba(20, 20, 20, 0.8)',
  },
});

export interface TopNavProps {
  children?: ReactNode;
}

export function TopNav({ children }: TopNavProps) {
  const styles = useStyles();
  const { resolved } = useAppTheme();

  return (
    <header className={`${styles.root} ${resolved === 'dark' ? styles.dark : styles.light}`}>
      {children}
    </header>
  );
}
