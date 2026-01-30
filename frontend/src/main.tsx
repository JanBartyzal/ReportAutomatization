import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// 1. IMPORT REACT QUERY
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// 2. VYTVOŘENÍ INSTANCE
import { ErrorBoundary } from './components/ErrorBoundary';

const queryClient = new QueryClient();

// 3. Render aplikace
ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <ErrorBoundary>
                <App />
            </ErrorBoundary>
        </QueryClientProvider>
    </React.StrictMode>,
);
