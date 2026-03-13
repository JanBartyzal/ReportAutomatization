import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ErrorBoundary from '../Error/ErrorBoundary';
import React from 'react';

// Simple child component for testing
function TestChild() {
    return <div>Test Child Content</div>;
}

// Error throwing component for testing
function ErrorThrowingComponent(): React.ReactElement {
    throw new Error('Test error');
    return <div>Error</div>;
}

describe('ErrorBoundary', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders children when no error occurs', () => {
        render(
            <ErrorBoundary>
                <TestChild />
            </ErrorBoundary>
        );

        expect(screen.getByText('Test Child Content')).toBeInTheDocument();
    });

    it('renders fallback when error occurs', () => {
        // Spy on console.error
        vi.spyOn(console, 'error').mockImplementation(() => { });

        render(
            <ErrorBoundary>
                <ErrorThrowingComponent />
            </ErrorBoundary>
        );

        // Should show error fallback UI
        expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
    });

    it('shows custom fallback when provided', () => {
        vi.spyOn(console, 'error').mockImplementation(() => { });

        const customFallback = <div>Custom Error Message</div>;

        render(
            <ErrorBoundary fallback={customFallback}>
                <ErrorThrowingComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('Custom Error Message')).toBeInTheDocument();
    });

    it('has try again button', () => {
        vi.spyOn(console, 'error').mockImplementation(() => { });

        render(
            <ErrorBoundary>
                <ErrorThrowingComponent />
            </ErrorBoundary>
        );

        // Should have try again button
        expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    });

    it('has go to dashboard button', () => {
        vi.spyOn(console, 'error').mockImplementation(() => { });

        render(
            <ErrorBoundary>
                <ErrorThrowingComponent />
            </ErrorBoundary>
        );

        // Should have go to dashboard button
        expect(screen.getByRole('button', { name: /go to dashboard/i })).toBeInTheDocument();
    });
});
