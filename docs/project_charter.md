# Project Charter: PPTX Analyzer & Automation Platform
Popis projektu: Webová aplikace pro bezpečný upload, analýzu a vizualizaci dat z PPTX prezentací (se zaměřením na OPEX a finanční reporty), postavená na Reactu a Pythonu, zabezpečená přes Azure Entra ID.

**1. Technická Architektura**
Frontend (Client)
Framework: React 18+ (Vite)
Jazyk: TypeScript
State Management: TanStack Query (React Query) – pro server state.
Styling: Tailwind CSS + Fluent UI (Microsoft Design Language).
Routing: React Router DOM.
HTTP Client: Axios (s interceptory).
Backend (Server)
Framework: Python FastAPI.
Auth Library: fastapi-azure-auth.
API Standard: REST.

Infrastruktura
Kontejnerizace: Docker & Docker Compose.
Identita: Azure Entra ID (bývalé Azure AD).

**2. Implementační Checklist (Scope)**

**A. Autentizace a Bezpečnost (Azure Entra ID)**
Toto byla nejkritičtější část. Je nutné zajistit:
[ ] Registrace Aplikace v Azure:
[ ] Nastaveno Redirect URI: http://localhost:5173 (Dev) a produkční URL.
[ ] Manifest: Nastaveno "accessTokenAcceptedVersion": 2 (pro kompatibilitu s Python backendem).
[ ] Expose an API: Vytvořen scope (např. user_impersonation nebo access_as_user).
[ ] API Permissions: Aplikace má přidělená oprávnění sama k sobě (Delegated) a udělený Admin Consent.
[ ] Frontend Auth (MSAL v3):
[ ] main.tsx: Inicializace PublicClientApplication a handling handleRedirectPromise (automaticky přes Provider).
[ ] App.tsx: Obalení routování pomocí <MsalProvider>.
[ ] Použití <AuthenticatedTemplate> pro chráněné routy.
[ ] Použití <UnauthenticatedTemplate> pro redirect na Login.
[ ] Login Flow:
[ ] Ošetření "Race Condition" (interaction_in_progress) v komponentě Login.tsx.
[ ] Tlačítko pro Login (Popup nebo Redirect flow).
[ ] Tlačítko pro Logout.

**B. Komunikace s API (Axios & Tokeny)**
Zajištění, že každý request má platný Bearer token.
[ ] Axios Instance (src/axios.ts):
[ ] Vytvoření centralizované instance Axiosu.
[ ] Interceptor: Automatické vložení Authorization: Bearer <token> do hlavičky každého requestu.
[ ] Logika Interceptoru:
[ ] Získání ActiveAccount z MSAL.
[ ] Fallback: Pokud není aktivní účet, vzít první ze seznamu getAllAccounts().
[ ] Volání acquireTokenSilent se správným Scope (shodným s Azure).
[ ] API Endpoints:
[ ] Handling HTTP 401 (Unauthorized) – frontend by měl poznat vypršení session.

**C. Hlavní Funkce Aplikace (Features)**
**1. Dashboard & Navigace**
[ ] Layout: Hlavní šablona s horní lištou (UserInfo, Logout) a bočním menu.
[ ] Routing:
[ ] / -> Dashboard (přehled).
[ ] /analytics -> Analytika.

[ ] /admin -> Admin sekce.
[ ] /opex/dashboard -> Specializovaný OPEX dashboard.
[ ] /import/opex/pptx -> Upload pro OPEX Powerpoint.
[ ] /import/opex/excel -> Upload pro OPEX Excel.

**2. Práce se soubory (Upload & List)**
[ ] Seznam souborů:
[ ] Fetch dat z API (/api/import/get-list-uploaded-files).
[ ] Zobrazení stavu načítání (isLoading) a chyb (isError, Error Boundary).
[ ] Upload souborů:
[ ] Drag & Drop zóna nebo tlačítko pro výběr.
[ ] Progress bar (využití onUploadProgress v Axiosu).
[ ] Podpora .pptx formátu.
[ ] Podpora .xlsx formátu.
[ ] Odeslání na endpointy /api/import/upload nebo /api/import/uploadopex.
[ ] Invalidace React Query cache po úspěšném uploadu (auto-refresh seznamu).

**D. DevOps & Deployment**
[ ] Docker Compose:
[ ] Služba frontend: Mapování portů, volume pro hot-reload (pokud je v dev), předání ENV proměnných.
[ ] Služba backend: Python container.
[ ] Environment Variables (.env):

[ ] ENVIRONMENT
[ ] API_URL
[ ] AZURE_CLIENT_ID
[ ] AZURE_TENANT
[ ] AZURE_REDIRECT_URI



[ ] Soulad názvů proměnných v docker-compose.yml a v aplikaci.

**3. Známá rizika a řešení (Lessons Learned)**
Chyba 401 (Invalid Issuer):
Řešení: Vždy zkontrolovat v Azure Manifestu accessTokenAcceptedVersion: 2.

Chyba 401 (Invalid Scope / 65005):
Řešení: Scope v authConfig.ts (např. api://<id>/user_impersonation) musí přesně sedět s tím v Azure "Expose an API".

MSAL Interaction in Progress:
Řešení: Nevolat loginRedirect okamžitě při renderu, pokud je status startup nebo handle_redirect. Vždy kontrolovat inProgress === InteractionStatus.None.

Chybějící Token v Requestu:
Řešení: Nepoužívat v komponentách import axios from 'axios', ale vždy import api from '../axios'.

**4. Další kroky (To-Do)**
[ ] Implementovat Error Boundary komponentu pro hezčí zobrazení pádů (místo bílé obrazovky).
[ ] Dokončit vizualizaci dat na Opex Dashboardu (grafy, tabulky).
[ ] Přidat validaci souborů na straně klienta (velikost, typ) před odesláním.