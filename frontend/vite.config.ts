import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    server: {
        //host: true, // Povolí přístup zvenčí kontejneru
        host: '0.0.0.0',
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8000', // your backend server URL
                changeOrigin: true,
                secure: false,
            },
        },
        allowedHosts: ['bartwin10'], // Povolit přístup z konkrétního hosta
        strictPort: true,
        watch: {
            usePolling: true // Nutné pro Docker na Windows/Mac
        }
    }
})