/**
 * ThemeProvider — Fluent UI theme provider with JSON-driven configuration
 * Supports light / dark / system theme switching via React context.
 */
import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import {
  FluentProvider,
  type Theme,
} from '@fluentui/react-components';
import { lightTheme, darkTheme } from './brandTokens';
import { injectChartCssProperties } from './chartColors';
import { injectBrandCssProperties } from './cssInjection';

export type ThemeMode = 'light' | 'dark' | 'system';

interface ThemeContextValue {
  mode: ThemeMode;
  resolved: 'light' | 'dark';
  setMode: (mode: ThemeMode) => void;
  theme: Theme;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const STORAGE_KEY = 'app-theme-mode';

function getSystemPreference(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function resolveTheme(mode: ThemeMode): 'light' | 'dark' {
  return mode === 'system' ? getSystemPreference() : mode;
}

interface ThemeProviderProps {
  children: ReactNode;
  defaultMode?: ThemeMode;
}

export function ThemeProvider({ children, defaultMode = 'system' }: ThemeProviderProps) {
  const [mode, setModeState] = useState<ThemeMode>(() => {
    if (typeof window === 'undefined') return defaultMode;
    return (localStorage.getItem(STORAGE_KEY) as ThemeMode) || defaultMode;
  });

  const [resolved, setResolved] = useState<'light' | 'dark'>(() => resolveTheme(mode));

  const setMode = useCallback((newMode: ThemeMode) => {
    setModeState(newMode);
    localStorage.setItem(STORAGE_KEY, newMode);
  }, []);

  // Listen for system preference changes when mode === 'system'
  useEffect(() => {
    setResolved(resolveTheme(mode));

    if (mode !== 'system') return;

    const mql = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (e: MediaQueryListEvent) => {
      setResolved(e.matches ? 'dark' : 'light');
    };
    mql.addEventListener('change', handler);
    return () => mql.removeEventListener('change', handler);
  }, [mode]);

  // Inject CSS custom properties on mount and when theme changes
  useEffect(() => {
    injectChartCssProperties();
    injectBrandCssProperties();
  }, [resolved]);

  const theme = resolved === 'dark' ? darkTheme : lightTheme;

  return (
    <ThemeContext.Provider value={{ mode, resolved, setMode, theme }}>
      <FluentProvider theme={theme}>
        {children}
      </FluentProvider>
    </ThemeContext.Provider>
  );
}

/**
 * Hook to access theme context (mode, resolved theme, setMode, theme object)
 */
export function useAppTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useAppTheme must be used within a ThemeProvider');
  }
  return ctx;
}
