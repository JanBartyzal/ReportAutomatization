import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import axios from 'axios';
// Note: We use the global configured axios instance or a fresh one. 
// Using the default import from 'axios' bypasses our interceptors if we want raw access,
// but for 'me' endpoint we DO want cookies, so we should actually use our configured api OR ensure default axios sends credentials.
// For safety/simplicity in this file, we can config axios locally or import the api client.
// Let's import the api client to ensure baseURL and credentials setup is consistent.
import api from '../../api/axios';

// Define User Type based on Backend Response
export interface UserPermissions {
    can_sync_prices: boolean;
    can_edit_plans: boolean;
    can_view_reports: boolean;
}

export interface User {
    id: string;
    email: string;
    roles: string[];
    permissions: UserPermissions;
}

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: () => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Initial Auth Check
    useEffect(() => {
        checkAuth();
    }, []);

    const checkAuth = async () => {
        try {
            const response = await api.get('/api/auth/me'); // uses configured baseURL /api
            setUser(response.data);
        } catch (error) {
            // console.log("Not authenticated", error);
            setUser(null);
        } finally {
            setIsLoading(false);
        }
    };

    const login = () => {
        // Redirect to Backend SSO Endpoint
        // We calculate the redirect URL (Frontend URL) to return to.
        // If we are currently on /login, we probably want to go to root / or dashboard after success.
        const currentPath = window.location.pathname;
        const targetPath = currentPath === '/login' ? '/' : currentPath;
        const redirectUrl = `${window.location.origin}${targetPath}`;

        const backendLoginUrl = `${import.meta.env.VITE_API_URL}/api/auth/sso/azure/login?redirect_after_login=${encodeURIComponent(redirectUrl)}`;
        window.location.href = backendLoginUrl;
    };

    const logout = () => {
        const backendLogoutUrl = `${import.meta.env.VITE_API_URL}/api/auth/sso/azure/logout`;
        window.location.href = backendLogoutUrl;
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
