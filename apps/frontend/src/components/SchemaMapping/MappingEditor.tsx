import React, { useState, useRef, useCallback } from 'react';
import {
    Card,
    Body1,
    Subtitle2,
    Button,
    Dropdown,
    Option,
    Divider,
    Spinner,
    Text,
} from '@fluentui/react-components';
import { ArrowRightRegular, DeleteRegular, SparkleRegular } from '@fluentui/react-icons';
import ColumnCard from './ColumnCard';
import SuggestionBadge from './SuggestionBadge';
import { MappingSuggestion, FormField, ExcelColumn } from '../../api/templates';

interface MappingEditorProps {
    sourceColumns: ExcelColumn[];
    targetFields: FormField[];
    mappings: MappingSuggestion[];
    onMappingChange: (excelColumn: string, formField: string | null) => void;
    onAutoMap: () => void;
    onClearMappings: () => void;
    isLoading?: boolean;
}

export const MappingEditor: React.FC<MappingEditorProps> = ({
    sourceColumns,
    targetFields,
    mappings,
    onMappingChange,
    onAutoMap,
    onClearMappings,
    isLoading = false,
}) => {
    const [selectedSource, setSelectedSource] = useState<string | null>(null);
    // const [selectedTarget, setSelectedTarget] = useState<string | null>(null);
    const sourcePanelRef = useRef<HTMLDivElement>(null);
    const targetPanelRef = useRef<HTMLDivElement>(null);

    // Get mapped and unmapped columns
    const mappedColumns = mappings.filter(m => m.formField !== null);
    const unmappedColumns = mappings.filter(m => m.formField === null);

    // Handle source column selection
    const handleSourceSelect = useCallback((columnName: string) => {
        setSelectedSource(columnName);
    }, []);

    // Handle target field selection
    const handleTargetSelect = useCallback((fieldName: string | null) => {
        if (selectedSource && fieldName) {
            onMappingChange(selectedSource, fieldName);
            setSelectedSource(null);
        }
    }, [selectedSource, onMappingChange]);

    // Handle dropdown change for a specific mapping
    const handleMappingDropdownChange = useCallback((excelColumn: string, formField: string | undefined) => {
        onMappingChange(excelColumn, formField || null);
    }, [onMappingChange]);

    // Calculate mapping stats
    const totalColumns = mappings.length;
    const mappedCount = mappedColumns.length;
    const mappingProgress = totalColumns > 0 ? (mappedCount / totalColumns) * 100 : 0;

    if (isLoading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '48px' }}>
                <Spinner label="Loading schema mapping..." />
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', height: '100%' }}>
            {/* Toolbar */}
            <Card style={{ padding: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div>
                        <Subtitle2>Column Mapping</Subtitle2>
                        <Body1 style={{ color: 'var(--colorNeutralForeground2)' }}>
                            {mappedCount} of {totalColumns} columns mapped ({Math.round(mappingProgress)}%)
                        </Body1>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                        <Button
                            appearance="secondary"
                            icon={<SparkleRegular />}
                            onClick={onAutoMap}
                        >
                            Auto-Map
                        </Button>
                        <Button
                            appearance="secondary"
                            icon={<DeleteRegular />}
                            onClick={onClearMappings}
                            disabled={mappedCount === 0}
                        >
                            Clear All
                        </Button>
                    </div>
                </div>

                {/* Progress bar */}
                <div style={{
                    marginTop: '12px',
                    height: '8px',
                    backgroundColor: 'var(--colorNeutralBackground3)',
                    borderRadius: '4px',
                    overflow: 'hidden',
                }}>
                    <div style={{
                        height: '100%',
                        width: `${mappingProgress}%`,
                        backgroundColor: mappingProgress === 100 ? 'var(--colorSuccessBackground3)' : 'var(--colorBrandBackground)',
                        transition: 'width 0.3s ease',
                    }} />
                </div>
            </Card>

            {/* Main mapping area */}
            <div style={{ display: 'flex', gap: '16px', flex: 1, minHeight: 0 }}>
                {/* Source columns panel */}
                <Card
                    ref={sourcePanelRef}
                    style={{
                        flex: 1,
                        padding: '12px',
                        overflowY: 'auto',
                        maxHeight: '500px',
                    }}
                >
                    <Subtitle2 style={{ marginBottom: '12px' }}>
                        Source Columns (Excel)
                    </Subtitle2>

                    {unmappedColumns.length > 0 && (
                        <>
                            <Text style={{ color: 'var(--colorNeutralForeground2)', fontSize: '12px' }}>
                                Unmapped
                            </Text>
                            {unmappedColumns.map((mapping) => (
                                <ColumnCard
                                    key={mapping.excelColumn}
                                    name={mapping.excelColumn}
                                    type="source"
                                    isMapped={false}
                                    sampleValue={sourceColumns.find(c => c.name === mapping.excelColumn)?.sampleValues[0]}
                                    isSelected={selectedSource === mapping.excelColumn}
                                    onClick={() => handleSourceSelect(mapping.excelColumn)}
                                />
                            ))}
                            <Divider style={{ margin: '12px 0' }} />
                        </>
                    )}

                    {mappedColumns.length > 0 && (
                        <>
                            <Text style={{ color: 'var(--colorNeutralForeground2)', fontSize: '12px' }}>
                                Mapped
                            </Text>
                            {mappedColumns.map((mapping) => {
                                const sourceCol = sourceColumns.find(c => c.name === mapping.excelColumn);
                                return (
                                    <ColumnCard
                                        key={mapping.excelColumn}
                                        name={mapping.excelColumn}
                                        type="source"
                                        isMapped={true}
                                        sampleValue={sourceCol?.sampleValues[0]}
                                        isSelected={selectedSource === mapping.excelColumn}
                                        onClick={() => handleSourceSelect(mapping.excelColumn)}
                                    />
                                );
                            })}
                        </>
                    )}

                    {mappings.length === 0 && (
                        <Body1 style={{ color: 'var(--colorNeutralForeground2)', textAlign: 'center', padding: '24px' }}>
                            No columns to map. Upload an Excel file first.
                        </Body1>
                    )}
                </Card>

                {/* Connection indicator */}
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '0 8px',
                }}>
                    <ArrowRightRegular style={{ fontSize: '24px', color: 'var(--colorNeutralForeground3)' }} />
                </div>

                {/* Target fields panel */}
                <Card
                    ref={targetPanelRef}
                    style={{
                        flex: 1,
                        padding: '12px',
                        overflowY: 'auto',
                        maxHeight: '500px',
                    }}
                >
                    <Subtitle2 style={{ marginBottom: '12px' }}>
                        Target Fields (Form)
                    </Subtitle2>

                    {targetFields.map((field) => {
                        // Find which source column is mapped to this field
                        const mappedSource = mappings.find(m => m.formField === field.name);

                        return (
                            <ColumnCard
                                key={field.id}
                                name={field.label}
                                type="target"
                                isMapped={!!mappedSource}
                                onClick={() => handleTargetSelect(field.name)}
                            />
                        );
                    })}

                    {targetFields.length === 0 && (
                        <Body1 style={{ color: 'var(--colorNeutralForeground2)', textAlign: 'center', padding: '24px' }}>
                            No form fields available.
                        </Body1>
                    )}
                </Card>
            </div>

            {/* Mapping table view */}
            <Card style={{ padding: '12px', maxHeight: '300px', overflowY: 'auto' }}>
                <Subtitle2 style={{ marginBottom: '12px' }}>
                    Mapping Details
                </Subtitle2>

                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ borderBottom: '1px solid var(--colorNeutralStroke1)' }}>
                            <th style={{ textAlign: 'left', padding: '8px', fontWeight: 600 }}>Excel Column</th>
                            <th style={{ textAlign: 'left', padding: '8px', fontWeight: 600 }}>Confidence</th>
                            <th style={{ textAlign: 'left', padding: '8px', fontWeight: 600 }}>Mapped Form Field</th>
                            <th style={{ textAlign: 'left', padding: '8px', fontWeight: 600 }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {mappings.map((mapping) => (
                            <tr key={mapping.excelColumn} style={{ borderBottom: '1px solid var(--colorNeutralStroke1)' }}>
                                <td style={{ padding: '8px' }}>{mapping.excelColumn}</td>
                                <td style={{ padding: '8px' }}>
                                    <SuggestionBadge confidence={mapping.confidence} />
                                </td>
                                <td style={{ padding: '8px' }}>
                                    <Dropdown
                                        placeholder="Select field..."
                                        value={mapping.formField || ''}
                                        onOptionSelect={(_, data) => handleMappingDropdownChange(mapping.excelColumn, data.optionValue as string)}
                                        style={{ minWidth: '200px' }}
                                    >
                                        <Option value="">-- Unmapped --</Option>
                                        {targetFields.map(field => (
                                            <Option key={field.id} value={field.name}>
                                                {field.label}
                                            </Option>
                                        ))}
                                    </Dropdown>
                                </td>
                                <td style={{ padding: '8px' }}>
                                    {mapping.suggestions.length > 0 && (
                                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                                            {mapping.suggestions.slice(0, 3).map((suggestion) => (
                                                <Button
                                                    key={suggestion}
                                                    size="small"
                                                    appearance="subtle"
                                                    onClick={() => handleMappingDropdownChange(mapping.excelColumn, suggestion)}
                                                >
                                                    {suggestion}
                                                </Button>
                                            ))}
                                        </div>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {mappings.length === 0 && (
                    <Body1 style={{ color: 'var(--colorNeutralForeground2)', textAlign: 'center', padding: '24px' }}>
                        No mappings to display.
                    </Body1>
                )}
            </Card>
        </div>
    );
};

export default MappingEditor;
