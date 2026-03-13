import React from 'react';

interface MappingLineProps {
    sourceY: number;
    targetY: number;
    sourceX: number;
    targetX: number;
    confidence: number;
    isActive?: boolean;
}

export const MappingLine: React.FC<MappingLineProps> = ({
    sourceY,
    targetY,
    sourceX,
    targetX,
    confidence,
    isActive = false,
}) => {
    // Calculate path for a smooth curve
    const midX = (sourceX + targetX) / 2;
    const path = `M ${sourceX} ${sourceY} C ${midX} ${sourceY}, ${midX} ${targetY}, ${targetX} ${targetY}`;

    // Get color based on confidence
    const getStrokeColor = () => {
        if (confidence >= 0.8) return 'var(--colorSuccessStroke)';
        if (confidence >= 0.5) return 'var(--colorWarningStroke)';
        return 'var(--colorDangerStroke)';
    };

    return (
        <g>
            {/* Shadow/background line for visibility */}
            <path
                d={path}
                fill="none"
                stroke="var(--colorNeutralBackground6)"
                strokeWidth="6"
                strokeLinecap="round"
                style={{ opacity: 0.3 }}
            />
            {/* Main line */}
            <path
                d={path}
                fill="none"
                stroke={getStrokeColor()}
                strokeWidth={isActive ? 3 : 2}
                strokeLinecap="round"
                strokeDasharray={confidence < 0.5 ? '5,5' : undefined}
                style={{
                    transition: 'stroke-width 0.2s ease',
                }}
            />
            {/* Arrow head at target */}
            <circle
                cx={targetX}
                cy={targetY}
                r="4"
                fill={getStrokeColor()}
            />
        </g>
    );
};

export default MappingLine;
