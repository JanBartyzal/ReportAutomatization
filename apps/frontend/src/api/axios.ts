import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { PublicClientApplication } from '@azure/msal-browser';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
// For token acquisition we request openid + profile; the resulting idToken
// has aud = our Azure client-id, which engine-core's TokenValidationService expects.
const TOKEN_SCOPES = ['openid', 'profile'];

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
      scopes: TOKEN_SCOPES,
      account: activeAccount,
    });
    // Use idToken (aud = client-id) rather than accessToken (aud = Graph API)
    config.headers.Authorization = `Bearer ${response.idToken}`;
  } catch {
    // Silent refresh failed (expired session) – try interactive popup
    try {
      const response = await msalInstance.acquireTokenPopup({
        scopes: TOKEN_SCOPES,
        account: activeAccount,
      });
      config.headers.Authorization = `Bearer ${response.idToken}`;
    } catch {
      // Both failed – clear stale session so UnauthenticatedTemplate renders
      msalInstance.setActiveAccount(null);
      sessionStorage.clear();
      window.location.href = '/login';
      return Promise.reject(new Error('Session expired'));
    }
  }

  return config;
});

// Response interceptor: handle 401 (redirect to login) and 429 (rate limited)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && msalInstance) {
      msalInstance.setActiveAccount(null);
      sessionStorage.clear();
      window.location.href = '/login';
    }
    if (error.response?.status === 429) {
      console.warn('Rate limited. Please wait before retrying.');
    }
    return Promise.reject(error);
  },
);

export default apiClient;
