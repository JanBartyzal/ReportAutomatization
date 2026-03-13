import React from 'react';
import { Card, Text, Badge } from '@fluentui/react-components';
import { ReOrderRegular } from '@fluentui/react-icons';

export type ColumnCardType = 'source' | 'target';

interface ColumnCardProps {
    name: string;
    type: ColumnCardType;
    isMapped?: boolean;
    sampleValue?: string;
    onClick?: () => void;
    isSelected?: boolean;
    onDragStart?: (e: React.DragEvent) => void;
    onDragEnd?: (e: React.DragEvent) => void;
}

export const ColumnCard: React.FC<ColumnCardProps> = ({
    name,
    type,
    isMapped = false,
    sampleValue,
    onClick,
    isSelected = false,
    onDragStart,
    onDragEnd,
}) => {
    const getTypeBadge = () => {
        if (type === 'source') {
            return <Badge appearance="filled" color="informative">Excel</Badge>;
        }
        return <Badge appearance="filled" color="success">Form</Badge>;
    };

    const getMappedBadge = () => {
        if (isMapped && type === 'source') {
            return <Badge appearance="filled" color="success">Mapped</Badge>;
        }
        if (!isMapped && type === 'source') {
            return <Badge appearance="filled" color="warning">Unmapped</Badge>;
        }
        return null;
    };

    return (
        <Card
            onClick={onClick}
            draggable={!!onDragStart}
            onDragStart={onDragStart}
            onDragEnd={onDragEnd}
            style={{
                padding: '12px',
                marginBottom: '8px',
                cursor: onClick ? 'pointer' : 'grab',
                border: isSelected ? '2px solid var(--colorBrandStroke)' : '1px solid var(--colorNeutralStroke1)',
                backgroundColor: isSelected ? 'var(--colorNeutralBackground4)' : 'var(--colorNeutralBackground1)',
                transition: 'all 0.2s ease',
            }}
            onMouseEnter={(e) => {
                if (!isSelected) {
                    e.currentTarget.style.borderColor = 'var(--colorBrandStroke)';
                }
            }}
            onMouseLeave={(e) => {
                if (!isSelected) {
                    e.currentTarget.style.borderColor = 'var(--colorNeutralStroke1)';
                }
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ReOrderRegular
                    style={{
                        color: 'var(--colorNeutralForeground3)',
                        cursor: 'grab',
                        fontSize: '16px',
                    }}
                />
                <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                        <Text weight="semibold" size={300}>{name}</Text>
                        {getTypeBadge()}
                        {getMappedBadge()}
                    </div>
                    {sampleValue && type === 'source' && (
                        <Text size={200} style={{ color: 'var(--colorNeutralForeground2)' }}>
                            Sample: {sampleValue}
                        </Text>
                    )}
                </div>
            </div>
        </Card>
    );
};

export default ColumnCard;
