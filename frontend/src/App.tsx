import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './components/auth/AuthProvider';
import { ProtectedRoute } from './components/auth/ProtectedRoute';

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

const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />

          {/* Protected Routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<MainLayout />}>
              <Route index element={<Dashboard />} />
              <Route path="analytics" element={<Analytics />} />
              <Route path="admin" element={<Admin />} />
              <Route path="opex/dashboard" element={<OpexDashboard />} />
              <Route path="import/opex" element={<ImportOpex />} />
              <Route path="import/upload/opex/excel" element={<ExcelImport />} />
              <Route path="aggregation" element={<AggregationDashboard />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;