import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReportsPage } from '../ReportsPage';

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

describe('ReportsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <ReportsPage />
            </TestWrapper>
        );

        // Should show loading spinner initially
        expect(screen.getByText(/loading reports/i)).toBeInTheDocument();
    });

    it('renders reports page title', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <ReportsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading reports/i)).not.toBeInTheDocument();
        });

        // Should show the page title
        expect(screen.getByText('Reports')).toBeInTheDocument();
    });

    it('renders filter dropdowns', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <ReportsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading reports/i)).not.toBeInTheDocument();
        });

        // Should have filter dropdowns
        expect(screen.getByPlaceholderText(/organization/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/period/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/status/i)).toBeInTheDocument();
    });

    it('renders search input', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <ReportsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading reports/i)).not.toBeInTheDocument();
        });

        // Should have search input
        expect(screen.getByPlaceholderText(/search report type/i)).toBeInTheDocument();
    });

    it('renders table with status badges', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <ReportsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading reports/i)).not.toBeInTheDocument();
        });

        // Should have table headers
        expect(screen.getByText('Report')).toBeInTheDocument();
        expect(screen.getByText('Organization')).toBeInTheDocument();
        expect(screen.getByText('Period')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.getByText('Actions')).toBeInTheDocument();
    });
});
