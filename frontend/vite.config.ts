import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        host: true, // Povolí přístup zvenčí kontejneru
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:80',
                changeOrigin: true,
                secure: false,
                // PŘIDAT TOTO PRO LOGOVÁNÍ:
                configure: (proxy, _options) => {
                    proxy.on('error', (err, _req, _res) => {
                        console.log('proxy error', err);
                    });
                    proxy.on('proxyReq', (proxyReq, req, _res) => {
                        console.log('Sending Request to the Target:', req.method, req.url);
                    });
                    proxy.on('proxyRes', (proxyRes, req, _res) => {
                        console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
                    });
                },
            }
        },
        strictPort: true,
        watch: {
            usePolling: true // Nutné pro Docker na Windows/Mac
        }
    }
})