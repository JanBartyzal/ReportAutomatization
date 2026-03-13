/**
 * ModalOverlay — glassmorphism overlay per design system
 * rgba(0, 0, 0, 0.4) + blur(4px) on backdrop
 * Content: Level 4 shadow, radiusLg
 */
import { makeStyles } from '@fluentui/react-components';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  backdrop: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    backdropFilter: 'blur(4px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  },
  content: {
    borderRadius: '12px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.16)',
    maxWidth: '90vw',
    maxHeight: '90vh',
    overflow: 'auto',
  },
});

export interface ModalOverlayProps {
  open: boolean;
  onDismiss?: () => void;
  children: ReactNode;
}

export function ModalOverlay({ open, onDismiss, children }: ModalOverlayProps) {
  const styles = useStyles();

  if (!open) return null;

  return (
    <div
      className={styles.backdrop}
      onClick={(e) => {
        if (e.target === e.currentTarget) onDismiss?.();
      }}
    >
      <div className={styles.content}>{children}</div>
    </div>
  );
}
