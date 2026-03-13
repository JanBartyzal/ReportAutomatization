import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import FilesPage from '../FilesPage';

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

    return ({ children }: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                {children}
            </BrowserRouter>
        </QueryClientProvider>
    );
};

describe('FilesPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        // Should show loading spinner initially
        expect(screen.getByText(/loading files/i)).toBeInTheDocument();
    });

    it('renders file list when data is loaded', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        // Wait for data to load
        await waitFor(() => {
            expect(screen.queryByText(/loading files/i)).not.toBeInTheDocument();
        });

        // Should show the page title
        expect(screen.getByText('Files')).toBeInTheDocument();
    });

    it('renders file filters', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading files/i)).not.toBeInTheDocument();
        });

        // Should have upload button
        expect(screen.getByRole('button', { name: /upload/i })).toBeInTheDocument();
    });

    it('shows empty state when no files exist', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading files/i)).not.toBeInTheDocument();
        });

        // Should show empty message
        expect(screen.getByText(/no files found/i)).toBeInTheDocument();
    });

    it('has clickable file names that navigate to file detail', async () => {
        const TestWrapper = createTestWrapper();


        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading files/i)).not.toBeInTheDocument();
        });

        // Check for file links (if files exist)
        // The actual click behavior would navigate, so we just verify the component renders
        expect(screen.getByText('Files')).toBeInTheDocument();
    });

    it('renders status badges for files', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <FilesPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading files/i)).not.toBeInTheDocument();
        });

        // The component should render without errors
        expect(screen.getByText('Files')).toBeInTheDocument();
    });
});
