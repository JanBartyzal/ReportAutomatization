
import React from 'react';
import { format } from 'date-fns';
import { useFiles } from '../api/files';
import { FileUploader } from '../components/Upload/FileUploader';
import { FileText, Clock, Hash, MapPin, Loader2 } from 'lucide-react';

export const Dashboard: React.FC = () => {
    const { data: files, isLoading, isError } = useFiles();

    return (
        <div className="space-y-8">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Dashboard</h1>
                    <p className="text-slate-500 mt-1">Manage your presentations and view extraction status.</p>
                </div>
            </div>

            {/* Quick Stats Row */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex items-center space-x-4">
                    <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600">
                        <FileText className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-2xl font-bold text-slate-900">{files?.length || 0}</div>
                        <div className="text-sm text-slate-500">Total Files Uploaded</div>
                    </div>
                </div>
                {/* Placeholders for other stats */}
                <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex items-center space-x-4 opacity-60">
                    <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center text-green-600">
                        <Clock className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-2xl font-bold text-slate-900">-</div>
                        <div className="text-sm text-slate-500">Avg. Processing Time</div>
                    </div>
                </div>
                <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex items-center space-x-4 opacity-60">
                    <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center text-purple-600">
                        <MapPin className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-2xl font-bold text-slate-900">-</div>
                        <div className="text-sm text-slate-500">Regions Covered</div>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Information / Upload Column */}
                <div className="lg:col-span-1 space-y-6">
                    <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
                        <h2 className="text-lg font-semibold text-slate-900 mb-4">Upload New File</h2>
                        <FileUploader />
                    </div>

                    <div className="bg-blue-50 border border-blue-100 rounded-xl p-6">
                        <h3 className="text-sm font-semibold text-blue-900 mb-2">Processing Info</h3>
                        <p className="text-sm text-blue-700 leading-relaxed">
                            Uploaded files are automatically queued for OCR and structure extraction.
                            Large files (&gt;20MB) may take up to 2 minutes to process.
                        </p>
                    </div>
                </div>

                {/* Recent Files List */}
                <div className="lg:col-span-2">
                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                            <h2 className="text-lg font-semibold text-slate-900">Recent Uploads</h2>
                        </div>

                        {isLoading ? (
                            <div className="p-12 flex justify-center">
                                <Loader2 className="w-8 h-8 text-blue-500 animate-spin" />
                            </div>
                        ) : isError ? (
                            <div className="p-12 text-center text-red-500">
                                Failed to load files. Please try refreshing.
                            </div>
                        ) : files && files.length > 0 ? (
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm text-left">
                                    <thead className="text-xs text-slate-500 uppercase bg-slate-50 border-b border-slate-100">
                                        <tr>
                                            <th className="px-6 py-3 font-medium">Filename</th>
                                            <th className="px-6 py-3 font-medium">Region</th>
                                            <th className="px-6 py-3 font-medium">Date</th>
                                            <th className="px-6 py-3 font-medium">MD5 Hash</th>
                                            <th className="px-6 py-3 font-medium text-right">Action</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {files.map((file) => (
                                            <tr key={file.id} className="hover:bg-slate-50/80 transition-colors group">
                                                <td className="px-6 py-4 font-medium text-slate-900 flex items-center space-x-2">
                                                    <FileText className="w-4 h-4 text-slate-400" />
                                                    <span className="truncate max-w-[200px]" title={file.filename}>{file.filename}</span>
                                                </td>
                                                <td className="px-6 py-4 text-slate-600">
                                                    {file.region || <span className="text-slate-300 italic">N/A</span>}
                                                </td>
                                                <td className="px-6 py-4 text-slate-600 whitespace-nowrap">
                                                    {file.created_at ? format(new Date(file.created_at), 'MMM d, yyyy HH:mm') : '-'}
                                                </td>
                                                <td className="px-6 py-4 text-slate-400 font-mono text-xs">
                                                    <span className="flex items-center space-x-1">
                                                        <Hash className="w-3 h-3" />
                                                        <span>{file.md5hash.substring(0, 8)}...</span>
                                                    </span>
                                                </td>
                                                <td className="px-6 py-4 text-right">
                                                    <button className="text-blue-600 hover:text-blue-800 font-medium text-xs opacity-0 group-hover:opacity-100 transition-opacity">
                                                        View Report
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <div className="p-12 text-center text-slate-500">
                                <FileText className="w-12 h-12 text-slate-200 mx-auto mb-3" />
                                <p>No files uploaded yet.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;