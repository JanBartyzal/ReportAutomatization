// frontend/src/App.tsx
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { ImportOpex } from './pages/import/upload_opex';
import { ViewerPPTX } from './pages/opex/viewerpptx';
import { OpexDashboard } from './pages/opex/OpexDashboard';




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

            {/* 3. Viewer */}
            <Route path="opex/viewer" element={<ViewerPPTX />} />

            {/* 4. Opex Dashboard */}
            <Route path="opex/dashboard" element={<OpexDashboard />} />

          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;