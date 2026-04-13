/**
 * Feature Flag Hooks
 * Manages feature flags for P6-W4 Local Scope & Advanced Comparison
 */

import { useState, useEffect, useCallback } from 'react';

// Feature flag configuration
export const FEATURE_FLAGS = {
    ENABLE_LOCAL_SCOPE: 'ENABLE_LOCAL_SCOPE',
    ENABLE_ADVANCED_COMPARISON: 'ENABLE_ADVANCED_COMPARISON',
    ENABLE_MULTI_ORG_COMPARISON: 'ENABLE_MULTI_ORG_COMPARISON',
    ENABLE_EXPORT_FLOWS: 'ENABLE_EXPORT_FLOWS',
} as const;

// Default feature flag values (can be overridden by environment or API)
const DEFAULT_FLAGS: Record<string, boolean> = {
    [FEATURE_FLAGS.ENABLE_LOCAL_SCOPE]: import.meta.env.VITE_ENABLE_LOCAL_SCOPE === 'true',
    [FEATURE_FLAGS.ENABLE_ADVANCED_COMPARISON]: import.meta.env.VITE_ENABLE_ADVANCED_COMPARISON === 'true',
    [FEATURE_FLAGS.ENABLE_MULTI_ORG_COMPARISON]: import.meta.env.VITE_ENABLE_MULTI_ORG_COMPARISON === 'true',
    [FEATURE_FLAGS.ENABLE_EXPORT_FLOWS]: import.meta.env.VITE_EXPORT_FLOWS_ENABLED !== 'false',
};

// Get initial flag state
const getInitialFlags = (): Record<string, boolean> => {
    if (typeof window === 'undefined') return DEFAULT_FLAGS;

    // Check sessionStorage for runtime overrides (useful for testing)
    const stored = sessionStorage.getItem('featureFlags');
    if (stored) {
        try {
            return { ...DEFAULT_FLAGS, ...JSON.parse(stored) };
        } catch {
            return DEFAULT_FLAGS;
        }
    }
    return DEFAULT_FLAGS;
};

/**
 * Hook to manage feature flags state and operations
 */
export function useFeatureFlags() {
    const [flags, setFlags] = useState<Record<string, boolean>>(getInitialFlags);
    const [isLoading, setIsLoading] = useState(true);

    // Load flags from backend on mount (optional enhancement)
    useEffect(() => {
        // For now, use client-side flags
        // Future: fetch from backend API
        setIsLoading(false);
    }, []);

    // Check if a specific feature is enabled
    const isEnabled = useCallback(
        (flag: string): boolean => {
            return flags[flag] ?? false;
        },
        [flags]
    );

    // Enable a feature flag (for runtime testing)
    const enableFeature = useCallback((flag: string) => {
        setFlags((prev: Record<string, boolean>) => {
            const updated: Record<string, boolean> = { ...prev, [flag]: true };
            sessionStorage.setItem('featureFlags', JSON.stringify(updated));
            return updated;
        });
    }, []);

    // Disable a feature flag
    const disableFeature = useCallback((flag: string) => {
        setFlags((prev: Record<string, boolean>) => {
            const updated: Record<string, boolean> = { ...prev, [flag]: false };
            sessionStorage.setItem('featureFlags', JSON.stringify(updated));
            return updated;
        });
    }, []);

    // Toggle a feature flag
    const toggleFeature = useCallback((flag: string) => {
        setFlags((prev: Record<string, boolean>) => {
            const updated: Record<string, boolean> = { ...prev, [flag]: !prev[flag] };
            sessionStorage.setItem('featureFlags', JSON.stringify(updated));
            return updated;
        });
    }, []);

    return {
        flags,
        isLoading,
        isEnabled,
        enableFeature,
        disableFeature,
        toggleFeature,
    };
}

/**
 * Hook to check if Local Scope feature is enabled
 */
export function useLocalScope(): boolean {
    const { isEnabled } = useFeatureFlags();
    return isEnabled(FEATURE_FLAGS.ENABLE_LOCAL_SCOPE);
}

/**
 * Hook to check if Advanced Comparison feature is enabled
 */
export function useAdvancedComparison(): boolean {
    const { isEnabled } = useFeatureFlags();
    return isEnabled(FEATURE_FLAGS.ENABLE_ADVANCED_COMPARISON);
}

/**
 * Hook to check if Multi-Org Comparison feature is enabled
 */
export function useMultiOrgComparison(): boolean {
    const { isEnabled } = useFeatureFlags();
    return isEnabled(FEATURE_FLAGS.ENABLE_MULTI_ORG_COMPARISON);
}

/**
 * Hook to check if Export Flows (FS27) feature is enabled
 */
export function useExportFlowsEnabled(): boolean {
    const { isEnabled } = useFeatureFlags();
    return isEnabled(FEATURE_FLAGS.ENABLE_EXPORT_FLOWS);
}