import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PeriodsPage } from '../PeriodsPage';

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

describe('PeriodsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <PeriodsPage />
            </TestWrapper>
        );

        // Should show loading spinner initially
        expect(screen.getByText(/loading periods/i)).toBeInTheDocument();
    });

    it('renders periods page title', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <PeriodsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading periods/i)).not.toBeInTheDocument();
        });

        // Should show the page title
        expect(screen.getByText('Reporting Periods')).toBeInTheDocument();
    });

    it('renders create period button', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <PeriodsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading periods/i)).not.toBeInTheDocument();
        });

        // Should have create period button
        expect(screen.getByRole('button', { name: /create period/i })).toBeInTheDocument();
    });

    it('renders filter dropdowns', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <PeriodsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading periods/i)).not.toBeInTheDocument();
        });

        // Should have filter dropdowns
        expect(screen.getByPlaceholderText(/filter by type/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/filter by status/i)).toBeInTheDocument();
    });

    it('renders table with period data', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <PeriodsPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading periods/i)).not.toBeInTheDocument();
        });

        // Should have table headers
        expect(screen.getByText('Period')).toBeInTheDocument();
        expect(screen.getByText('Type')).toBeInTheDocument();
        expect(screen.getByText('Dates')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.getByText('Actions')).toBeInTheDocument();
    });
});
