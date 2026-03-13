import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import GenerationProgress from '../Generation/GenerationProgress';


describe('GenerationProgress', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders without crashing', () => {
        render(<GenerationProgress progress={50} status="PROCESSING" />);
        // Basic smoke test - component should render without errors
        expect(document.body).toBeInTheDocument();
    });

    it('renders progress bar with correct value', () => {
        render(<GenerationProgress progress={75} status="PROCESSING" />);

        // Check if progress value is displayed
        expect(screen.getByText('75%')).toBeInTheDocument();
    });

    it('renders pending status correctly', () => {
        render(<GenerationProgress progress={0} status="PENDING" />);

        // Should show pending status
        expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    it('renders processing status correctly', () => {
        render(<GenerationProgress progress={50} status="PROCESSING" />);

        // Should show processing status
        expect(screen.getByText(/processing/i)).toBeInTheDocument();
    });

    it('renders completed status correctly', () => {
        render(<GenerationProgress progress={100} status="COMPLETED" />);

        // Should show completed status
        expect(screen.getByText(/completed/i)).toBeInTheDocument();
    });

    it('renders failed status correctly', () => {
        render(<GenerationProgress progress={30} status="FAILED" />);

        // Should show failed status
        expect(screen.getByText(/failed/i)).toBeInTheDocument();
    });
});
