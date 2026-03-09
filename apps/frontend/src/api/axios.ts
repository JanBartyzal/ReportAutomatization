import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { PublicClientApplication } from '@azure/msal-browser';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
const API_SCOPE = import.meta.env.VITE_AZURE_API_SCOPE ?? 'api://default/access_as_user';

let msalInstance: PublicClientApplication | null = null;

/** Set the MSAL instance for token acquisition. Call once during app initialization. */
export function setMsalInstance(instance: PublicClientApplication): void {
  msalInstance = instance;
}

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach Bearer token via MSAL acquireTokenSilent
apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  if (!msalInstance) return config;

  const activeAccount = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!activeAccount) return config;

  try {
    const response = await msalInstance.acquireTokenSilent({
      scopes: [API_SCOPE],
      account: activeAccount,
    });
    config.headers.Authorization = `Bearer ${response.accessToken}`;
  } catch {
    // Silent token acquisition failed; request proceeds without token
  }

  return config;
});

// Response interceptor: handle 401 (redirect to login) and 429 (rate limited)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login';
    }
    if (error.response?.status === 429) {
      console.warn('Rate limited. Please wait before retrying.');
    }
    return Promise.reject(error);
  },
);

export default apiClient;
