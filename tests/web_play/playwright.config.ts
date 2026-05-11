import { defineConfig, devices } from '@playwright/test';
import path from 'path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';

export const AUTH_DIR = path.join(__dirname, 'logs', 'auth');
export const ADMIN_STATE  = path.join(AUTH_DIR, 'holding-admin.json');
export const EDITOR_STATE = path.join(AUTH_DIR, 'editor.json');
export const VIEWER_STATE = path.join(AUTH_DIR, 'viewer.json');

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 2,

  reporter: [
    ['list'],
    ['html', { outputFolder: './logs/html-report', open: 'never' }],
    ['json', { outputFile: './logs/results.json' }],
  ],

  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    viewport: { width: 1280, height: 800 },
    locale: 'cs-CZ',
    timezoneId: 'Europe/Prague',
  },

  globalSetup: './global-setup.ts',

  projects: [
    // ── Auth setup – runs once, saves storageState files ────────────────────
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
    },

    // ── Holding Admin perspective (approves, manages periods, templates) ────
    {
      name: 'chromium-admin',
      use: {
        ...devices['Desktop Chrome'],
        storageState: ADMIN_STATE,
      },
      dependencies: ['setup'],
      testMatch: [
        '**/FS07_Admin_UI/**',
        '**/FS08_Batch/**',
        '**/FS11_Dashboards/**',
        '**/FS12_Search_AI_MCP/**',
        '**/FS13_Notifications/**',
        '**/FS14_Versioning/**',
        '**/FS15_Schema_Mapping/**',
        '**/FS16_Audit/**',
        '**/FS17_Report_Lifecycle/**',
        '**/FS18_PPTX_Generation/**',
        '**/FS19_Form_Builder/**',
        '**/FS20_Period_Management/**',
        '**/FS21_Local_Scope/**',
        '**/FS22_Period_Comparison/**',
        '**/FS23_Integrations/**',
        '**/FS24_Data_Promotion/**',
        '**/FS25_Sink_Browser/**',
        '**/FS26_Report_Generation/**',
        '**/FS27_Excel_Sync/**',
      ],
    },

    // ── Editor perspective (uploads, fills forms, submits reports) ───────────
    {
      name: 'chromium-editor',
      use: {
        ...devices['Desktop Chrome'],
        storageState: EDITOR_STATE,
      },
      dependencies: ['setup'],
      testMatch: [
        '**/FS09_File_Upload/**',
        '**/FS19_Form_Filling/**',
      ],
    },

    // ── Auth + navigation (uses own auth per test) ───────────────────────────
    {
      name: 'chromium-auth',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],
      testMatch: ['**/FS09_Auth_Navigation/**'],
    },

    // ── UX quality (responsive, a11y) – multiple viewports ──────────────────
    {
      name: 'ux-desktop',
      use: {
        ...devices['Desktop Chrome'],
        storageState: ADMIN_STATE,
        viewport: { width: 1440, height: 900 },
      },
      dependencies: ['setup'],
      testMatch: ['**/FS99_UX_Quality/**'],
    },
    {
      name: 'ux-tablet',
      use: {
        ...devices['iPad (gen 7)'],
        storageState: ADMIN_STATE,
      },
      dependencies: ['setup'],
      testMatch: ['**/FS99_UX_Quality/**'],
    },
    {
      name: 'ux-mobile',
      use: {
        ...devices['iPhone 14'],
        storageState: ADMIN_STATE,
      },
      dependencies: ['setup'],
      testMatch: ['**/FS99_UX_Quality/**'],
    },
  ],
});
