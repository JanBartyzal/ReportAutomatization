import { AuthenticatedTemplate, UnauthenticatedTemplate } from '@azure/msal-react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { isAuthBypassed } from './auth/msalConfig';

import AppLayout from './components/Layout/AppLayout';
import AdminGuard from './components/auth/AdminGuard';
import ToastContainer from './components/NotificationCenter/ToastContainer';

import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import DashboardListPage from './pages/DashboardListPage';
import DashboardEditorPage from './pages/DashboardEditorPage';
import DashboardViewerPage from './pages/DashboardViewerPage';
import FilesPage from './pages/FilesPage';
import FileDetailPage from './pages/FileDetailPage';
import UploadPage from './pages/UploadPage';
import SettingsPage from './pages/SettingsPage';
import ReportsPage from './pages/ReportsPage';
import ReportDetailPage from './pages/ReportDetailPage';
import PeriodsPage from './pages/PeriodsPage';
import PeriodDetailPage from './pages/PeriodDetailPage';
import MatrixDashboard from './pages/MatrixDashboard';
import FormsListPage from './pages/FormsListPage';
import FormFillerPage from './pages/FormFillerPage';
import ExcelImportPage from './pages/ExcelImportPage';
import FormEditorPage from './pages/FormEditorPage';
import FormAssignmentPage from './pages/FormAssignmentPage';
import NotificationSettingsPage from './pages/NotificationSettingsPage';
import SearchResultsPage from './pages/SearchResultsPage';
import TemplateListPage from './pages/TemplateListPage';
import TemplateDetailPage from './pages/TemplateDetailPage';
import BatchGenerationPage from './pages/BatchGenerationPage';
import GeneratedReportsListPage from './pages/GeneratedReportsListPage';
import LocalDashboardPage from './pages/LocalDashboardPage';
import ComparisonPage from './pages/ComparisonPage';
import HoldingAdminOverviewPage from './pages/HoldingAdminOverviewPage';
import HealthDashboardPage from './pages/HealthDashboardPage';
import IntegrationPage from './pages/IntegrationPage';
import DistributionRulesPage from './pages/DistributionRulesPage';
import PromotionPage from './pages/PromotionPage';
import AdminPage from './pages/AdminPage';
import NotFoundPage from './pages/NotFoundPage';

function AuthenticatedRoutes() {
    return (
        <AppLayout>
            <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/dashboards" element={<DashboardListPage />} />
                <Route path="/dashboards/new" element={<DashboardEditorPage />} />
                <Route path="/dashboards/:dashboardId" element={<DashboardViewerPage />} />
                <Route path="/dashboards/:dashboardId/edit" element={<DashboardEditorPage />} />
                <Route path="/matrix" element={<MatrixDashboard />} />
                <Route path="/files" element={<FilesPage />} />
                <Route path="/files/:fileId" element={<FileDetailPage />} />
                <Route path="/upload" element={<UploadPage />} />
                <Route path="/reports" element={<ReportsPage />} />
                <Route path="/reports/:reportId" element={<ReportDetailPage />} />
                <Route path="/periods" element={<PeriodsPage />} />
                <Route path="/periods/:periodId" element={<PeriodDetailPage />} />
                <Route path="/periods/new" element={<PeriodsPage />} />
                <Route path="/forms" element={<FormsListPage />} />
                <Route path="/forms/new" element={<FormEditorPage />} />
                <Route path="/forms/:formId" element={<FormEditorPage />} />
                <Route path="/forms/:formId/edit" element={<FormEditorPage />} />
                <Route path="/forms/:formId/assignments" element={<FormAssignmentPage />} />
                <Route path="/forms/:formId/fill" element={<FormFillerPage />} />
                <Route path="/forms/:formId/fill/:responseId" element={<FormFillerPage />} />
                <Route path="/forms/:formId/import" element={<ExcelImportPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/settings/notifications" element={<NotificationSettingsPage />} />
                <Route path="/templates" element={<TemplateListPage />} />
                <Route path="/templates/:templateId" element={<TemplateDetailPage />} />
                <Route path="/batch-generation" element={<BatchGenerationPage />} />
                <Route path="/generated-reports" element={<GeneratedReportsListPage />} />
                <Route path="/search" element={<SearchResultsPage />} />
                <Route path="/local" element={<LocalDashboardPage />} />
                <Route path="/comparison" element={<ComparisonPage />} />
                <Route path="/admin/holding" element={
                    <AdminGuard>
                        <HoldingAdminOverviewPage />
                    </AdminGuard>
                } />
                <Route path="/admin/health" element={
                    <AdminGuard>
                        <HealthDashboardPage />
                    </AdminGuard>
                } />
                <Route path="/admin/integrations" element={
                    <AdminGuard>
                        <IntegrationPage />
                    </AdminGuard>
                } />
                <Route path="/admin/integrations/distribution" element={
                    <AdminGuard>
                        <DistributionRulesPage />
                    </AdminGuard>
                } />
                <Route path="/admin/promotions" element={
                    <AdminGuard>
                        <PromotionPage />
                    </AdminGuard>
                } />
                <Route path="/admin/manage" element={
                    <AdminGuard>
                        <AdminPage />
                    </AdminGuard>
                } />
                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </AppLayout>
    );
}

function App() {
    // In dev bypass mode, skip MSAL auth checks entirely
    if (isAuthBypassed) {
        return (
            <>
                <ToastContainer />
                <AuthenticatedRoutes />
            </>
        );
    }

    return (
        <>
            <ToastContainer />
            <AuthenticatedTemplate>
                <AuthenticatedRoutes />
            </AuthenticatedTemplate>

            <UnauthenticatedTemplate>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="*" element={<Navigate to="/login" replace />} />
                </Routes>
            </UnauthenticatedTemplate>
        </>
    );
}

export default App;
