import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useGenerateReport, useReportGenerationStatus, useGenerationPolling } from '../useGeneration';
import React from 'react';

// Create a test wrapper with providers
const createTestWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
                gcTime: 0,
            },
        },
    });

    return ({ children }: { children?: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            {children}
        </QueryClientProvider>
    );
};

describe('useGenerateReport', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns mutation functions', () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() => useGenerateReport(), { wrapper });

        // Should have mutate function
        expect(result.current.mutate).toBeDefined();
        expect(result.current.mutateAsync).toBeDefined();
    });

    it('has correct initial state', () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() => useGenerateReport(), { wrapper });

        expect(result.current.isPending).toBe(false);
        expect(result.current.isError).toBe(false);
        expect(result.current.isSuccess).toBe(false);
    });
});

describe('useReportGenerationStatus', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('does not fetch when not enabled', () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() =>
            useReportGenerationStatus(null, null, false),
            { wrapper }
        );

        // Should not be fetching
        expect(result.current.isFetching).toBe(false);
    });
});

describe('useGenerationPolling', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns default pending status when not enabled', () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() =>
            useGenerationPolling(null, null),
            { wrapper }
        );

        // Should return default pending status
        expect(result.current.status).toBe('PENDING');
        expect(result.current.isPolling).toBe(false);
    });
});
