
import React, { useState, useMemo } from 'react';
import { useFiles } from '../../api/files';
import { useAggregationPreview, useAggregatedData, SchemaInfo, ColumnMetadata, AggregatedRow } from '../../api/analytics';
import { GraphErrorBoundary } from '../../components/Common/GraphErrorBoundary';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LineChart, Line } from 'recharts';
import { Play, Table, BarChart2, CheckCircle2, AlertCircle, Loader2, ArrowRight } from 'lucide-react';
import { cn } from '../../lib/utils';
import { format } from 'date-fns';

export const Analytics: React.FC = () => {
    // 1. Select Files
    const { data: files } = useFiles();
    const [selectedSchema, setSelectedSchema] = useState<string | null>(null);
    const [schemas, setSchemas] = useState<SchemaInfo[]>([]);

    // 2. Preview Aggregation
    const previewMutation = useAggregationPreview();

    const handleRunAnalysis = () => {
        if (!files || files.length === 0) return;
        const fileIds = files.map(f => f.id);

        previewMutation.mutate(fileIds, {
            onSuccess: (data) => {
                setSchemas(data.schemas);
                if (data.schemas.length > 0) {
                    setSelectedSchema(data.schemas[0].fingerprint);
                }
            }
        });
    };

    // 3. Get Data for selected schema
    const { data: aggregatedData, isLoading: isDataLoading } = useAggregatedData(selectedSchema);

    // Helper: Identify numeric columns for charting
    const numericColumns = useMemo(() => {
        if (!aggregatedData?.columns) return [];
        return aggregatedData.columns.filter(col =>
            // Simple heuristic if type info isn't explicit 'number', check sample values or name
            col.type === 'number' ||
            col.type === 'integer' ||
            col.type === 'float' ||
            ['revenue', 'cost', 'profit', 'sales', 'amount', 'total', 'count'].some(k => col.name.toLowerCase().includes(k))
        );
    }, [aggregatedData]);

    // Helper: Identify a good label column (string)
    const labelColumn = useMemo(() => {
        if (!aggregatedData?.columns) return 'index';
        // Prefer 'date', 'month', 'region', 'category'
        const preferred = aggregatedData.columns.find(col =>
            ['date', 'month', 'year', 'period', 'region', 'category', 'product', 'name'].some(k => col.name.toLowerCase().includes(k))
        );
        return preferred ? preferred.name : aggregatedData.columns.find(c => c.type === 'string')?.name || 'index';
    }, [aggregatedData]);


    return (
        <div className="space-y-8 h-full flex flex-col">
            <div className="flex items-center justify-between flex-shrink-0">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Analytics & Reports</h1>
                    <p className="text-slate-500 mt-1">Aggregate data across multiple presentations and visualize trends.</p>
                </div>
                <div>
                    <button
                        onClick={handleRunAnalysis}
                        disabled={previewMutation.isPending || !files?.length}
                        className={cn(
                            "flex items-center space-x-2 px-6 py-3 bg-blue-600 text-white rounded-lg shadow-lg shadow-blue-900/20 font-medium transition-all hover:bg-blue-700 active:scale-95",
                            (previewMutation.isPending || !files?.length) && "opacity-50 cursor-not-allowed"
                        )}
                    >
                        {previewMutation.isPending ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                            <Play className="w-5 h-5" />
                        )}
                        <span>Run Global Analysis</span>
                    </button>
                    {!files?.length && (
                        <p className="text-xs text-red-500 mt-1 text-right">No files uploaded yet.</p>
                    )}
                </div>
            </div>

            {/* Results Area */}
            {schemas.length > 0 ? (
                <div className="grid grid-cols-12 gap-8 flex-1 min-h-0">

                    {/* Schema Selection Sidebar */}
                    <div className="col-span-3 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col max-h-full">
                        <div className="p-4 border-b border-slate-100 bg-slate-50">
                            <h2 className="font-semibold text-slate-900">Detected Schemas</h2>
                            <p className="text-xs text-slate-500 mt-1">{schemas.length} patterns found</p>
                        </div>
                        <div className="overflow-y-auto flex-1 p-2 space-y-2">
                            {schemas.map((schema, idx) => (
                                <button
                                    key={schema.fingerprint}
                                    onClick={() => setSelectedSchema(schema.fingerprint)}
                                    className={cn(
                                        "w-full text-left p-3 rounded-lg border transition-all duration-200 group relative",
                                        selectedSchema === schema.fingerprint
                                            ? "bg-blue-50 border-blue-200 shadow-sm"
                                            : "bg-white border-transparent hover:bg-slate-50 hover:border-slate-200"
                                    )}
                                >
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="flex items-center space-x-2">
                                            <div className={cn(
                                                "w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold",
                                                selectedSchema === schema.fingerprint ? "bg-blue-100 text-blue-600" : "bg-slate-100 text-slate-500"
                                            )}>
                                                {idx + 1}
                                            </div>
                                            <span className="text-sm font-semibold text-slate-900">
                                                Table Type {String.fromCharCode(65 + idx)}
                                            </span>
                                        </div>
                                        {schema.confidence_score > 90 && (
                                            <CheckCircle2 className="w-4 h-4 text-green-500" />
                                        )}
                                    </div>

                                    <div className="space-y-1">
                                        <div className="text-xs text-slate-500 flex justify-between">
                                            <span>Columns:</span>
                                            <span className="font-medium text-slate-700">{schema.column_count}</span>
                                        </div>
                                        <div className="text-xs text-slate-500 flex justify-between">
                                            <span>Matching Files:</span>
                                            <span className="font-medium text-slate-700">{schema.matching_files}</span>
                                        </div>
                                        <div className="text-xs text-slate-500 flex justify-between">
                                            <span>Total Rows:</span>
                                            <span className="font-medium text-slate-700">{schema.total_rows}</span>
                                        </div>
                                    </div>

                                    {selectedSchema === schema.fingerprint && (
                                        <div className="absolute right-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-blue-500 rounded-l-full" />
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Main Content Area (Charts + Table) */}
                    <div className="col-span-9 flex flex-col space-y-6 overflow-hidden max-h-full">

                        {isDataLoading ? (
                            <div className="flex-1 flex items-center justify-center bg-white rounded-xl border border-slate-200">
                                <div className="text-center space-y-4">
                                    <Loader2 className="w-10 h-10 text-blue-500 animate-spin mx-auto" />
                                    <p className="text-slate-500 font-medium">Aggregating data...</p>
                                </div>
                            </div>
                        ) : aggregatedData ? (
                            <>
                                {/* 1. Visualization Section */}
                                <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 flex-shrink-0">
                                    <div className="flex items-center justify-between mb-6">
                                        <h2 className="font-bold text-slate-900 flex items-center space-x-2">
                                            <BarChart2 className="w-5 h-5 text-blue-500" />
                                            <span>Data Visualization</span>
                                        </h2>
                                        {/* Simple visualization logic: If numeric columns found, show chart */}
                                        {numericColumns.length > 0 && <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full font-medium">Auto-Generated Chart</span>}
                                    </div>

                                    <div className="h-[300px] w-full">
                                        <GraphErrorBoundary>
                                            {numericColumns.length > 0 ? (
                                                <ResponsiveContainer width="100%" height="100%">
                                                    <BarChart data={aggregatedData.rows}>
                                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                                                        <XAxis
                                                            dataKey={labelColumn}
                                                            tick={{ fontSize: 12, fill: '#64748B' }}
                                                            axisLine={{ stroke: '#E2E8F0' }}
                                                            tickLine={false}
                                                        />
                                                        <YAxis
                                                            tick={{ fontSize: 12, fill: '#64748B' }}
                                                            axisLine={{ stroke: '#E2E8F0' }}
                                                            tickLine={false}
                                                        />
                                                        <Tooltip
                                                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                                        />
                                                        <Legend />
                                                        {numericColumns.slice(0, 3).map((col, i) => (
                                                            <Bar
                                                                key={col.name}
                                                                dataKey={col.name}
                                                                fill={['#3B82F6', '#8B5CF6', '#10B981'][i % 3]}
                                                                radius={[4, 4, 0, 0]}
                                                            />
                                                        ))}
                                                    </BarChart>
                                                </ResponsiveContainer>
                                            ) : (
                                                <div className="h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50 rounded-lg border border-dashed border-slate-200">
                                                    <AlertCircle className="w-8 h-8 mb-2" />
                                                    <p>No numeric data detected for automatic visualization.</p>
                                                </div>
                                            )}
                                        </GraphErrorBoundary>
                                    </div>
                                </div>

                                {/* 2. Data Table Section */}
                                <div className="bg-white rounded-xl border border-slate-200 shadow-sm flex-1 overflow-hidden flex flex-col min-h-[300px]">
                                    <div className="p-4 border-b border-slate-100 flex items-center space-x-2 bg-slate-50/50">
                                        <Table className="w-5 h-5 text-slate-500" />
                                        <h2 className="font-semibold text-slate-900">Aggregated Data Source</h2>
                                        <span className="text-xs text-slate-400 px-2 py-0.5 bg-slate-100 rounded-full border border-slate-200">
                                            {aggregatedData.row_count} rows
                                        </span>
                                    </div>
                                    <div className="overflow-auto flex-1">
                                        <table className="w-full text-sm text-left">
                                            <thead className="text-xs text-slate-500 uppercase bg-slate-50 border-b border-slate-100 sticky top-0 z-10">
                                                <tr>
                                                    {aggregatedData.columns.map((col) => (
                                                        <th key={col.name} className="px-6 py-3 font-medium whitespace-nowrap bg-slate-50">
                                                            {col.name}
                                                        </th>
                                                    ))}
                                                    <th className="px-6 py-3 font-medium bg-slate-50">Source File</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-100">
                                                {aggregatedData.rows.map((row, i) => (
                                                    <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                                                        {aggregatedData.columns.map((col) => (
                                                            <td key={col.name} className="px-6 py-3 whitespace-nowrap text-slate-600">
                                                                {typeof row[col.name] === 'object' ? JSON.stringify(row[col.name]) : String(row[col.name])}
                                                            </td>
                                                        ))}
                                                        <td className="px-6 py-3 whitespace-nowrap text-slate-400 text-xs italic">
                                                            {row._source_filename || 'Unknown'}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </>
                        ) : null}
                    </div>

                </div>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-center space-y-6 opacity-60">
                    <div className="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center">
                        <Play className="w-10 h-10 text-slate-300 ml-1" />
                    </div>
                    <div className="max-w-md space-y-2">
                        <h2 className="text-xl font-bold text-slate-900">Ready to Analyze</h2>
                        <p className="text-slate-500">
                            Click "Run Global Analysis" to detect common table structures across your uploaded files and generate aggregated reports.
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};
