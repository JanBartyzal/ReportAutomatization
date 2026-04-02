import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { PublicClientApplication } from '@azure/msal-browser';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
// For token acquisition we request openid + profile; the resulting idToken
// has aud = our Azure client-id, which engine-core's TokenValidationService expects.
const TOKEN_SCOPES = ['openid', 'profile'];

let msalInstance: PublicClientApplication | null = null;
// Default to test-org-1 — matches UAT test data uploads.
// Will be updated after /auth/me resolves in non-dev environments.
let devOrgId: string | null = 'a0000000-0000-0000-0000-000000000001';

/** Set the MSAL instance for token acquisition. Call once during app initialization. */
export function setMsalInstance(instance: PublicClientApplication): void {
  msalInstance = instance;
}

/** Set org ID for dev bypass mode (called after /auth/me resolves). */
export function setDevOrgId(orgId: string): void {
  devOrgId = orgId;
}

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach Bearer token via MSAL or dev bypass
apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // Dev bypass mode: send Bearer token + X-headers for direct service access
  const authBypass = import.meta.env.VITE_AUTH_BYPASS === 'true';
  if (authBypass || !msalInstance) {
    config.headers.Authorization = 'Bearer dev-bypass-token';
    // Headers required by backend services for auth context (normally set by nginx ForwardAuth)
    config.headers['X-User-Id'] = '6bbc3213-00ac-4d30-bf27-7477b207c515';
    config.headers['X-Org-Id'] = devOrgId || 'a0000000-0000-0000-0000-000000000001';
    config.headers['X-Roles'] = 'HOLDING_ADMIN';
    return config;
  }

  const activeAccount = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!activeAccount) {
    // No active account — send dev token as fallback
    config.headers.Authorization = 'Bearer dev-bypass-token';
    return config;
  }

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
