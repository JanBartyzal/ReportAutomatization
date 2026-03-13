import { http, HttpResponse, delay } from 'msw';

// Types for mock data
export interface MockFile {
    file_id: string;
    filename: string;
    size_bytes: number;
    mime_type: string;
    status: 'UPLOADED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIAL';
    blob_url: string;
    uploaded_at: string;
    org_id: string;
}

export interface MockReport {
    id: string;
    org_id: string;
    period_id: string;
    report_type: string;
    status: 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
    scope: 'CENTRAL' | 'LOCAL';
    locked: boolean;
    submitted_by?: string;
    created_by: string;
    created_at: string;
    updated_at: string;
}

export interface MockDashboard {
    id: string;
    name: string;
    is_public: boolean;
    created_at: string;
}

export interface MockPeriod {
    id: string;
    name: string;
    period_type: string;
    start_date: string;
    end_date: string;
    status: 'DRAFT' | 'OPEN' | 'CLOSED';
}

export interface MockTemplate {
    id: string;
    name: string;
    version: number;
    scope: 'CENTRAL' | 'LOCAL';
    placeholderCount: number;
    createdAt: string;
    updatedAt: string;
}

// Mock data
const mockFiles: MockFile[] = [
    {
        file_id: 'file-001',
        filename: 'Q1_Report.pptx',
        size_bytes: 2048576,
        mime_type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        status: 'COMPLETED',
        blob_url: 'https://storage.example.com/files/file-001',
        uploaded_at: '2024-01-15T10:30:00Z',
        org_id: 'org-001',
    },
    {
        file_id: 'file-002',
        filename: 'Budget_2024.xlsx',
        size_bytes: 524288,
        mime_type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        status: 'PROCESSING',
        blob_url: 'https://storage.example.com/files/file-002',
        uploaded_at: '2024-01-16T14:20:00Z',
        org_id: 'org-001',
    },
    {
        file_id: 'file-003',
        filename: 'Draft_Proposal.pdf',
        size_bytes: 1048576,
        mime_type: 'application/pdf',
        status: 'UPLOADED',
        blob_url: 'https://storage.example.com/files/file-003',
        uploaded_at: '2024-01-17T09:15:00Z',
        org_id: 'org-002',
    },
];

const mockReports: MockReport[] = [
    {
        id: 'report-001',
        org_id: 'org-001',
        period_id: 'period-001',
        report_type: 'OPEX',
        status: 'APPROVED',
        scope: 'CENTRAL',
        locked: true,
        submitted_by: 'user@example.com',
        created_by: 'admin@example.com',
        created_at: '2024-01-10T08:00:00Z',
        updated_at: '2024-01-15T16:30:00Z',
    },
    {
        id: 'report-002',
        org_id: 'org-001',
        period_id: 'period-002',
        report_type: 'OPEX',
        status: 'SUBMITTED',
        scope: 'CENTRAL',
        locked: false,
        submitted_by: 'editor@example.com',
        created_by: 'editor@example.com',
        created_at: '2024-01-12T10:00:00Z',
        updated_at: '2024-01-14T11:00:00Z',
    },
    {
        id: 'report-003',
        org_id: 'org-002',
        period_id: 'period-001',
        report_type: 'OPEX',
        status: 'DRAFT',
        scope: 'LOCAL',
        locked: false,
        created_by: 'user@example.com',
        created_at: '2024-01-16T09:00:00Z',
        updated_at: '2024-01-16T09:00:00Z',
    },
];

const mockDashboards: MockDashboard[] = [
    {
        id: 'dash-001',
        name: 'OPEX Overview',
        is_public: true,
        created_at: '2024-01-01T00:00:00Z',
    },
    {
        id: 'dash-002',
        name: 'Department Budget',
        is_public: false,
        created_at: '2024-01-05T00:00:00Z',
    },
];

const mockPeriods: MockPeriod[] = [
    {
        id: 'period-001',
        name: 'Q1 2024',
        period_type: 'QUARTERLY',
        start_date: '2024-01-01',
        end_date: '2024-03-31',
        status: 'CLOSED',
    },
    {
        id: 'period-002',
        name: 'Q2 2024',
        period_type: 'QUARTERLY',
        start_date: '2024-04-01',
        end_date: '2024-06-30',
        status: 'OPEN',
    },
];

const mockTemplates: MockTemplate[] = [
    {
        id: 'template-001',
        name: 'Standard OPEX Report',
        version: 3,
        scope: 'CENTRAL',
        placeholderCount: 12,
        createdAt: '2023-12-01T00:00:00Z',
        updatedAt: '2024-01-10T00:00:00Z',
    },
    {
        id: 'template-002',
        name: 'Department Budget Template',
        version: 1,
        scope: 'LOCAL',
        placeholderCount: 8,
        createdAt: '2024-01-05T00:00:00Z',
        updatedAt: '2024-01-05T00:00:00Z',
    },
];

