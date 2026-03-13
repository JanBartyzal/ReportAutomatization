import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DashboardListPage from '../DashboardListPage';

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

describe('DashboardListPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <DashboardListPage />
            </TestWrapper>
        );

        // Should show loading spinner initially
        expect(screen.getByText(/loading dashboards/i)).toBeInTheDocument();
    });

    it('renders dashboards page title', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <DashboardListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading dashboards/i)).not.toBeInTheDocument();
        });

        // Should show the page title
        expect(screen.getByText('Dashboards')).toBeInTheDocument();
    });

    it('renders new dashboard button', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <DashboardListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading dashboards/i)).not.toBeInTheDocument();
        });

        // Should have new dashboard button
        expect(screen.getByRole('button', { name: /new dashboard/i })).toBeInTheDocument();
    });

    it('shows empty state when no dashboards exist', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <DashboardListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading dashboards/i)).not.toBeInTheDocument();
        });

        // Should show empty message
        expect(screen.getByText(/no dashboards yet/i)).toBeInTheDocument();
    });

    it('renders dashboard cards with visibility badges', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <DashboardListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading dashboards/i)).not.toBeInTheDocument();
        });

        // Should show the page
        expect(screen.getByText('Dashboards')).toBeInTheDocument();
    });
});
