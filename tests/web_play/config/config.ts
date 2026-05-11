export const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

export const USERS = {
  holdingAdmin: {
    email: 'admin1@testorg1.ra',
    password: '',
    role: 'HOLDING_ADMIN',
    orgSlug: 'test-org-1',
  },
  editor: {
    email: 'user1@testorg1.ra',
    password: '',
    role: 'EDITOR',
    orgSlug: 'test-org-1',
  },
  viewer: {
    email: 'user2@testorg2.ra',
    password: '',
    role: 'VIEWER',
    orgSlug: 'test-org-2',
  },
  admin2: {
    email: 'admin2@testorg2.ra',
    password: '',
    role: 'HOLDING_ADMIN',
    orgSlug: 'test-org-2',
  },
} as const;

export const ROUTES = {
  login:              '/login',
  dashboard:          '/dashboard',
  upload:             '/upload',
  files:              '/files',
  forms:              '/forms',
  formNew:            '/forms/new',
  reports:            '/reports',
  periods:            '/periods',
  templates:          '/templates',
  dashboards:         '/dashboards',
  dashboardNew:       '/dashboards/new',
  matrix:             '/matrix',
  adminManage:        '/admin/manage',
  adminHolding:       '/admin/holding',
  adminHealth:        '/admin/health',
  adminIntegrations:  '/admin/integrations',
  adminPromotions:    '/admin/promotions',
  settings:           '/settings',
  notificationSettings: '/settings/notifications',
  search:             '/search',
  namedQueries:       '/reporting/named-queries',
  textTemplates:      '/reporting/text-templates',
  textTemplateNew:    '/reporting/text-templates/new',
  batchGeneration:    '/batch-generation',
  generatedReports:   '/generated-reports',
  comparison:         '/comparison',
  local:              '/local',
  sinks:              '/sinks',
  projects:           '/projects',
  exportFlows:        '/admin/export-flows',
} as const;

export const TIMEOUTS = {
  short:    5_000,
  default:  15_000,
  upload:   60_000,
  generate: 90_000,
} as const;

export const SERVICES = {
  frontend:        BASE_URL,
  engineCore:      'http://localhost:8081',
  engineIngestor:  'http://localhost:8082',
  engineData:      'http://localhost:8100',
  engineReporting: 'http://localhost:8105',
} as const;

/** Selector helpers — prefer role/label/testid selectors for accessibility coverage */
export const SEL = {
  sidebar:          'nav, aside, [role="navigation"]',
  notificationBell: '[data-testid="notification-bell"], button[aria-label*="notification" i], button[aria-label*="notif" i]',
  loadingSpinner:   '[role="progressbar"], .loading, [data-testid="spinner"]',
  errorMessage:     '[role="alert"], .error-message, [data-testid="error"]',
  successToast:     '[role="status"], .success-toast, [data-testid="toast-success"]',
  uploadZone:       '[data-testid="dropzone"], [data-testid="upload-zone"], input[type="file"]',
  progressBar:      '[role="progressbar"], .progress-bar',
  statusBadge:      '[data-testid*="status"], .badge, [class*="status"]',
} as const;
