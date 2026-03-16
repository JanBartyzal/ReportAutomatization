import { type Configuration, LogLevel } from '@azure/msal-browser';

const clientId = import.meta.env.VITE_AZURE_CLIENT_ID ?? '';
const tenantId = import.meta.env.VITE_AZURE_TENANT_ID ?? 'common';
const redirectUri = import.meta.env.VITE_AZURE_REDIRECT_URI ?? 'http://localhost:5173';
const authBypass = import.meta.env.VITE_AUTH_BYPASS === 'true';

export const msalConfig: Configuration = {
    auth: {
        clientId,
        authority: `https://login.microsoftonline.com/${tenantId}`,
        redirectUri,
        postLogoutRedirectUri: redirectUri,
        navigateToLoginRequestUrl: true,
    },
    cache: {
        cacheLocation: 'sessionStorage',
        storeAuthStateInCookie: false,
    },
    system: {
        loggerOptions: {
            loggerCallback: (level, message, containsPii) => {
                if (containsPii) return;
                switch (level) {
                    case LogLevel.Error:
                        console.error(message);
                        break;
                    case LogLevel.Warning:
                        console.warn(message);
                        break;
                    default:
                        break;
                }
            },
            logLevel: LogLevel.Warning,
        },
    },
};

// Export for use in components
export const isAuthBypassed = authBypass;

export const loginRequest = {
    scopes: ['openid', 'profile', 'User.Read'],
};
