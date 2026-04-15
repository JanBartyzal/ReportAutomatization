import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// Service URLs for direct proxying (bypasses nginx ForwardAuth issues)
const ENGINE_CORE = process.env.VITE_ENGINE_CORE_URL || 'http://localhost:8081';
const ENGINE_INGESTOR = process.env.VITE_ENGINE_INGESTOR_URL || 'http://localhost:8082';
const ENGINE_DATA = process.env.VITE_ENGINE_DATA_URL || 'http://localhost:8100';
const ENGINE_REPORTING = process.env.VITE_ENGINE_REPORTING_URL || 'http://localhost:8105';
const ENGINE_EXCEL_SYNC = process.env.VITE_ENGINE_EXCEL_SYNC_URL || 'http://localhost:8106';

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@reportplatform/types': path.resolve(__dirname, '../../packages/types/src/index.ts'),
        },
    },
    server: {
        host: '0.0.0.0',
        port: 5173,
        proxy: {
            // engine-core: auth, admin, batches, versioning, audit
            '/api/auth': { target: ENGINE_CORE, changeOrigin: true },
            '/api/admin': { target: ENGINE_CORE, changeOrigin: true },
            '/api/batches': { target: ENGINE_CORE, changeOrigin: true },
            '/api/versions': { target: ENGINE_CORE, changeOrigin: true },
            '/api/audit': { target: ENGINE_CORE, changeOrigin: true },

            // engine-ingestor: upload, files
            '/api/upload': { target: ENGINE_INGESTOR, changeOrigin: true },
            '/api/files': { target: ENGINE_INGESTOR, changeOrigin: true },

            // engine-data: query, dashboards, search
            '/api/query': { target: ENGINE_DATA, changeOrigin: true },
            '/api/dashboards': { target: ENGINE_DATA, changeOrigin: true },
            '/api/search': { target: ENGINE_DATA, changeOrigin: true, rewrite: (p) => p.replace('/api/search', '/api/v1/search') },
            '/api/comparisons': { target: ENGINE_DATA, changeOrigin: true },

            // engine-excel-sync: export flows (FS27)
            '/api/export-flows': { target: ENGINE_EXCEL_SYNC, changeOrigin: true },

            // engine-reporting: reports, periods, forms, templates, notifications
            '/api/reports': { target: ENGINE_REPORTING, changeOrigin: true },
            '/api/periods': { target: ENGINE_REPORTING, changeOrigin: true },
            '/api/forms': { target: ENGINE_REPORTING, changeOrigin: true },
            '/api/templates': { target: ENGINE_REPORTING, changeOrigin: true },
            '/api/notifications': { target: ENGINE_REPORTING, changeOrigin: true, rewrite: (p) => p.replace('/api/notifications', '/api/v1/notifications') },
        },
        allowedHosts: ['*'],
    },
    build: {
        outDir: 'dist',
        sourcemap: true,
    },
});
