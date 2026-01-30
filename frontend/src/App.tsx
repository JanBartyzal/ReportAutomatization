import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthenticatedTemplate, MsalProvider, UnauthenticatedTemplate } from '@azure/msal-react';
import { PublicClientApplication } from '@azure/msal-browser'; // <-- NUTNÝ IMPORT PRO TYPY
// MsalProvider už neimportujeme, je v main.tsx!

// Importy tvých komponent
import { MainLayout } from './components/Layout/MainLayout';
import { Login } from './pages/Login';
import { OpexDashboard } from './pages/opex/OpexDashboard';
import { ImportOpex } from './pages/import/upload_opex';
import { AggregationDashboard } from './pages/AggregationDashboard';
import { ExcelImport } from './pages/import/ExcelImport';

// Real Page Imports
import { Dashboard } from './pages/dashboard/Dashboard';
import { Analytics } from './pages/analytics/Analytics';
import { Admin } from './pages/admin/Admin';

type AppProps = {
  msalInstance: PublicClientApplication;
};
// App už nepotřebuje props 'msalInstance', protože už je "uvnitř" Provideru z main.tsx
const App: React.FC<AppProps> = ({ msalInstance }) => {
  return (
    <MsalProvider instance={msalInstance}>
      <BrowserRouter>

        {/* Scénář A: Uživatel JE přihlášen */}
        <AuthenticatedTemplate>
          <Routes>
            <Route path="/" element={<MainLayout />}>
              <Route index element={<Dashboard />} />
              <Route path="analytics" element={<Analytics />} />
              <Route path="admin" element={<Admin />} />
              <Route path="opex/dashboard" element={<OpexDashboard />} />
              <Route path="import/opex" element={<ImportOpex />} />
              <Route path="import/upload/opex/excel" element={<ExcelImport />} />
              <Route path="aggregation" element={<AggregationDashboard />} />
            </Route>
            <Route path="/login" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthenticatedTemplate>

        {/* Scénář B: Uživatel NENÍ přihlášen */}
        <UnauthenticatedTemplate>
          <Routes>
            <Route path="*" element={<Login />} />
          </Routes>
        </UnauthenticatedTemplate>

      </BrowserRouter>
    </MsalProvider>
  );
};

export default App;