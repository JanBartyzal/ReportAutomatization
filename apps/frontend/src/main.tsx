import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MsalProvider } from '@azure/msal-react';
import { PublicClientApplication } from '@azure/msal-browser';
import { FluentProvider, webLightTheme } from '@fluentui/react-components';
import { lightTheme } from './theme/brandTokens';

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

    // Link MSAL to Axios
    import('./api/axios').then(({ setMsalInstance }) => {
        setMsalInstance(msalInstance);
    });

    ReactDOM.createRoot(document.getElementById('root')!).render(
        <React.StrictMode>
            <BrowserRouter>
                <MsalProvider instance={msalInstance}>
                    <QueryClientProvider client={queryClient}>
                        <FluentProvider theme={lightTheme}>
                            <ErrorBoundary>
                                <App />
                            </ErrorBoundary>
                        </FluentProvider>
                    </QueryClientProvider>
                </MsalProvider>
            </BrowserRouter>
        </React.StrictMode>
    );
}

initializeApp();
