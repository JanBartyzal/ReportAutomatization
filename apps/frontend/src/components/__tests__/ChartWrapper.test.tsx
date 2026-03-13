import { describe, it, expect } from 'vitest';
import * as matchers from '@testing-library/jest-dom/matchers';
import { render, screen } from '@testing-library/react';
import { ChartWrapper } from '../Charts/ChartWrapper';
import { LineChart, Line } from 'recharts';


expect.extend(matchers);

describe('ChartWrapper', () => {
    const mockData = [
        { name: 'Jan', value: 100 },
        { name: 'Feb', value: 200 },
        { name: 'Mar', value: 150 },
    ];

    it('renders children correctly', () => {
        render(
            <ChartWrapper>
                <LineChart data={mockData}>
                    <Line type="monotone" dataKey="value" />
                </LineChart>
            </ChartWrapper>
        );

        // Component should render without errors
        expect(document.querySelector('.recharts-responsive-container')).toBeInTheDocument();
    });

    it('renders with custom height', () => {
        render(
            <ChartWrapper height={400}>
                <LineChart data={mockData}>
                    <Line type="monotone" dataKey="value" />
                </LineChart>
            </ChartWrapper>
        );

        const container = screen.getByTestId('chart-wrapper');
        expect(container).toBeInTheDocument();
    });

    it('renders legend when showLegend is true', () => {
        render(
            <ChartWrapper showLegend legendContent={<div>Test Legend</div>}>
                <LineChart data={mockData}>
                    <Line type="monotone" dataKey="value" />
                </LineChart>
            </ChartWrapper>
        );

        expect(screen.getByText('Test Legend')).toBeInTheDocument();
    });

    it('handles empty data', () => {
        render(
            <ChartWrapper>
                <LineChart data={[]}>
                    <Line type="monotone" dataKey="value" />
                </LineChart>
            </ChartWrapper>
        );

        // Should render without errors
        expect(document.querySelector('.recharts-responsive-container')).toBeInTheDocument();
    });
});
