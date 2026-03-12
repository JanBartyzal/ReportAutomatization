import React, { useState } from 'react';
import {
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridHeaderCell,
    DataGridBody,
    DataGridCell,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
} from '@fluentui/react-components';
import {
    Button,
    Text,
    Subtitle2,
    Badge,
    Spinner,
    Input,
    InputProps,
    Dropdown,
    Option,
    SelectionEvents,
    OptionOnSelectData,
} from '@fluentui/react-components';
import {
    Save24Regular,
} from '@fluentui/react-icons';
import { Placeholder, PlaceholderMappingItem } from '../../api/templates';
import { reportBrand } from '../../theme/brandTokens';

interface PlaceholderMapperProps {
    templateId: string;
    placeholders: Placeholder[];
    initialMappings?: PlaceholderMappingItem[];
    onSave: (mappings: PlaceholderMappingItem[]) => Promise<void>;
    isSaving?: boolean;
}

export const PlaceholderMapper: React.FC<PlaceholderMapperProps> = ({
    placeholders,
    initialMappings = [],
    onSave,
    isSaving = false,
}) => {
    const [mappings, setMappings] = useState<PlaceholderMappingItem[]>(
        placeholders.map((p) => {
            const existing = initialMappings.find((m) => m.placeholder === p.name);
            return existing || { placeholder: p.name, source: 'form_field' as const };
        })
    );

    const handleSourceChange = (placeholderName: string, source: string) => {
        setMappings((prev) =>
            prev.map((m) =>
                m.placeholder === placeholderName
                    ? { ...m, source: source as any, field: '', tableName: '', calculation: '' }
                    : m
            )
        );
    };

    const handleFieldChange = (placeholderName: string, field: string) => {
        setMappings((prev) =>
            prev.map((m) =>
                m.placeholder === placeholderName ? { ...m, field } : m
            )
        );
    };

    const handleSave = () => {
        onSave(mappings);
    };

    const columns: TableColumnDefinition<PlaceholderMappingItem>[] = [
        createTableColumn<PlaceholderMappingItem>({
            columnId: 'placeholder',
            renderHeaderCell: () => 'Placeholder',
            renderCell: (item) => (
                <TableCellLayout>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Text weight="semibold">{item.placeholder}</Text>
                        <Badge appearance="outline" size="small">
                            {placeholders.find(p => p.name === item.placeholder)?.type}
                        </Badge>
                    </div>
                </TableCellLayout>
            ),
        }),
        createTableColumn<PlaceholderMappingItem>({
            columnId: 'source',
            renderHeaderCell: () => 'Data Source',
            renderCell: (item) => (
                <TableCellLayout>
                    <Dropdown
                        value={item.source}
                        selectedOptions={[item.source]}
                        onOptionSelect={(_ev: SelectionEvents, data: OptionOnSelectData) => 
                            handleSourceChange(item.placeholder, data.optionValue || 'form_field')
                        }
                        size="small"
                        style={{ minWidth: '150px' }}
                    >
                        <Option value="form_field">Form Field</Option>
                        <Option value="table">Table Data</Option>
                        <Option value="aggregated">Aggregated Metric</Option>
                        <Option value="time_series">Time Series (Chart)</Option>
                    </Dropdown>
                </TableCellLayout>
            ),
        }),
        createTableColumn<PlaceholderMappingItem>({
            columnId: 'mapping',
            renderHeaderCell: () => 'Mapping / Field ID',
            renderCell: (item) => (
                <TableCellLayout>
                    <Input
                        value={item.field || ''}
                        onChange={(_ev: React.ChangeEvent<HTMLInputElement>, data: InputProps) => 
                            handleFieldChange(item.placeholder, data.value || '')
                        }
                        placeholder="Enter field/metric key"
                        size="small"
                        style={{ width: '100%' }}
                    />
                </TableCellLayout>
            ),
        }),
    ];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Subtitle2>Map placeholders to system data sources</Subtitle2>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <Button
                        appearance="primary"
                        icon={isSaving ? <Spinner size="tiny" /> : <Save24Regular />}
                        onClick={handleSave}
                        disabled={isSaving}
                        style={{ backgroundColor: reportBrand[90] }}
                    >
                        Save Mapping
                    </Button>
                </div>
            </div>

            <DataGrid
                items={mappings}
                columns={columns}
                resizableColumns
                style={{ minWidth: '100%' }}
            >
                <DataGridHeader>
                    <DataGridRow>
                        {({ renderHeaderCell }: any) => (
                            <DataGridHeaderCell>{renderHeaderCell()}</DataGridHeaderCell>
                        )}
                    </DataGridRow>
                </DataGridHeader>
                <DataGridBody<PlaceholderMappingItem>>
                    {({ item, rowId }: any) => (
                        <DataGridRow<PlaceholderMappingItem> key={rowId}>
                            {({ renderCell }: any) => (
                                <DataGridCell>{renderCell(item)}</DataGridCell>
                            )}
                        </DataGridRow>
                    )}
                </DataGridBody>
            </DataGrid>
        </div>
    );
};

export default PlaceholderMapper;