// Handlers
export const handlers = [
    // Files endpoints
    http.get('/api/files', async ({ request }) => {
        await delay(200);
        const url = new URL(request.url);
        const status = url.searchParams.get('status');
        const mimeType = url.searchParams.get('mime_type');

        let filteredFiles = [...mockFiles];
        if (status) {
            filteredFiles = filteredFiles.filter(f => f.status === status);
        }
        if (mimeType) {
            filteredFiles = filteredFiles.filter(f => f.mime_type.includes(mimeType));
        }

        return HttpResponse.json({
            files: filteredFiles,
            total: filteredFiles.length,
        });
    }),

    http.get('/api/files/:fileId', async ({ params }) => {
        await delay(100);
        const file = mockFiles.find(f => f.file_id === params.fileId);
        if (!file) {
            return new HttpResponse(null, { status: 404 });
        }
        return HttpResponse.json(file);
    }),

    // Reports endpoints
    http.get('/api/reports', async ({ request }) => {
        await delay(200);
        const url = new URL(request.url);
        const status = url.searchParams.get('status');
        const orgId = url.searchParams.get('org_id');

        let filteredReports = [...mockReports];
        if (status) {
            filteredReports = filteredReports.filter(r => r.status === status);
        }
        if (orgId) {
            filteredReports = filteredReports.filter(r => r.org_id === orgId);
        }

        return HttpResponse.json({
            reports: filteredReports,
            total: filteredReports.length,
        });
    }),

    http.get('/api/reports/:reportId', async ({ params }) => {
        await delay(100);
        const report = mockReports.find(r => r.id === params.reportId);
        if (!report) {
            return new HttpResponse(null, { status: 404 });
        }
        return HttpResponse.json(report);
    }),

    // Dashboards endpoints
    http.get('/api/dashboards', async () => {
        await delay(200);
        return HttpResponse.json({
            dashboards: mockDashboards,
            total: mockDashboards.length,
        });
    }),

    http.get('/api/dashboards/:dashboardId', async ({ params }) => {
        await delay(100);
        const dashboard = mockDashboards.find(d => d.id === params.dashboardId);
        if (!dashboard) {
            return new HttpResponse(null, { status: 404 });
        }
        return HttpResponse.json(dashboard);
    }),

    // Periods endpoints
    http.get('/api/periods', async () => {
        await delay(200);
        return HttpResponse.json({
            periods: mockPeriods,
            total: mockPeriods.length,
        });
    }),

    http.get('/api/periods/:periodId', async ({ params }) => {
        await delay(100);
        const period = mockPeriods.find(p => p.id === params.periodId);
        if (!period) {
            return new HttpResponse(null, { status: 404 });
        }
        return HttpResponse.json(period);
    }),

    // Templates endpoints
    http.get('/api/templates/pptx', async () => {
        await delay(200);
        return HttpResponse.json({
            templates: mockTemplates,
            total: mockTemplates.length,
        });
    }),

    http.get('/api/templates/pptx/:templateId', async ({ params }) => {
        await delay(100);
        const template = mockTemplates.find(t => t.id === params.templateId);
        if (!template) {
            return new HttpResponse(null, { status: 404 });
        }
        return HttpResponse.json(template);
    }),

    // Auth endpoint
    http.get('/api/auth/me', async () => {
        await delay(100);
        return HttpResponse.json({
            user_id: 'user-001',
            email: 'test@example.com',
            display_name: 'Test User',
            organizations: [
                { id: 'org-001', name: 'Acme Corp', type: 'COMPANY', parent_id: 'holding-001' },
                { id: 'org-002', name: 'Acme Division', type: 'DIVISION', parent_id: 'org-001' },
            ],
            active_org_id: 'org-001',
            roles: ['ADMIN'],
        });
    }),

    // Generation endpoints
    http.post('/api/generation/generate', async ({ request }) => {
        await delay(500);
        const body = await request.json() as { template_id: string; period_id: string };
        return HttpResponse.json({
            job_id: `job-${Date.now()}`,
            template_id: body.template_id,
            period_id: body.period_id,
            status: 'QUEUED',
            created_at: new Date().toISOString(),
        });
    }),

    http.get('/api/generation/jobs/:jobId', async ({ params }) => {
        await delay(100);
        return HttpResponse.json({
            job_id: params.jobId,
            status: 'COMPLETED',
            progress: 100,
            result_url: '/api/downloads/report-001.pptx',
            completed_at: new Date().toISOString(),
        });
    }),
];
