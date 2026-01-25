
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { PublicClientApplication } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./authConfig";
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const msalInstance = new PublicClientApplication(msalConfig);
const queryClient = new QueryClient();

// Initialize the msal instance
msalInstance.initialize().then(async () => {
    // Handle redirect response (important for loginRedirect flow)
    await msalInstance.handleRedirectPromise().catch(error => {
        console.error("Redirect handle error: ", error);
    });

    ReactDOM.createRoot(document.getElementById('root')!).render(
        <React.StrictMode>
            <MsalProvider instance={msalInstance}>
                <QueryClientProvider client={queryClient}>
                    <App />
                </QueryClientProvider>
            </MsalProvider>
        </React.StrictMode>,
    );
});