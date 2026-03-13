/**
 * ContentCard — generic content card wrapper per design system
 * radiusMd (8px), Stroke1 1px border, spacingL padding
 * Shadow: Level 1, Hover: Level 2 (transition 200ms)
 */
import React from 'react';
import { makeStyles, tokens } from '@fluentui/react-components';
import type { ReactNode } from 'react';
import { elevation } from '../../theme/tokens';

const useStyles = makeStyles({
  root: {
    backgroundColor: tokens.colorNeutralBackground1,
    borderRadius: '8px',
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    padding: '24px',
    boxShadow: elevation.level1,
    transitionProperty: 'box-shadow',
    transitionDuration: '200ms',
    transitionTimingFunction: 'ease-in-out',
  },
  hoverable: {
    ':hover': {
      boxShadow: elevation.level2,
    },
  },
});

export interface ContentCardProps {
  children: ReactNode;
  hoverable?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

export function ContentCard({ children, hoverable = true, className, style }: ContentCardProps) {
  const styles = useStyles();

  return (
    <div
      className={`${styles.root} ${hoverable ? styles.hoverable : ''} ${className ?? ''}`}
      style={style}
    >
      {children}
    </div>
  );
}
