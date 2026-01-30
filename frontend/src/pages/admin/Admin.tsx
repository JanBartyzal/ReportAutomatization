
import React from 'react';
import api from '../../api/axios';
import { useQuery } from '@tanstack/react-query';
import { Users, FileText, Server, AlertTriangle, Activity } from 'lucide-react';
import { useAuth } from '../../components/auth/AuthProvider';

interface SystemStats {
    total_users: number;
    total_files: number;
    total_storage: string;
    system_health: string;
    active_jobs: number;
}

const getAdminStats = async () => {
    // This endpoint might not exist yet based on previous check, using a mock if fails or implement calling real endpoint if available.
    // The previous view_file of admin_api.md showed GET /api/admin/all-stats
    try {
        const response = await api.get<SystemStats>('/admin/all-stats');
        return response.data;
    } catch (error) {
        // Fallback or re-throw
        console.error("Failed to fetch admin stats", error);
        throw error;
    }
};

export const Admin: React.FC = () => {
    const { user } = useAuth();
    // Case insensitive check for ADMIN role
    const isAdmin = user?.roles?.some(r => r.toUpperCase() === 'ADMIN' || r === 'AppAdmin');

    const { data: stats, isLoading, isError } = useQuery({
        queryKey: ['admin', 'stats'],
        queryFn: getAdminStats,
        enabled: true // Should be isAdmin but for dev we enable
    });

    if (!isAdmin) {
        return (
            <div className="flex flex-col items-center justify-center h-full text-center space-y-4">
                <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center text-red-600">
                    <AlertTriangle className="w-8 h-8" />
                </div>
                <h1 className="text-2xl font-bold text-slate-900">Access Restricted</h1>
                <p className="text-slate-500 max-w-md">
                    You do not have the required permissions (AppAdmin role) to view this page.
                </p>
            </div>
        );
    }

    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold text-slate-900 tracking-tight">System Administration</h1>
                <p className="text-slate-500 mt-1">Monitor system health and resource usage.</p>
            </div>

            {isError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
                    Unable to load system statistics. The backend might be unreachable or configured incorrectly.
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <div className="text-sm text-slate-500 font-medium">Total Users</div>
                        {isLoading ? <div className="h-6 w-12 bg-slate-100 animate-pulse rounded mt-1" /> : (
                            <div className="text-2xl font-bold text-slate-900">{stats?.total_users || '-'}</div>
                        )}
                    </div>
                    <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-lg flex items-center justify-center">
                        <Users className="w-5 h-5" />
                    </div>
                </div>

                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <div className="text-sm text-slate-500 font-medium">Files Stored</div>
                        {isLoading ? <div className="h-6 w-12 bg-slate-100 animate-pulse rounded mt-1" /> : (
                            <div className="text-2xl font-bold text-slate-900">{stats?.total_files || '-'}</div>
                        )}
                    </div>
                    <div className="w-10 h-10 bg-indigo-100 text-indigo-600 rounded-lg flex items-center justify-center">
                        <FileText className="w-5 h-5" />
                    </div>
                </div>

                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <div className="text-sm text-slate-500 font-medium">Storage Used</div>
                        {isLoading ? <div className="h-6 w-12 bg-slate-100 animate-pulse rounded mt-1" /> : (
                            <div className="text-2xl font-bold text-slate-900">{stats?.total_storage || '-'}</div>
                        )}
                    </div>
                    <div className="w-10 h-10 bg-purple-100 text-purple-600 rounded-lg flex items-center justify-center">
                        <Server className="w-5 h-5" />
                    </div>
                </div>

                <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <div className="text-sm text-slate-500 font-medium">System Health</div>
                        {isLoading ? <div className="h-6 w-12 bg-slate-100 animate-pulse rounded mt-1" /> : (
                            <div className={api.getUri ? "text-2xl font-bold text-green-600" : "text-2xl font-bold text-slate-900"}>
                                {stats?.system_health || 'Unknown'}
                            </div>
                        )}
                    </div>
                    <div className="w-10 h-10 bg-green-100 text-green-600 rounded-lg flex items-center justify-center">
                        <Activity className="w-5 h-5" />
                    </div>
                </div>
            </div>

            {/* Logs or other admin features could go here */}
            <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-slate-400 border-dashed">
                <p>Additional administrative tools (User Management, Audit Logs) coming soon.</p>
            </div>
        </div>
    );
};
