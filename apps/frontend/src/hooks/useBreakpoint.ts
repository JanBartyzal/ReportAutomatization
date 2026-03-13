/**
 * useBreakpoint — matchMedia-based responsive hook per design system
 * Breakpoints: xs (0-639), sm (640-767), md (768-1023), lg (1024-1279), xl (1280+)
 * Replaces scattered CSS media queries with a unified hook.
 */
import { useState, useEffect } from 'react';
import { breakpoints, type BreakpointKey } from '../theme/tokens';

const orderedBreakpoints: { key: BreakpointKey; min: number }[] = [
  { key: 'xl', min: breakpoints.xl },
  { key: 'lg', min: breakpoints.lg },
  { key: 'md', min: breakpoints.md },
  { key: 'sm', min: breakpoints.sm },
  { key: 'xs', min: breakpoints.xs },
];

function getCurrentBreakpoint(width: number): BreakpointKey {
  for (const bp of orderedBreakpoints) {
    if (width >= bp.min) return bp.key;
  }
  return 'xs';
}

export interface BreakpointResult {
  breakpoint: BreakpointKey;
  width: number;
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
}

export function useBreakpoint(): BreakpointResult {
  const [width, setWidth] = useState(() =>
    typeof window !== 'undefined' ? window.innerWidth : 1280
  );

  useEffect(() => {
    const queries = orderedBreakpoints
      .filter(bp => bp.min > 0)
      .map(bp => window.matchMedia(`(min-width: ${bp.min}px)`));

    function update() {
      setWidth(window.innerWidth);
    }

    queries.forEach(mql => mql.addEventListener('change', update));
    return () => {
      queries.forEach(mql => mql.removeEventListener('change', update));
    };
  }, []);

  const breakpoint = getCurrentBreakpoint(width);

  return {
    breakpoint,
    width,
    isMobile: breakpoint === 'xs' || breakpoint === 'sm',
    isTablet: breakpoint === 'md',
    isDesktop: breakpoint === 'lg' || breakpoint === 'xl',
  };
}
