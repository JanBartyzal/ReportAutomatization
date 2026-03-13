import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { GenerateButton } from '../Generation/GenerateButton';


describe('GenerateButton', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders without crashing', () => {
        render(<GenerateButton reportId="test-report" reportStatus="APPROVED" />);
        // Basic smoke test - component should render without errors
        expect(document.body).toBeInTheDocument();
    });
});
