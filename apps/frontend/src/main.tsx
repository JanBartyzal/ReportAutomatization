import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MsalProvider } from '@azure/msal-react';
import { PublicClientApplication } from '@azure/msal-browser';
import { ThemeProvider } from './theme';

import App from './App';
import { msalConfig } from './auth/msalConfig';
import { initTelemetry } from './telemetry/otel';
import { ErrorBoundary } from './components/Error';
import './index.css';

// Initialize OpenTelemetry browser tracing
initTelemetry();

// Create MSAL instance
const msalInstance = new PublicClientApplication(msalConfig);

// Create Query Client
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 5 * 60 * 1000, // 5 minutes
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});

// Initialize app
async function initializeApp() {
    await msalInstance.initialize();

    // Restore active account from session cache
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0) {
        msalInstance.setActiveAccount(accounts[0]);
    }

    // Link MSAL to Axios
    import('./api/axios').then(({ setMsalInstance }) => {
        setMsalInstance(msalInstance);
    });

    ReactDOM.createRoot(document.getElementById('root')!).render(
        <React.StrictMode>
            <BrowserRouter>
                <MsalProvider instance={msalInstance}>
                    <QueryClientProvider client={queryClient}>
                        <ThemeProvider>
                            <ErrorBoundary>
                                <App />
                            </ErrorBoundary>
                        </ThemeProvider>
                    </QueryClientProvider>
                </MsalProvider>
            </BrowserRouter>
        </React.StrictMode>
    );
}

initializeApp();
