// frontend/src/App.tsx
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { ImportOpex } from './pages/import/upload_opex';


const queryClient = new QueryClient();

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            {/* 1. Informativní Dashboard */}
            <Route index element={<Dashboard />} />

            {/* 2. Importní cesty */}
            <Route path="import/opex" element={<ImportOpex />} />


          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;