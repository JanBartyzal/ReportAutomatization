
import React from 'react';
import { useMsal } from "@azure/msal-react";
import { LogOut, User as UserIcon, Bell } from 'lucide-react';
import { loginRequest } from '../../authConfig';

export const Header: React.FC = () => {
    const { instance, accounts } = useMsal();
    const activeAccount = accounts[0];

    const handleLogout = () => {
        instance.logoutPopup({
            postLogoutRedirectUri: "/",
            mainWindowRedirectUri: "/"
        });
    };

    return (
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-8 shadow-sm z-10">
            <div className="text-sm breadcrumbs text-slate-500">
                {/* Placeholder for breadcrumbs */}
                <span className="font-semibold text-slate-900">Application</span> / Dashboard
            </div>

            <div className="flex items-center space-x-6">
                <button className="relative p-2 text-slate-400 hover:text-slate-600 transition-colors">
                    <Bell className="w-5 h-5" />
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border border-white"></span>
                </button>

                <div className="h-8 w-px bg-slate-200"></div>

                <div className="flex items-center space-x-3">
                    <div className="text-right hidden md:block">
                        <div className="text-sm font-semibold text-slate-900">
                            {activeAccount?.name || 'Guest User'}
                        </div>
                        <div className="text-xs text-slate-500">
                            {activeAccount?.username || 'Please login'}
                        </div>
                    </div>

                    <div className="relative group">
                        <button className="w-10 h-10 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 overflow-hidden shadow-sm transition-all hover:ring-2 hover:ring-blue-500 hover:ring-offset-2">
                            <UserIcon className="w-5 h-5" />
                        </button>

                        {/* Dropdown Menu */}
                        <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-slate-100 py-1 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 transform origin-top-right z-50">
                            <button
                                onClick={handleLogout}
                                className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-slate-50 flex items-center space-x-2"
                            >
                                <LogOut className="w-4 h-4" />
                                <span>Sign Out</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </header>
    );
};
