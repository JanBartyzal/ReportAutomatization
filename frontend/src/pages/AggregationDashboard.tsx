import React, { useState } from 'react';
import { useFiles, UploadedFile } from '../api/files';
import { useAggregationPreview, useAggregatedData, SchemaInfo } from '../api/analytics';

export const AggregationDashboard: React.FC = () => {
    const { data: files, isLoading: isLoadingFiles, isError: isFilesError } = useFiles();
    const [selectedFileIds, setSelectedFileIds] = useState<number[]>([]);

    // Preview Mutation
    const {
        mutate: preview,
        data: previewData,
        isPending: isPreviewLoading,
        isError: isPreviewError
    } = useAggregationPreview();

    // Data Query
    const [selectedFingerprint, setSelectedFingerprint] = useState<string | null>(null);
    const {
        data: aggregatedData,
        isLoading: isDataLoading,
        isError: isDataError
    } = useAggregatedData(selectedFingerprint);

    const handleFileToggle = (fileId: number) => {
        setSelectedFileIds(prev =>
            prev.includes(fileId)
                ? prev.filter(id => id !== fileId)
                : [...prev, fileId]
        );
    };

    const handleAnalyze = () => {
        if (selectedFileIds.length > 0) {
            preview(selectedFileIds);
            setSelectedFingerprint(null); // Reset detail view
        }
    };

    return (
        <div className="p-6 space-y-6">
            <header>
                <h1 className="text-2xl font-bold text-gray-800">Aggregation Dashboard</h1>
                <p className="text-gray-600">Select files to analyze common structures and aggregate data.</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Left Panel: File Selection */}
                <div className="bg-white p-4 rounded-lg shadow border border-gray-200 md:col-span-1">
                    <h2 className="text-lg font-semibold mb-4">Select Files</h2>

                    {isLoadingFiles && <p>Loading files...</p>}
                    {isFilesError && <p className="text-red-500">Error loading files.</p>}

                    <div className="space-y-2 max-h-[60vh] overflow-y-auto">
                        {files?.map((file: UploadedFile) => (
                            <div key={file.id} className="flex items-center space-x-2 p-2 hover:bg-gray-50 rounded">
                                <input
                                    type="checkbox"
                                    id={`file-${file.id}`}
                                    checked={selectedFileIds.includes(file.id)}
                                    onChange={() => handleFileToggle(file.id)}
                                    className="rounded text-blue-600 focus:ring-blue-500"
                                />
                                <label htmlFor={`file-${file.id}`} className="text-sm text-gray-700 truncate cursor-pointer flex-1">
                                    {file.filename}
                                </label>
                            </div>
                        ))}
                        {files && files.length === 0 && (
                            <p className="text-gray-500 italic text-sm">No files uploaded yet.</p>
                        )}
                    </div>

                    <div className="mt-4 pt-4 border-t border-gray-100">
                        <button
                            onClick={handleAnalyze}
                            disabled={selectedFileIds.length < 2 || isPreviewLoading}
                            className={`w-full py-2 px-4 rounded font-medium text-white transition-colors
                                ${selectedFileIds.length < 2 || isPreviewLoading
                                    ? 'bg-gray-400 cursor-not-allowed'
                                    : 'bg-blue-600 hover:bg-blue-700'
                                }`}
                        >
                            {isPreviewLoading ? 'Analyzing...' : 'Analyze Selected'}
                        </button>
                        {selectedFileIds.length < 2 && (
                            <p className="text-xs text-gray-500 mt-1 text-center">Select at least 2 files</p>
                        )}
                    </div>
                </div>

                {/* Right Panel: Results & Preview */}
                <div className="md:col-span-2 space-y-6">

                    {/* Schema Preview List */}
                    {previewData && (
                        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
                            <h2 className="text-lg font-semibold mb-4">Detected Common Schemas</h2>
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Columns</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Matching Files</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Rows</th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                                        </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                        {previewData.schemas.map((schema: SchemaInfo) => (
                                            <tr key={schema.fingerprint} className={selectedFingerprint === schema.fingerprint ? "bg-blue-50" : ""}>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                                    {schema.column_count} cols
                                                    <div className="text-xs text-gray-500 truncate max-w-[200px]" title={schema.columns.join(', ')}>
                                                        {schema.columns.join(', ')}
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {schema.matching_files}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {schema.total_rows}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                                    <button
                                                        onClick={() => setSelectedFingerprint(schema.fingerprint)}
                                                        className="text-blue-600 hover:text-blue-900"
                                                    >
                                                        View Data
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}

                    {/* Aggregated Data View */}
                    {selectedFingerprint && (
                        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
                            <h2 className="text-lg font-semibold mb-4">Aggregated Data</h2>

                            {isDataLoading && <div className="text-center py-8">Loading data...</div>}
                            {isDataError && <div className="text-red-500 text-center py-8">Error loading aggregated data.</div>}

                            {aggregatedData && (
                                <div className="overflow-x-auto max-h-[600px]">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50 sticky top-0">
                                            <tr>
                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border-b">
                                                    Source File
                                                </th>
                                                {aggregatedData.columns.map(col => (
                                                    <th key={col.name} className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border-b">
                                                        {col.name}
                                                    </th>
                                                ))}
                                            </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                            {aggregatedData.rows.map((row, idx) => (
                                                <tr key={idx} className="hover:bg-gray-50">
                                                    <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-500 italic">
                                                        {row['_source_file']}
                                                    </td>
                                                    {aggregatedData.columns.map(col => (
                                                        <td key={`${idx}-${col.name}`} className="px-4 py-2 whitespace-nowrap text-sm text-gray-900">
                                                            {String(row[col.name] ?? '')}
                                                        </td>
                                                    ))}
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
