import axios, { InternalAxiosRequestConfig } from 'axios';
import { msalInstance } from '../msalInstance'; // Importujeme naši instanci
import { loginRequest } from '../authConfig';   // Importujeme scope

// Vytvoření instance Axiosu
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

// REQUEST INTERCEPTOR
api.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
        // DEBUG LOG: Vidíme, že interceptor běží
        console.log(`🔌 Axios Interceptor: Zpracovávám ${config.url}`);

        // 1. Zkusíme získat aktivní účet
        let account = msalInstance.getActiveAccount();

        // FALLBACK: Pokud ActiveAccount je null (stává se po refresh),
        // zkusíme ho vytáhnout ze seznamu všech účtů.
        if (!account) {
            const allAccounts = msalInstance.getAllAccounts();
            if (allAccounts.length > 0) {
                console.log("⚠️ ActiveAccount byl null, beru první ze seznamu.");
                account = allAccounts[0];
                // Pro jistotu ho nastavíme jako aktivní pro příště
                msalInstance.setActiveAccount(account);
            }
        }

        if (account) {
            try {
                // 2. Získání tokenu (Silent = na pozadí)
                // Používáme 'loginRequest', protože tam máš definované scopes pro API
                const response = await msalInstance.acquireTokenSilent({
                    ...loginRequest,
                    account: account
                });

                // 3. Přidáme token do hlavičky
                config.headers.Authorization = `Bearer ${response.accessToken}`;
                console.log("🔑 Token úspěšně přidán do hlavičky.");

            } catch (error) {
                console.error("❌ Chyba při získávání tokenu (Silent fail):", error);
                // Poznámka: Pokud selže silent token (např. vypršela session),
                // request odejde bez tokenu a skončí 401. To je správně.
                // Frontend by pak měl uživatele přesměrovat na login.
            }
        } else {
            console.warn("⚠️ Interceptor: Žádný uživatel není přihlášen! Posílám request bez tokenu.");
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;