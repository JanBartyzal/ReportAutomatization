/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string;
    readonly VITE_AZURE_CLIENT_ID: string;
    readonly VITE_AZURE_TENANT_ID: string;
    readonly VITE_AZURE_REDIRECT_URI: string;
    readonly VITE_AZURE_API_SCOPE: string;
    readonly VITE_AUTH_BYPASS: string;
    // Feature flags for P6-W4
    readonly VITE_ENABLE_LOCAL_SCOPE?: string;
    readonly VITE_ENABLE_ADVANCED_COMPARISON?: string;
    readonly VITE_ENABLE_MULTI_ORG_COMPARISON?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
