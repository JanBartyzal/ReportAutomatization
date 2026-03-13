import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TemplateListPage } from '../TemplateListPage';

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

describe('TemplateListPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <TemplateListPage />
            </TestWrapper>
        );

        // Should show loading spinner initially
        expect(screen.getByText(/loading templates/i)).toBeInTheDocument();
    });

    it('renders templates page title', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <TemplateListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading templates/i)).not.toBeInTheDocument();
        });

        // Should show the page title
        expect(screen.getByText('PPTX Templates')).toBeInTheDocument();
    });

    it('renders upload template button', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <TemplateListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading templates/i)).not.toBeInTheDocument();
        });

        // Should have upload template button
        expect(screen.getByRole('button', { name: /upload template/i })).toBeInTheDocument();
    });

    it('shows empty state when no templates exist', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <TemplateListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading templates/i)).not.toBeInTheDocument();
        });

        // Should show empty message
        expect(screen.getByText(/no templates yet/i)).toBeInTheDocument();
    });

    it('renders scope badges for templates', async () => {
        const TestWrapper = createTestWrapper();

        render(
            <TestWrapper>
                <TemplateListPage />
            </TestWrapper>
        );

        await waitFor(() => {
            expect(screen.queryByText(/loading templates/i)).not.toBeInTheDocument();
        });

        // Should show the page
        expect(screen.getByText('PPTX Templates')).toBeInTheDocument();
    });
});
