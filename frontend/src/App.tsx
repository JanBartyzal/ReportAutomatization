
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthenticatedTemplate, UnauthenticatedTemplate } from "@azure/msal-react";
import { MainLayout } from './components/Layout/MainLayout';
import { Login } from './pages/Login';
import { OpexDashboard } from './pages/opex/OpexDashboard';
import { ImportOpex } from './pages/import/upload_opex';

// Placeholder Pages
const Dashboard = () => <h1 className="text-2xl font-bold">Dashboard</h1>;
const Analytics = () => <h1 className="text-2xl font-bold">Analytics</h1>;
const Admin = () => <h1 className="text-2xl font-bold">Admin</h1>;

const App: React.FC = () => {
  return (
    <BrowserRouter>
      {/* If authenticated, show layout */}
      <AuthenticatedTemplate>
        <Routes>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<Dashboard />} />
            <Route path="analytics" element={<Analytics />} />
            <Route path="admin" element={<Admin />} />
            <Route path="opex/dashboard" element={<OpexDashboard />} />
            <Route path="import/opex" element={<ImportOpex />} />
          </Route>
        </Routes>
      </AuthenticatedTemplate>

      {/* If NOT authenticated, show login */}
      <UnauthenticatedTemplate>
        <Routes>
          <Route path="*" element={<Login />} />
        </Routes>
      </UnauthenticatedTemplate>
    </BrowserRouter>
  );
};

export default App;