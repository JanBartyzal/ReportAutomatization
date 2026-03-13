import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth, useMe } from '../useAuth';
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

describe('useMe', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns user context data', async () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() => useMe(), { wrapper });

        // Initially loading
        expect(result.current.isLoading).toBe(true);

        // Wait for data
        await waitFor(() => {
            expect(result.current.isSuccess).toBe(true);
        });

        // Should have data
        expect(result.current.data).toBeDefined();
    });
});

describe('useAuth', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('returns auth state with user data', async () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() => useAuth(), { wrapper });

        await waitFor(() => {
            expect(result.current.isSuccess).toBe(true);
        });

        // Should have user data
        expect(result.current.data).toBeDefined();
    });

    it('includes logout function', async () => {
        const wrapper = createTestWrapper();

        const { result } = renderHook(() => useAuth(), { wrapper });

        await waitFor(() => {
            expect(result.current.isSuccess).toBe(true);
        });

        // Should have logout function
        expect(result.current.logout).toBeDefined();
        expect(result.current.logout.mutate).toBeDefined();
    });
});
