import { PublicClientApplication } from "@azure/msal-browser";
import { msalConfig } from "./authConfig";

// Jen vytvořit, NEinicializovat, NEčíst účty
export const msalInstance = new PublicClientApplication(msalConfig);