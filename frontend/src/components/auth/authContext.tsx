import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../../api/axios';

interface User {
    id: string;
    email: string;
    name: string;
    role: string;
    organization_id: string;
}

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: () => void;
    loginDev: () => Promise<void>;
    logout: () => void;
    handleCallback: (code: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const initAuth = async () => {
            const token = localStorage.getItem('token');
            if (token) {
                try {
                    // Verify token and get user info
                    const response = await api.get('/api/auth/me');
                    setUser(response.data);
                } catch (error) {
                    console.error("Failed to fetch user", error);
                    localStorage.removeItem('token');
                }
            }
            setIsLoading(false);
        };

        initAuth();
    }, []);

    const login = () => {
        // Redirect to backend Azure Auth endpoint which redirects to Microsoft
        window.location.href = `${import.meta.env.VITE_API_URL || ''}/api/auth/sso/azure/login`;
    };

    const loginDev = async () => {
        try {
            const response = await api.post('/api/auth/dev/login');
            const { access_token, user: userData } = response.data;

            localStorage.setItem('token', access_token);
            setUser(userData);
            window.location.href = '/plans';
        } catch (error) {
            console.error("Dev login failed", error);
            alert("Dev login failed. Ensure backend is running in development mode.");
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
        window.location.href = '/login';
    };

    const handleCallback = async (code: string) => {
        setIsLoading(true);
        try {
            // Exchange code for token
            const response = await api.get(`/api/auth/sso/azure/callback?code=${code}`);
            const { access_token, user: userData } = response.data;

            localStorage.setItem('token', access_token);
            setUser(userData);
            window.location.href = '/plans'; // Redirect to dashboard
        } catch (error) {
            console.error("SSO Callback failed", error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, loginDev, logout, handleCallback }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
