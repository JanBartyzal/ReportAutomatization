import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { EventType } from "@azure/msal-browser";
// Importujeme naši sdílenou instanci
import { msalInstance } from "./msalInstance";

// 1. IMPORT REACT QUERY
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// 2. VYTVOŘENÍ INSTANCE
import { ErrorBoundary } from './components/ErrorBoundary';

const queryClient = new QueryClient();

// 1. Nastavení callbacků (Bezpečné před init)
msalInstance.addEventCallback((event) => {
    if (event.eventType === EventType.LOGIN_SUCCESS && event.payload) {
        const account = (event.payload as any).account;
        msalInstance.setActiveAccount(account);
    }
});

// 2. Inicializace (Async)
msalInstance.initialize().then(() => {

    // 3. Teprve TEĎ je bezpečné sáhnout na účty
    // Zkontrolujeme, zda máme účet v cache a nastavíme ho jako aktivní
    if (!msalInstance.getActiveAccount() && msalInstance.getAllAccounts().length > 0) {
        msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
    }

    // 4. Render aplikace
    ReactDOM.createRoot(document.getElementById('root')!).render(
        <React.StrictMode>
            <QueryClientProvider client={queryClient}>
                <ErrorBoundary>
                    <App msalInstance={msalInstance} />
                </ErrorBoundary>
            </QueryClientProvider>
        </React.StrictMode>,
    );
});