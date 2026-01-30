
import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, FileText, Settings, Database, Server, Table } from 'lucide-react';
import { cn } from '../../lib/utils';

const navItems = [
    { icon: LayoutDashboard, label: 'Dashboard', to: '/' },
    { icon: Table, label: 'Aggregation', to: '/aggregation' },
    { icon: LayoutDashboard, label: 'Opex', to: '/opex/dashboard' },
    { icon: Database, label: 'Import Opex', to: '/import/opex' },
    { icon: Server, label: 'Admin', to: '/admin', adminOnly: true },
];

export const Sidebar: React.FC = () => {
    return (
        <aside className="w-64 bg-slate-900 text-white flex flex-col h-full border-r border-slate-800 shadow-xl">
            <div className="p-6 border-b border-slate-800 flex items-center space-x-3">
                <div className="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center shadow-lg shadow-blue-500/30">
                    <LayoutDashboard className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">Report Automatization</span>
            </div>

            <nav className="flex-1 p-4 space-y-2 mt-4">
                {navItems.map((item) => (
                    <NavLink
                        key={item.to}
                        to={item.to}
                        className={({ isActive }) =>
                            cn(
                                "flex items-center space-x-3 px-4 py-3 rounded-lg transition-all duration-200 group relative overflow-hidden",
                                isActive
                                    ? "bg-blue-600 text-white shadow-lg shadow-blue-900/20"
                                    : "text-slate-400 hover:bg-slate-800 hover:text-white"
                            )
                        }
                    >
                        <item.icon className="w-5 h-5" />
                        <span className="font-medium">{item.label}</span>
                        {/* Hover effect highlight */}
                        <div className="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity" />
                    </NavLink>
                ))}
            </nav>

            <div className="p-4 border-t border-slate-800">
                <div className="text-xs text-slate-500 text-center">
                    &copy; 2026 Report Automatization
                </div>
            </div>
        </aside>
    );
};
