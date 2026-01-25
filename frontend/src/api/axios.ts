
import axios, { InternalAxiosRequestConfig } from 'axios';
import { PublicClientApplication, AccountInfo } from '@azure/msal-browser';
import { msalConfig, tokenRequest } from '../authConfig';

const api = axios.create({
    baseURL: '/api', // Proxy in vite config handles path
    headers: {
        'Content-Type': 'application/json',
    },
});

// Create a separate MSAL instance for token acquisition outside of React context
const msalInstance = new PublicClientApplication(msalConfig);
let msalInitialized = false;

async function getAccessToken(): Promise<string | null> {
    if (!msalInitialized) {
        await msalInstance.initialize();
        msalInitialized = true;
    }

    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0) {
        const request = {
            ...tokenRequest,
            account: accounts[0],
        };
        try {
            const response = await msalInstance.acquireTokenSilent(request);
            return response.accessToken;
        } catch (error) {
            console.error("Silent token acquisition failed", error);
            // Fallback to interaction if needed, or return null to trigger login
            return null;
        }
    }
    return null;
}

api.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
        const token = await getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;
