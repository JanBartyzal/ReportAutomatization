
import { Configuration, PopupRequest } from "@azure/msal-browser";

const clientId = import.meta.env.VITE_AZURE_CLIENT_ID;
if (!clientId) {
    throw new Error("Missing VITE_AZURE_CLIENT_ID in environment variables! Application cannot start.");
}

const tenantId = import.meta.env.VITE_AZURE_TENANT_ID;
if (!tenantId) {
    throw new Error("Missing VITE_AZURE_TENANT_ID in environment variables! Application cannot start.");
}
const redirectUri = import.meta.env.VITE_AZURE_REDIRECT_URI || window.location.origin + "/";

console.log("MSAL Config:", { clientId, tenantId, redirectUri });

export const msalConfig: Configuration = {
    auth: {
        clientId: clientId,
        authority: `https://login.microsoftonline.com/${tenantId}`,
        redirectUri: redirectUri,
        postLogoutRedirectUri: "/"
    },
    cache: {
        cacheLocation: "localStorage", // Ukládá tokeny, aby přežily refresh

    },
    system: {
        loggerOptions: {
            loggerCallback: (level, message, containsPii) => {
                if (containsPii) {
                    return;
                }
                switch (level) {
                    case 0:
                        console.error(message);
                        return;
                    case 1:
                        console.warn(message);
                        return;
                    case 2:
                        console.info(message);
                        return;
                    case 3:
                        console.debug(message);
                        return;
                }
            },
            logLevel: 3, // Verbose
        }
    }
};

export const loginRequest = {
    scopes: [`api://${clientId}/user_impersonation`]
};

export const tokenRequest = {
    scopes: [`api://${clientId}/user_impersonation`]
};
